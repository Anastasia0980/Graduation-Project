"""
强化学习对战平台统一模型模板（多人模式）

用途：
1. 教师/学生提交 config.json + model.pt 后，测评脚本可统一加载模型。
2. 支持井字棋 tictactoe_v3 与四子棋 connect_four_v3。
3. 支持 DQN 系列与离散动作 SAC 风格策略网络。

当前支持：
- DQN + MLP / CNN
- DoubleDQN + MLP / CNN（测评阶段与 DQN 加载方式一致）
- DuelingDQN + MLP / CNN
- SAC(discrete) + MLP / CNN（测评只需 actor/policy 网络）
- QLearning（兼容旧版本）

统一接口：
    agent = AgentFactory.create_from_config(config_path)
    agent.load(model_path)
    action = agent.act(observation, explore=True)

推荐 config.json 示例：
{
  "algorithm": "DQN",
  "model_type": "cnn",
  "dqn_variant": "dueling",
  "input_shape": [6, 7, 2],
  "action_dim": 7,
  "cnn_channels": [32, 64],
  "hidden_layers": [256, 128],
  "activation": "relu",
  "eval_epsilon": 0.02
}

{
  "algorithm": "SAC",
  "model_type": "mlp",
  "input_dim": 18,
  "action_dim": 9,
  "hidden_layers": [128, 128],
  "activation": "relu",
  "eval_epsilon": 0.02,
  "eval_temperature": 1.0
}
"""

import json
import pickle
import random
import re
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F


def get_device():
    return torch.device("cuda" if torch.cuda.is_available() else "cpu")


DEVICE = get_device()


# -----------------------------
# 通用工具
# -----------------------------

def _as_list(value, default: Sequence[int]) -> List[int]:
    if isinstance(value, list) and len(value) > 0:
        return [int(x) for x in value]
    if isinstance(value, tuple) and len(value) > 0:
        return [int(x) for x in value]
    return list(default)


def _activation(name: str):
    name = str(name or "relu").lower()
    if name == "relu":
        return nn.ReLU()
    if name == "tanh":
        return nn.Tanh()
    if name == "gelu":
        return nn.GELU()
    if name in ["leaky_relu", "leakyrelu", "lrelu"]:
        return nn.LeakyReLU(0.01)
    if name == "elu":
        return nn.ELU()
    raise ValueError(f"不支持的激活函数: {name}")


def _safe_hidden_layers(config: Dict, default=(128, 64)) -> List[int]:
    hidden_layers = config.get("hidden_layers")
    if isinstance(hidden_layers, list) and len(hidden_layers) > 0:
        return [int(x) for x in hidden_layers]
    hidden_dim = config.get("hidden_dim")
    if hidden_dim is not None:
        hidden_dim = int(hidden_dim)
        return [hidden_dim, hidden_dim]
    return list(default)


def _infer_shape_from_config(config: Dict) -> Optional[Tuple[int, int, int]]:
    shape = config.get("input_shape") or config.get("obs_shape") or config.get("observation_shape")
    if isinstance(shape, list) and len(shape) == 3:
        return int(shape[0]), int(shape[1]), int(shape[2])
    return None


def _default_shape_from_dims(input_dim: int, action_dim: int) -> Optional[Tuple[int, int, int]]:
    # PettingZoo classic:
    # tictactoe_v3 observation: 3 * 3 * 2 = 18, action_dim=9
    # connect_four_v3 observation: 6 * 7 * 2 = 84, action_dim=7
    if int(input_dim) == 18 or int(action_dim) == 9:
        return 3, 3, 2
    if int(input_dim) == 84 or int(action_dim) == 7:
        return 6, 7, 2
    return None


def _extract_obs_and_mask(observation: Dict):
    obs = observation.get("observation") if isinstance(observation, dict) else observation
    if isinstance(observation, dict):
        mask = observation.get("action_mask")
    else:
        mask = None
    obs_arr = np.asarray(obs, dtype=np.float32)
    if mask is None:
        # 没有 action_mask 时，默认所有动作可用；动作数由最后一维无法推断时在 agent 中补齐。
        mask_arr = None
    else:
        mask_arr = np.asarray(mask, dtype=np.int64)
    return obs_arr, mask_arr


