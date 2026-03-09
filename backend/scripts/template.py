"""
强化学习对战平台统一模板（基础版本）

支持算法：
- DQN（神经网络）
- QLearning（表格型）

统一接口：
    agent = AgentFactory.create_from_config(config_path)
    agent.load(model_path)
    action = agent.act(observation)

"""

import json
import pickle
import numpy as np
import torch
import torch.nn as nn
from typing import Dict


# =========================
# 设备检测（仅 DQN 使用）
# =========================

def get_device():
    if torch.cuda.is_available():
        return torch.device("cuda")
    return torch.device("cpu")


DEVICE = get_device()


# =========================
# 基础接口（所有算法统一）
# =========================

class BaseAgent:
    def act(self, observation: Dict):
        raise NotImplementedError

    def load(self, path: str):
        raise NotImplementedError


# =========================
# 通用 MLP 网络
# =========================

class MLP(nn.Module):
    def __init__(self, input_dim, output_dim, hidden_layers, activation="relu"):
        super().__init__()

        # 安全限制
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


# =========================
# DQN Agent
# =========================

class DQNAgent(BaseAgent):
    def __init__(self, config):

        self.device = DEVICE
        self.action_dim = 9

        self.input_dim = config.get("input_dim", 18)  # 默认18

        hidden_layers = config.get("hidden_layers", [128, 64])
        activation = config.get("activation", "relu")

        self.q_net = MLP(
            input_dim=self.input_dim,
            output_dim=self.action_dim,
            hidden_layers=hidden_layers,
            activation=activation
        ).to(self.device)

        self.q_net.eval()

    def load(self, path: str):
        state_dict = torch.load(path, map_location=self.device)
        self.q_net.load_state_dict(state_dict)
        self.q_net.to(self.device)
        self.q_net.eval()

    def act(self, observation: Dict):

        board = observation["observation"].flatten()
        action_mask = observation["action_mask"]

        state = torch.FloatTensor(board).unsqueeze(0).to(self.device)

        with torch.no_grad():
            q_values = self.q_net(state).cpu().numpy().flatten()

        # 屏蔽非法动作
        masked_q = np.where(action_mask == 1, q_values, -1e9)
        return int(np.argmax(masked_q))


# =========================
# QLearning Agent（表格型）
# =========================

class QLearningAgent(BaseAgent):
    def __init__(self, config):
        self.q_table = {}

    def load(self, path: str):
        with open(path, "rb") as f:
            self.q_table = pickle.load(f)

    def act(self, observation: Dict):

        board = tuple(observation["observation"].flatten())
        action_mask = observation["action_mask"]

        # 若状态未见过，则随机合法动作
        if board not in self.q_table:
            valid_actions = np.where(action_mask == 1)[0]
            return int(np.random.choice(valid_actions))

        q_values = np.array(self.q_table[board])

        masked_q = q_values * action_mask + (1 - action_mask) * -1e9
        return int(np.argmax(masked_q))


# =========================
# Agent 工厂
# =========================

class AgentFactory:

    @staticmethod
    def create_from_config(config_path: str):

        with open(config_path, "r") as f:
            config = json.load(f)

        algo = config.get("algorithm", "DQN")

        if algo == "DQN":
            return DQNAgent(config)

        elif algo == "QLearning":
            return QLearningAgent(config)

        else:
            raise ValueError("不支持的算法类型")