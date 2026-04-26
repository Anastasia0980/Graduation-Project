"""
强化学习对战平台统一模板（兼容版本）

当前稳定支持算法：
- DQN
- QLearning

统一接口：
    agent = AgentFactory.create_from_config(config_path)
    agent.load(model_path)
    action = agent.act(observation)
"""

import json
import pickle
import re
from typing import Dict

import numpy as np
import torch
import torch.nn as nn


def get_device():
    if torch.cuda.is_available():
        return torch.device("cuda")
    return torch.device("cpu")


DEVICE = get_device()


class BaseAgent:
    def act(self, observation: Dict):
        raise NotImplementedError

    def load(self, path: str):
        raise NotImplementedError


class MLP(nn.Module):
    def __init__(self, input_dim, output_dim, hidden_layers, activation="relu"):
        super().__init__()

        if len(hidden_layers) > 3:
            raise ValueError("最多支持3层隐藏层")
        for h in hidden_layers:
            if h > 512:
                raise ValueError("单层神经元不能超过512")

        layers = []
        prev_dim = input_dim

        for h in hidden_layers:
            layers.append(nn.Linear(prev_dim, h))
            if activation == "relu":
                layers.append(nn.ReLU())
            elif activation == "tanh":
                layers.append(nn.Tanh())
            else:
                raise ValueError("不支持的激活函数")
            prev_dim = h

        layers.append(nn.Linear(prev_dim, output_dim))
        self.model = nn.Sequential(*layers)

    def forward(self, x):
        return self.model(x)


class DQNAgent(BaseAgent):
    def __init__(self, config):
        self.device = DEVICE
        self.config = config
        self.action_dim = int(config.get("action_dim", 9))
        self.input_dim = int(config.get("input_dim", 18))
        self.activation = config.get("activation", "relu")
        self.hidden_layers = self._resolve_hidden_layers(config)
        self.q_net = self._build_network(
            input_dim=self.input_dim,
            output_dim=self.action_dim,
            hidden_layers=self.hidden_layers
        )

    def _resolve_hidden_layers(self, config):
        hidden_layers = config.get("hidden_layers")
        if isinstance(hidden_layers, list) and len(hidden_layers) > 0:
            return [int(x) for x in hidden_layers]

        hidden_dim = config.get("hidden_dim")
        if hidden_dim is not None:
            hidden_dim = int(hidden_dim)
            return [hidden_dim, hidden_dim]

        return [128, 64]

    def _build_network(self, input_dim, output_dim, hidden_layers):
        net = MLP(
            input_dim=input_dim,
            output_dim=output_dim,
            hidden_layers=hidden_layers,
            activation=self.activation
        ).to(self.device)
        net.eval()
        return net

    def _extract_state_dict(self, raw_obj):
        if isinstance(raw_obj, dict):
            for key in ["state_dict", "model_state_dict", "q_net_state_dict", "net_state_dict"]:
                value = raw_obj.get(key)
                if isinstance(value, dict):
                    return value
        return raw_obj

    def _normalize_key(self, key: str) -> str:
        if key.startswith("module."):
            key = key[len("module."):]

        if key.startswith("q_net."):
            key = key[len("q_net."):]
        if key.startswith("q_network."):
            key = key[len("q_network."):]

        if key.startswith("network."):
            key = "model." + key[len("network."):]
        elif key.startswith("net."):
            key = "model." + key[len("net."):]
        return key

    def _normalize_state_dict(self, state_dict):
        normalized = {}
        for key, value in state_dict.items():
            normalized[self._normalize_key(key)] = value
        return normalized

    def _infer_structure_from_state_dict(self, state_dict):
        linear_layers = []
        for key, value in state_dict.items():
            match = re.match(r"^model\.(\d+)\.weight$", key)
            if match and hasattr(value, "shape") and len(value.shape) == 2:
                idx = int(match.group(1))
                out_dim = int(value.shape[0])
                in_dim = int(value.shape[1])
                linear_layers.append((idx, out_dim, in_dim))

        if not linear_layers:
            return None

        linear_layers.sort(key=lambda x: x[0])
        input_dim = linear_layers[0][2]
        output_dim = linear_layers[-1][1]
        hidden_layers = [item[1] for item in linear_layers[:-1]]

        return {
            "input_dim": input_dim,
            "output_dim": output_dim,
            "hidden_layers": hidden_layers
        }

    def _rebuild_network_if_needed(self, inferred):
        if inferred is None:
            return

        need_rebuild = (
            self.input_dim != inferred["input_dim"] or
            self.action_dim != inferred["output_dim"] or
            self.hidden_layers != inferred["hidden_layers"]
        )

        if need_rebuild:
            self.input_dim = inferred["input_dim"]
            self.action_dim = inferred["output_dim"]
            self.hidden_layers = inferred["hidden_layers"]
            self.q_net = self._build_network(
                input_dim=self.input_dim,
                output_dim=self.action_dim,
                hidden_layers=self.hidden_layers
            )

    def load(self, path: str):
        raw_obj = torch.load(path, map_location=self.device)
        state_dict = self._extract_state_dict(raw_obj)
        if not isinstance(state_dict, dict):
            raise ValueError("模型文件格式不正确，无法解析 state_dict")

        state_dict = self._normalize_state_dict(state_dict)
        inferred = self._infer_structure_from_state_dict(state_dict)
        self._rebuild_network_if_needed(inferred)

        self.q_net.load_state_dict(state_dict, strict=True)
        self.q_net.to(self.device)
        self.q_net.eval()

    def act(self, observation: Dict):
        board = np.asarray(observation["observation"]).flatten()
        action_mask = np.asarray(observation["action_mask"])

        state = torch.FloatTensor(board).unsqueeze(0).to(self.device)
        with torch.no_grad():
            q_values = self.q_net(state).cpu().numpy().flatten()

        masked_q = np.where(action_mask == 1, q_values, -1e9)
        return int(np.argmax(masked_q))


class QLearningAgent(BaseAgent):
    def __init__(self, config):
        self.q_table = {}

    def load(self, path: str):
        with open(path, "rb") as f:
            self.q_table = pickle.load(f)

    def act(self, observation: Dict):
        board = tuple(np.asarray(observation["observation"]).flatten())
        action_mask = np.asarray(observation["action_mask"])

        if board not in self.q_table:
            valid_actions = np.where(action_mask == 1)[0]
            return int(np.random.choice(valid_actions))

        q_values = np.array(self.q_table[board])
        masked_q = q_values * action_mask + (1 - action_mask) * -1e9
        return int(np.argmax(masked_q))


class AgentFactory:

    @staticmethod
    def create_from_config(config_path: str):
        with open(config_path, "r", encoding="utf-8") as f:
            config = json.load(f)

        algo = str(config.get("algorithm", "DQN")).strip().lower()

        if algo == "dqn":
            return DQNAgent(config)

        if algo in ["qlearning", "q_learning", "q-learning"]:
            return QLearningAgent(config)

        raise ValueError(f"不支持的算法类型: {config.get('algorithm')}")