def _valid_actions(action_mask: Optional[np.ndarray], action_dim: int) -> np.ndarray:
    if action_mask is None:
        return np.arange(action_dim, dtype=np.int64)
    valid = np.where(np.asarray(action_mask).astype(np.int64) == 1)[0]
    if valid.size == 0:
        return np.arange(action_dim, dtype=np.int64)
    return valid


def _choose_with_exploration(scores: np.ndarray, action_mask: Optional[np.ndarray], epsilon: float) -> int:
    action_dim = int(scores.shape[-1])
    valid = _valid_actions(action_mask, action_dim)
    if epsilon > 0 and random.random() < epsilon:
        return int(np.random.choice(valid))
    masked = np.full(action_dim, -1e9, dtype=np.float32)
    masked[valid] = scores[valid]
    return int(np.argmax(masked))


def _masked_softmax_sample(logits: np.ndarray, action_mask: Optional[np.ndarray], epsilon: float, temperature: float) -> int:
    action_dim = int(logits.shape[-1])
    valid = _valid_actions(action_mask, action_dim)
    if epsilon > 0 and random.random() < epsilon:
        return int(np.random.choice(valid))

    masked = np.full(action_dim, -1e9, dtype=np.float32)
    masked[valid] = logits[valid]
    temp = max(float(temperature), 1e-6)
    x = masked / temp
    x = x - np.max(x[valid])
    probs = np.exp(x)
    probs[np.setdiff1d(np.arange(action_dim), valid)] = 0.0
    total = probs.sum()
    if not np.isfinite(total) or total <= 0:
        return int(np.random.choice(valid))
    probs = probs / total
    return int(np.random.choice(np.arange(action_dim), p=probs))


def _extract_state_dict(raw_obj, preferred_keys: Iterable[str]):
    if isinstance(raw_obj, dict):
        for key in preferred_keys:
            value = raw_obj.get(key)
            if isinstance(value, dict):
                return value
        for key in ["state_dict", "model_state_dict", "q_net_state_dict", "net_state_dict", "actor_state_dict", "policy_state_dict"]:
            value = raw_obj.get(key)
            if isinstance(value, dict):
                return value
    return raw_obj


def _strip_prefixes(key: str) -> str:
    prefixes = [
        "module.", "q_net.", "q_network.", "online_net.", "online_network.",
        "actor.", "policy.", "policy_net.", "net.", "network.",
    ]
    changed = True
    while changed:
        changed = False
        for p in prefixes:
            if key.startswith(p):
                key = key[len(p):]
                changed = True
    return key


def _normalize_state_dict_for_model(state_dict: Dict, model: nn.Module) -> Dict:
    """尽量兼容旧模型权重前缀；若原 key 已匹配，则优先保留。"""
    model_keys = set(model.state_dict().keys())
    out = {}
    for key, value in state_dict.items():
        if key in model_keys:
            out[key] = value
            continue
        stripped = _strip_prefixes(key)
        candidates = [
            stripped,
            "model." + stripped,
            "feature." + stripped,
            "features." + stripped,
            "head." + stripped,
            "advantage." + stripped,
            "value." + stripped,
        ]
        matched = False
        for c in candidates:
            if c in model_keys:
                out[c] = value
                matched = True
                break
        if not matched:
            # 保留原 key，strict=False 时可忽略；strict=True 时会提示。
            out[key] = value
    return out


# -----------------------------
# 网络模板
# -----------------------------

class MLP(nn.Module):
    def __init__(self, input_dim: int, output_dim: int, hidden_layers: Sequence[int], activation="relu"):
        super().__init__()
        layers = []
        prev = int(input_dim)
        for h in hidden_layers:
            h = int(h)
            layers.append(nn.Linear(prev, h))
            layers.append(_activation(activation))
            prev = h
        layers.append(nn.Linear(prev, int(output_dim)))
        self.model = nn.Sequential(*layers)

    def forward(self, x):
        return self.model(x)


class CNNBackbone(nn.Module):
    def __init__(self, input_shape: Tuple[int, int, int], cnn_channels: Sequence[int], activation="relu"):
        super().__init__()
        h, w, c = [int(x) for x in input_shape]
        layers = []
        in_ch = c
        for out_ch in cnn_channels:
            layers.append(nn.Conv2d(in_ch, int(out_ch), kernel_size=3, padding=1))
            layers.append(_activation(activation))
            in_ch = int(out_ch)
        self.conv = nn.Sequential(*layers)
        self.output_dim = in_ch * h * w
        self.input_shape = (h, w, c)

    def forward(self, x):
        # 输入允许 [B,H,W,C] 或 [B,C,H,W]
        if x.dim() == 3:
            x = x.unsqueeze(0)
        h, w, c = self.input_shape
        if x.shape[-1] == c:
            x = x.permute(0, 3, 1, 2).contiguous()
        x = self.conv(x)
        return x.reshape(x.shape[0], -1)


class CNNQNetwork(nn.Module):
    def __init__(self, input_shape, action_dim, cnn_channels=(32, 64), hidden_layers=(256,), activation="relu"):
        super().__init__()
        self.backbone = CNNBackbone(input_shape, cnn_channels, activation)
        self.head = MLP(self.backbone.output_dim, int(action_dim), hidden_layers, activation)

    def forward(self, x):
        z = self.backbone(x)
        return self.head(z)


class DuelingMLPQNetwork(nn.Module):
    def __init__(self, input_dim, action_dim, hidden_layers=(128, 128), activation="relu"):
        super().__init__()
        hidden_layers = list(hidden_layers)
        if len(hidden_layers) == 0:
            hidden_layers = [128]
        feature_layers = hidden_layers[:-1]
        last_dim = hidden_layers[-1]
        if feature_layers:
            self.feature = MLP(input_dim, last_dim, feature_layers, activation)
        else:
            self.feature = nn.Sequential(nn.Linear(int(input_dim), int(last_dim)), _activation(activation))
        self.value = MLP(last_dim, 1, [last_dim], activation)
        self.advantage = MLP(last_dim, int(action_dim), [last_dim], activation)

    def forward(self, x):
        z = self.feature(x)
        v = self.value(z)
        a = self.advantage(z)
        return v + a - a.mean(dim=1, keepdim=True)


class DuelingCNNQNetwork(nn.Module):
    def __init__(self, input_shape, action_dim, cnn_channels=(32, 64), hidden_layers=(256,), activation="relu"):
        super().__init__()
        self.backbone = CNNBackbone(input_shape, cnn_channels, activation)
        hidden = list(hidden_layers) if hidden_layers else [256]
        self.value = MLP(self.backbone.output_dim, 1, hidden, activation)
        self.advantage = MLP(self.backbone.output_dim, int(action_dim), hidden, activation)

    def forward(self, x):
        z = self.backbone(x)
        v = self.value(z)
        a = self.advantage(z)
        return v + a - a.mean(dim=1, keepdim=True)


class SACMLPPolicy(nn.Module):
    def __init__(self, input_dim, action_dim, hidden_layers=(128, 128), activation="relu"):
        super().__init__()
        self.model = MLP(input_dim, action_dim, hidden_layers, activation)

    def forward(self, x):
        return self.model(x)


class SACCNNPolicy(nn.Module):
    def __init__(self, input_shape, action_dim, cnn_channels=(32, 64), hidden_layers=(256,), activation="relu"):
        super().__init__()
        self.backbone = CNNBackbone(input_shape, cnn_channels, activation)
        self.head = MLP(self.backbone.output_dim, action_dim, hidden_layers, activation)

    def forward(self, x):
        return self.head(self.backbone(x))


# -----------------------------
# Agent 模板
# -----------------------------

class BaseAgent:
    def act(self, observation: Dict, explore: bool = True):
        raise NotImplementedError

    def load(self, path: str):
        raise NotImplementedError


class DQNAgent(BaseAgent):
    def __init__(self, config: Dict):
        self.device = DEVICE
        self.config = dict(config)
        self.action_dim = int(config.get("action_dim", 9))
        self.input_dim = int(config.get("input_dim", config.get("state_dim", 18)))
        self.model_type = str(config.get("model_type", config.get("network", "mlp"))).lower()
        self.dqn_variant = str(config.get("dqn_variant", config.get("variant", "standard"))).lower()
        self.activation = config.get("activation", "relu")
        self.hidden_layers = _safe_hidden_layers(config, default=(128, 64))
        self.cnn_channels = _as_list(config.get("cnn_channels"), default=(32, 64))
        self.input_shape = _infer_shape_from_config(config) or _default_shape_from_dims(self.input_dim, self.action_dim)
        self.eval_epsilon = float(config.get("eval_epsilon", config.get("epsilon", 0.02)))
        self.q_net = self._build_network().to(self.device)
        self.q_net.eval()

    def _build_network(self):
        is_dueling = self.dqn_variant in ["dueling", "dueling_dqn", "duelingdqn"]
        if self.model_type == "cnn":
            if self.input_shape is None:
                raise ValueError("CNN 模型需要在 config.json 中提供 input_shape，例如 [6,7,2]")
            if is_dueling:
                return DuelingCNNQNetwork(self.input_shape, self.action_dim, self.cnn_channels, self.hidden_layers, self.activation)
            return CNNQNetwork(self.input_shape, self.action_dim, self.cnn_channels, self.hidden_layers, self.activation)
        if is_dueling:
            return DuelingMLPQNetwork(self.input_dim, self.action_dim, self.hidden_layers, self.activation)
        return MLP(self.input_dim, self.action_dim, self.hidden_layers, self.activation)

    def load(self, path: str):
        raw = torch.load(path, map_location=self.device)
        state_dict = _extract_state_dict(raw, ["q_net_state_dict", "q_network_state_dict", "model_state_dict", "state_dict"])
        if not isinstance(state_dict, dict):
            raise ValueError("模型文件格式不正确，无法解析 DQN state_dict")
        state_dict = _normalize_state_dict_for_model(state_dict, self.q_net)
        try:
            self.q_net.load_state_dict(state_dict, strict=True)
        except RuntimeError:
            # 兼容旧版 MLP：如果 config 结构不完整，尝试根据权重尺寸重建普通 MLP。
            rebuilt = self._try_rebuild_legacy_mlp(state_dict)
            if not rebuilt:
                raise
            self.q_net.load_state_dict(_normalize_state_dict_for_model(state_dict, self.q_net), strict=True)
        self.q_net.to(self.device)
        self.q_net.eval()

    def _try_rebuild_legacy_mlp(self, state_dict: Dict) -> bool:
        linear_layers = []
        for key, value in state_dict.items():
            k = _strip_prefixes(key)
            m = re.match(r"^(?:model\.)?(\d+)\.weight$", k)
            if m and hasattr(value, "shape") and len(value.shape) == 2:
                linear_layers.append((int(m.group(1)), int(value.shape[0]), int(value.shape[1])))
        if not linear_layers:
            return False
        linear_layers.sort(key=lambda x: x[0])
        self.input_dim = linear_layers[0][2]
        self.action_dim = linear_layers[-1][1]
        self.hidden_layers = [x[1] for x in linear_layers[:-1]]
        self.model_type = "mlp"
        self.dqn_variant = "standard"
        self.q_net = MLP(self.input_dim, self.action_dim, self.hidden_layers, self.activation).to(self.device)
        self.q_net.eval()
        return True

    def _state_tensor(self, obs_arr: np.ndarray):
        if self.model_type == "cnn":
            return torch.FloatTensor(obs_arr).unsqueeze(0).to(self.device)
        return torch.FloatTensor(obs_arr.flatten()).unsqueeze(0).to(self.device)

    def act(self, observation: Dict, explore: bool = True):
        obs_arr, action_mask = _extract_obs_and_mask(observation)
        state = self._state_tensor(obs_arr)
        with torch.no_grad():
            q_values = self.q_net(state).detach().cpu().numpy().reshape(-1)
        eps = self.eval_epsilon if explore else 0.0
        return _choose_with_exploration(q_values, action_mask, eps)


class SACAgent(BaseAgent):
    """离散动作 SAC 策略网络测评封装。训练脚本保存 actor_state_dict 后可直接加载。"""

    def __init__(self, config: Dict):
        self.device = DEVICE
        self.config = dict(config)
        self.action_dim = int(config.get("action_dim", 9))
        self.input_dim = int(config.get("input_dim", config.get("state_dim", 18)))
        self.model_type = str(config.get("model_type", config.get("network", "mlp"))).lower()
        self.activation = config.get("activation", "relu")
        self.hidden_layers = _safe_hidden_layers(config, default=(128, 128))
        self.cnn_channels = _as_list(config.get("cnn_channels"), default=(32, 64))
        self.input_shape = _infer_shape_from_config(config) or _default_shape_from_dims(self.input_dim, self.action_dim)
        self.eval_epsilon = float(config.get("eval_epsilon", 0.02))
        self.eval_temperature = float(config.get("eval_temperature", 1.0))
        self.actor = self._build_actor().to(self.device)
        self.actor.eval()

    def _build_actor(self):
        if self.model_type == "cnn":
            if self.input_shape is None:
                raise ValueError("CNN SAC 模型需要在 config.json 中提供 input_shape，例如 [6,7,2]")
            return SACCNNPolicy(self.input_shape, self.action_dim, self.cnn_channels, self.hidden_layers, self.activation)
        return SACMLPPolicy(self.input_dim, self.action_dim, self.hidden_layers, self.activation)

    def load(self, path: str):
        raw = torch.load(path, map_location=self.device)
        state_dict = _extract_state_dict(raw, ["actor_state_dict", "policy_state_dict", "model_state_dict", "state_dict"])
        if not isinstance(state_dict, dict):
            raise ValueError("模型文件格式不正确，无法解析 SAC actor state_dict")
        state_dict = _normalize_state_dict_for_model(state_dict, self.actor)
        self.actor.load_state_dict(state_dict, strict=True)
        self.actor.to(self.device)
        self.actor.eval()

    def _state_tensor(self, obs_arr: np.ndarray):
        if self.model_type == "cnn":
            return torch.FloatTensor(obs_arr).unsqueeze(0).to(self.device)
        return torch.FloatTensor(obs_arr.flatten()).unsqueeze(0).to(self.device)

    def act(self, observation: Dict, explore: bool = True):
        obs_arr, action_mask = _extract_obs_and_mask(observation)
        state = self._state_tensor(obs_arr)
        with torch.no_grad():
            logits = self.actor(state).detach().cpu().numpy().reshape(-1)
        if explore:
            return _masked_softmax_sample(logits, action_mask, self.eval_epsilon, self.eval_temperature)
        return _choose_with_exploration(logits, action_mask, 0.0)


class QLearningAgent(BaseAgent):
    def __init__(self, config):
        self.q_table = {}
        self.eval_epsilon = float(config.get("eval_epsilon", 0.02))

    def load(self, path: str):
        with open(path, "rb") as f:
            self.q_table = pickle.load(f)

    def act(self, observation: Dict, explore: bool = True):
        board = tuple(np.asarray(observation["observation"]).flatten())
        action_mask = np.asarray(observation["action_mask"])
        valid = _valid_actions(action_mask, len(action_mask))
        if explore and self.eval_epsilon > 0 and random.random() < self.eval_epsilon:
            return int(np.random.choice(valid))
        if board not in self.q_table:
            return int(np.random.choice(valid))
        q_values = np.asarray(self.q_table[board], dtype=np.float32)
        return _choose_with_exploration(q_values, action_mask, 0.0)


class AgentFactory:
    @staticmethod
    def create_from_config(config_path: str):
        with open(config_path, "r", encoding="utf-8") as f:
            config = json.load(f)
        algo = str(config.get("algorithm", "DQN")).strip().lower()
        if algo in ["dqn", "double_dqn", "doubledqn", "dueling_dqn", "duelingdqn"]:
            # 兼容 algorithm 直接写变种的旧配置。
            if algo != "dqn" and "dqn_variant" not in config:
                config["dqn_variant"] = algo
            return DQNAgent(config)
        if algo in ["sac", "discrete_sac", "sac_discrete"]:
            return SACAgent(config)
        if algo in ["qlearning", "q_learning", "q-learning"]:
            return QLearningAgent(config)
        raise ValueError(f"不支持的算法类型: {config.get('algorithm')}")
