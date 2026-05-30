#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
井字棋 / 四子棋多人模型训练脚本（模板版）

目标：
- 生成可被 scripts/template.py 与 scripts/battle_evaluator.py 统一加载的 config.json + model.pt。
- 支持 DQN 系列：MLP / CNN / DuelingDQN / DoubleDQN 风格目标更新。
- 支持离散动作 SAC 框架：MLP / CNN actor，训练后导出 actor_state_dict。
- 训练阶段可使用随机对手或自博弈对手，便于产生不同风格、不同强度模型。

示例：
1）训练井字棋 MLP-DQN：
python train_battle_template.py --env tictactoe_v3 --algorithm dqn --model_type mlp --episodes 2000 --output_dir ./out/ttt_dqn_mlp

2）训练四子棋 CNN-DuelingDQN：
python train_battle_template.py --env connect_four_v3 --algorithm dqn --dqn_variant dueling --model_type cnn --episodes 5000 --output_dir ./out/cf_dueling_cnn

3）训练四子棋 CNN-SAC：
python train_battle_template.py --env connect_four_v3 --algorithm sac --model_type cnn --episodes 5000 --output_dir ./out/cf_sac_cnn

4）训练不同难度模型：
- easy：episodes 少、hidden_layers 小、opponent random
- medium：episodes 中等、opponent mix
- hard：episodes 多、opponent self_play，或使用更大 CNN

导出目录结构：
output_dir/
  config.json
  model.pt
"""

import argparse
import json
import os
import random
from collections import deque
from dataclasses import dataclass
from typing import List, Optional, Tuple

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

from pettingzoo.classic import connect_four_v3, tictactoe_v3

# 直接复用测评模板中的网络，保证训练导出的模型能被测评脚本加载。
from template import (
    CNNQNetwork,
    DuelingCNNQNetwork,
    DuelingMLPQNetwork,
    MLP,
    SACCNNPolicy,
    SACMLPPolicy,
)


def get_device():
    return torch.device("cuda" if torch.cuda.is_available() else "cpu")


DEVICE = get_device()


def ensure_dir(path: str):
    os.makedirs(path, exist_ok=True)


def parse_int_list(text: str) -> List[int]:
    if text is None or str(text).strip() == "":
        return []
    return [int(x.strip()) for x in str(text).split(",") if x.strip()]


def normalize_env_name(env_name: str) -> str:
    name = str(env_name or "tictactoe_v3").strip().lower()
    aliases = {
        "tictactoe": "tictactoe_v3",
        "tic_tac_toe": "tictactoe_v3",
        "tic-tac-toe": "tictactoe_v3",
        "tictactoe_v3": "tictactoe_v3",
        "connect_four": "connect_four_v3",
        "connect-four": "connect_four_v3",
        "connectfour": "connect_four_v3",
        "connect_four_v3": "connect_four_v3",
    }
    if name not in aliases:
        raise ValueError(f"仅支持 tictactoe_v3/connect_four_v3，收到 env={env_name}")
    return aliases[name]


def make_env(env_name: str):
    env_name = normalize_env_name(env_name)
    if env_name == "connect_four_v3":
        return connect_four_v3.env(render_mode=None)
    return tictactoe_v3.env(render_mode=None)


def env_dims(env_name: str) -> Tuple[Tuple[int, int, int], int, int]:
    env_name = normalize_env_name(env_name)
    if env_name == "connect_four_v3":
        shape = (6, 7, 2)
        return shape, int(np.prod(shape)), 7
    shape = (3, 3, 2)
    return shape, int(np.prod(shape)), 9


def reset_env(env, seed=None):
    try:
        return env.reset(seed=seed)
    except TypeError:
        if seed is not None:
            random.seed(seed)
            np.random.seed(seed % (2 ** 32 - 1))
        return env.reset()


def obs_to_tensor(obs_arr: np.ndarray, model_type: str):
    obs_arr = np.asarray(obs_arr, dtype=np.float32)
    if model_type == "cnn":
        return torch.FloatTensor(obs_arr).unsqueeze(0).to(DEVICE)
    return torch.FloatTensor(obs_arr.flatten()).unsqueeze(0).to(DEVICE)


def batch_obs_to_tensor(obs_batch: np.ndarray, model_type: str):
    obs_batch = np.asarray(obs_batch, dtype=np.float32)
    if model_type == "cnn":
        return torch.FloatTensor(obs_batch).to(DEVICE)
    return torch.FloatTensor(obs_batch.reshape(obs_batch.shape[0], -1)).to(DEVICE)


def valid_actions(mask) -> np.ndarray:
    mask = np.asarray(mask, dtype=np.int64)
    acts = np.where(mask == 1)[0]
    if acts.size == 0:
        return np.arange(mask.shape[0])
    return acts


def random_action(mask) -> int:
    return int(np.random.choice(valid_actions(mask)))


class ReplayBuffer:
    def __init__(self, capacity: int):
        self.buffer = deque(maxlen=int(capacity))

    def add(self, obs, action, reward, next_obs, next_mask, done):
        self.buffer.append((
            np.asarray(obs, dtype=np.float32),
            int(action),
            float(reward),
            np.asarray(next_obs, dtype=np.float32),
            np.asarray(next_mask, dtype=np.float32),
            float(done),
        ))

    def sample(self, batch_size: int):
        batch = random.sample(self.buffer, int(batch_size))
        obs, actions, rewards, next_obs, next_masks, dones = zip(*batch)
        return (
            np.stack(obs),
            np.asarray(actions, dtype=np.int64),
            np.asarray(rewards, dtype=np.float32),
            np.stack(next_obs),
            np.stack(next_masks),
            np.asarray(dones, dtype=np.float32),
        )

    def __len__(self):
        return len(self.buffer)


def build_q_network(model_type: str, dqn_variant: str, input_dim: int, input_shape, action_dim: int,
                    hidden_layers: List[int], cnn_channels: List[int], activation: str):
    model_type = model_type.lower()
    dqn_variant = dqn_variant.lower()
    if model_type == "cnn":
        if dqn_variant in ["dueling", "dueling_dqn", "duelingdqn"]:
            return DuelingCNNQNetwork(input_shape, action_dim, cnn_channels, hidden_layers, activation).to(DEVICE)
        return CNNQNetwork(input_shape, action_dim, cnn_channels, hidden_layers, activation).to(DEVICE)
    if dqn_variant in ["dueling", "dueling_dqn", "duelingdqn"]:
        return DuelingMLPQNetwork(input_dim, action_dim, hidden_layers, activation).to(DEVICE)
    return MLP(input_dim, action_dim, hidden_layers, activation).to(DEVICE)


def build_sac_actor(model_type: str, input_dim: int, input_shape, action_dim: int,
                    hidden_layers: List[int], cnn_channels: List[int], activation: str):
    if model_type.lower() == "cnn":
        return SACCNNPolicy(input_shape, action_dim, cnn_channels, hidden_layers, activation).to(DEVICE)
    return SACMLPPolicy(input_dim, action_dim, hidden_layers, activation).to(DEVICE)


def masked_argmax(q_values: np.ndarray, mask: np.ndarray) -> int:
    valid = valid_actions(mask)
    masked = np.full_like(q_values, -1e9, dtype=np.float32)
    masked[valid] = q_values[valid]
    return int(np.argmax(masked))


@dataclass
class TrainStats:
    episode: int
    win: int
    lose: int
    draw: int
    avg_reward: float


class DQNTrainer:
    def __init__(self, args, input_shape, input_dim, action_dim):
        self.args = args
        self.model_type = args.model_type.lower()
        self.action_dim = action_dim
        self.q_net = build_q_network(args.model_type, args.dqn_variant, input_dim, input_shape, action_dim,
                                     args.hidden_layers, args.cnn_channels, args.activation)
        self.target_net = build_q_network(args.model_type, args.dqn_variant, input_dim, input_shape, action_dim,
                                          args.hidden_layers, args.cnn_channels, args.activation)
        self.target_net.load_state_dict(self.q_net.state_dict())
        self.optimizer = torch.optim.Adam(self.q_net.parameters(), lr=args.lr)
        self.buffer = ReplayBuffer(args.buffer_size)
        self.learn_steps = 0

    def act(self, obs, mask, epsilon: float):
        if random.random() < epsilon:
            return random_action(mask)
        with torch.no_grad():
            state = obs_to_tensor(obs, self.model_type)
            q = self.q_net(state).detach().cpu().numpy().reshape(-1)
        return masked_argmax(q, mask)

    def update(self):
        if len(self.buffer) < self.args.batch_size:
            return None
        obs, actions, rewards, next_obs, next_masks, dones = self.buffer.sample(self.args.batch_size)
        obs_t = batch_obs_to_tensor(obs, self.model_type)
        next_obs_t = batch_obs_to_tensor(next_obs, self.model_type)
        actions_t = torch.LongTensor(actions).unsqueeze(1).to(DEVICE)
        rewards_t = torch.FloatTensor(rewards).unsqueeze(1).to(DEVICE)
        dones_t = torch.FloatTensor(dones).unsqueeze(1).to(DEVICE)
        next_masks_t = torch.FloatTensor(next_masks).to(DEVICE)

        q = self.q_net(obs_t).gather(1, actions_t)
        with torch.no_grad():
            if self.args.dqn_variant.lower() in ["double", "double_dqn", "doubledqn"]:
                next_q_online = self.q_net(next_obs_t)
                next_q_online = next_q_online.masked_fill(next_masks_t <= 0, -1e9)
                next_actions = next_q_online.argmax(dim=1, keepdim=True)
                next_q = self.target_net(next_obs_t).gather(1, next_actions)
            else:
                next_q_all = self.target_net(next_obs_t)
                next_q_all = next_q_all.masked_fill(next_masks_t <= 0, -1e9)
                next_q = next_q_all.max(dim=1, keepdim=True)[0]
            target = rewards_t + (1.0 - dones_t) * self.args.gamma * next_q
        loss = F.mse_loss(q, target)
        self.optimizer.zero_grad()
        loss.backward()
        nn.utils.clip_grad_norm_(self.q_net.parameters(), 5.0)
        self.optimizer.step()
        self.learn_steps += 1
        if self.learn_steps % self.args.target_update_freq == 0:
            self.target_net.load_state_dict(self.q_net.state_dict())
        return float(loss.detach().cpu().item())

    def save(self, output_dir: str, config: dict):
        ensure_dir(output_dir)
        torch.save({"model_state_dict": self.q_net.state_dict(), "q_net_state_dict": self.q_net.state_dict()},
                   os.path.join(output_dir, "model.pt"))
        with open(os.path.join(output_dir, "config.json"), "w", encoding="utf-8") as f:
            json.dump(config, f, ensure_ascii=False, indent=2)


class DiscreteSACTrainer:
    def __init__(self, args, input_shape, input_dim, action_dim):
        self.args = args
        self.model_type = args.model_type.lower()
        self.action_dim = action_dim
        self.actor = build_sac_actor(args.model_type, input_dim, input_shape, action_dim,
                                     args.hidden_layers, args.cnn_channels, args.activation)
        self.q1 = build_q_network(args.model_type, "standard", input_dim, input_shape, action_dim,
                                  args.hidden_layers, args.cnn_channels, args.activation)
        self.q2 = build_q_network(args.model_type, "standard", input_dim, input_shape, action_dim,
                                  args.hidden_layers, args.cnn_channels, args.activation)
        self.tq1 = build_q_network(args.model_type, "standard", input_dim, input_shape, action_dim,
                                   args.hidden_layers, args.cnn_channels, args.activation)
        self.tq2 = build_q_network(args.model_type, "standard", input_dim, input_shape, action_dim,
                                   args.hidden_layers, args.cnn_channels, args.activation)
        self.tq1.load_state_dict(self.q1.state_dict())
        self.tq2.load_state_dict(self.q2.state_dict())
        self.actor_opt = torch.optim.Adam(self.actor.parameters(), lr=args.lr)
        self.q1_opt = torch.optim.Adam(self.q1.parameters(), lr=args.lr)
        self.q2_opt = torch.optim.Adam(self.q2.parameters(), lr=args.lr)
        self.buffer = ReplayBuffer(args.buffer_size)
        self.alpha = float(args.sac_alpha)
        self.learn_steps = 0

    def act(self, obs, mask, epsilon: float):
        if random.random() < epsilon:
            return random_action(mask)
        with torch.no_grad():
            state = obs_to_tensor(obs, self.model_type)
            logits = self.actor(state).detach().cpu().numpy().reshape(-1)
        valid = valid_actions(mask)
        masked = np.full(self.action_dim, -1e9, dtype=np.float32)
        masked[valid] = logits[valid]
        probs = np.exp(masked - np.max(masked[valid]))
        probs[np.setdiff1d(np.arange(self.action_dim), valid)] = 0.0
        probs = probs / max(probs.sum(), 1e-8)
        return int(np.random.choice(np.arange(self.action_dim), p=probs))

    def _policy_probs(self, obs_t, masks_t):
        logits = self.actor(obs_t).masked_fill(masks_t <= 0, -1e9)
        probs = F.softmax(logits, dim=1)
        log_probs = F.log_softmax(logits, dim=1)
        probs = probs * masks_t
        probs = probs / probs.sum(dim=1, keepdim=True).clamp_min(1e-8)
        log_probs = torch.log(probs.clamp_min(1e-8))
        return probs, log_probs

    def update(self):
        if len(self.buffer) < self.args.batch_size:
            return None
        obs, actions, rewards, next_obs, next_masks, dones = self.buffer.sample(self.args.batch_size)
        obs_t = batch_obs_to_tensor(obs, self.model_type)
        next_obs_t = batch_obs_to_tensor(next_obs, self.model_type)
        actions_t = torch.LongTensor(actions).unsqueeze(1).to(DEVICE)
        rewards_t = torch.FloatTensor(rewards).unsqueeze(1).to(DEVICE)
        dones_t = torch.FloatTensor(dones).unsqueeze(1).to(DEVICE)
        next_masks_t = torch.FloatTensor(next_masks).to(DEVICE)
        # 当前状态 mask 无法从 buffer 直接取，按 q 输出全动作策略训练；动作合法性主要在采样/next target 处约束。
        current_masks_t = torch.ones((obs_t.shape[0], self.action_dim), device=DEVICE)

        with torch.no_grad():
            next_probs, next_log_probs = self._policy_probs(next_obs_t, next_masks_t)
            tq = torch.min(self.tq1(next_obs_t), self.tq2(next_obs_t))
            next_v = (next_probs * (tq - self.alpha * next_log_probs)).sum(dim=1, keepdim=True)
            target_q = rewards_t + (1.0 - dones_t) * self.args.gamma * next_v

        q1_val = self.q1(obs_t).gather(1, actions_t)
        q2_val = self.q2(obs_t).gather(1, actions_t)
        q1_loss = F.mse_loss(q1_val, target_q)
        q2_loss = F.mse_loss(q2_val, target_q)
        self.q1_opt.zero_grad()
        q1_loss.backward()
        nn.utils.clip_grad_norm_(self.q1.parameters(), 5.0)
        self.q1_opt.step()
        self.q2_opt.zero_grad()
        q2_loss.backward()
        nn.utils.clip_grad_norm_(self.q2.parameters(), 5.0)
        self.q2_opt.step()

        probs, log_probs = self._policy_probs(obs_t, current_masks_t)
        q_min = torch.min(self.q1(obs_t), self.q2(obs_t))
        actor_loss = (probs * (self.alpha * log_probs - q_min)).sum(dim=1).mean()
        self.actor_opt.zero_grad()
        actor_loss.backward()
        nn.utils.clip_grad_norm_(self.actor.parameters(), 5.0)
        self.actor_opt.step()

        self.learn_steps += 1
        if self.learn_steps % self.args.target_update_freq == 0:
            self.tq1.load_state_dict(self.q1.state_dict())
            self.tq2.load_state_dict(self.q2.state_dict())
        return float((actor_loss + q1_loss + q2_loss).detach().cpu().item())

    def save(self, output_dir: str, config: dict):
        ensure_dir(output_dir)
        torch.save({
            "actor_state_dict": self.actor.state_dict(),
            "q1_state_dict": self.q1.state_dict(),
            "q2_state_dict": self.q2.state_dict(),
        }, os.path.join(output_dir, "model.pt"))
        with open(os.path.join(output_dir, "config.json"), "w", encoding="utf-8") as f:
            json.dump(config, f, ensure_ascii=False, indent=2)


def build_trainer(args, input_shape, input_dim, action_dim):
    if args.algorithm.lower() in ["sac", "discrete_sac", "sac_discrete"]:
        return DiscreteSACTrainer(args, input_shape, input_dim, action_dim)
    return DQNTrainer(args, input_shape, input_dim, action_dim)


def epsilon_by_episode(args, ep: int) -> float:
    ratio = min(1.0, ep / max(1, args.epsilon_decay_episodes))
    return float(args.epsilon_start + ratio * (args.epsilon_end - args.epsilon_start))


def play_training_episode(env_name: str, trainer, args, ep: int, seed: int):
    env = make_env(env_name)
    reset_env(env, seed=seed)
    names = list(env.possible_agents)
    train_player = names[0] if ep % 2 == 0 else names[1]
    epsilon = epsilon_by_episode(args, ep)

    last_obs = None
    last_action = None
    last_mask = None
    episode_reward = 0.0
    winner = 0

    try:
        for agent_name in env.agent_iter():
            obs, reward, termination, truncation, info = env.last()
            done = termination or truncation

            if agent_name == train_player and last_obs is not None:
                if done:
                    next_obs = np.asarray(last_obs, dtype=np.float32)
                    next_mask = np.asarray(last_mask, dtype=np.float32)
                else:
                    next_obs = np.asarray(obs["observation"], dtype=np.float32)
                    next_mask = np.asarray(obs["action_mask"], dtype=np.float32)
                trainer.buffer.add(last_obs, last_action, float(reward), next_obs, next_mask, done)
                episode_reward += float(reward)
                trainer.update()
                last_obs = None
                last_action = None
                last_mask = None
                if done:
                    if reward > 0:
                        winner = 1
                    elif reward < 0:
                        winner = -1

            if done:
                env.step(None)
                continue

            if agent_name == train_player:
                action = trainer.act(obs["observation"], obs["action_mask"], epsilon)
                last_obs = np.asarray(obs["observation"], dtype=np.float32)
                last_action = int(action)
                last_mask = np.asarray(obs["action_mask"], dtype=np.float32)
            else:
                if args.opponent == "self_play" and random.random() > args.random_opponent_prob:
                    action = trainer.act(obs["observation"], obs["action_mask"], epsilon)
                elif args.opponent == "mix" and random.random() > 0.5:
                    action = trainer.act(obs["observation"], obs["action_mask"], max(epsilon, 0.05))
                else:
                    action = random_action(obs["action_mask"])
            env.step(action)
    finally:
        env.close()
    return episode_reward, winner


def evaluate_against_random(env_name: str, trainer, episodes: int, seed: int):
    wins = loses = draws = 0
    for ep in range(episodes):
        env = make_env(env_name)
        reset_env(env, seed=seed + ep)
        names = list(env.possible_agents)
        train_player = names[0] if ep % 2 == 0 else names[1]
        final_reward = 0.0
        try:
            for agent_name in env.agent_iter():
                obs, reward, termination, truncation, info = env.last()
                done = termination or truncation
                if done:
                    if agent_name == train_player:
                        final_reward = float(reward)
                    env.step(None)
                    continue
                if agent_name == train_player:
                    action = trainer.act(obs["observation"], obs["action_mask"], 0.0)
                else:
                    action = random_action(obs["action_mask"])
                env.step(action)
        finally:
            env.close()
        if final_reward > 0:
            wins += 1
        elif final_reward < 0:
            loses += 1
        else:
            draws += 1
    return wins, loses, draws


def build_config(args, input_shape, input_dim, action_dim):
    algorithm = args.algorithm.upper()
    if algorithm in ["DISCRETE_SAC", "SAC_DISCRETE"]:
        algorithm = "SAC"
    return {
        "algorithm": algorithm,
        "model_type": args.model_type,
        "dqn_variant": args.dqn_variant,
        "input_shape": list(input_shape),
        "input_dim": int(input_dim),
        "action_dim": int(action_dim),
        "hidden_layers": args.hidden_layers,
        "cnn_channels": args.cnn_channels,
        "activation": args.activation,
        "eval_epsilon": args.eval_epsilon,
        "eval_temperature": args.eval_temperature,
        "env": normalize_env_name(args.env),
        "note": "generated by train_battle_template.py; compatible with template.py and battle_evaluator.py",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--env", default="tictactoe_v3", choices=["tictactoe_v3", "connect_four_v3", "tictactoe", "connect_four"])
    parser.add_argument("--algorithm", default="dqn", choices=["dqn", "sac", "discrete_sac"])
    parser.add_argument("--model_type", default="mlp", choices=["mlp", "cnn"])
    parser.add_argument("--dqn_variant", default="standard", choices=["standard", "double", "dueling", "double_dqn", "dueling_dqn"])
    parser.add_argument("--hidden_layers", default="128,128", help="例如 128,128 或 256,128")
    parser.add_argument("--cnn_channels", default="32,64", help="CNN通道数，例如 32,64")
    parser.add_argument("--activation", default="relu", choices=["relu", "tanh", "gelu", "leaky_relu", "elu"])
    parser.add_argument("--episodes", type=int, default=2000)
    parser.add_argument("--eval_interval", type=int, default=200)
    parser.add_argument("--eval_episodes", type=int, default=50)
    parser.add_argument("--output_dir", required=True)
    parser.add_argument("--opponent", default="mix", choices=["random", "self_play", "mix"])
    parser.add_argument("--random_opponent_prob", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=None, help="训练可选seed；不传则随机")
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--gamma", type=float, default=0.99)
    parser.add_argument("--buffer_size", type=int, default=50000)
    parser.add_argument("--batch_size", type=int, default=64)
    parser.add_argument("--target_update_freq", type=int, default=200)
    parser.add_argument("--epsilon_start", type=float, default=1.0)
    parser.add_argument("--epsilon_end", type=float, default=0.05)
    parser.add_argument("--epsilon_decay_episodes", type=int, default=1500)
    parser.add_argument("--sac_alpha", type=float, default=0.2)
    parser.add_argument("--eval_epsilon", type=float, default=0.02)
    parser.add_argument("--eval_temperature", type=float, default=1.0)
    args = parser.parse_args()

    args.env = normalize_env_name(args.env)
    args.hidden_layers = parse_int_list(args.hidden_layers) or [128, 128]
    args.cnn_channels = parse_int_list(args.cnn_channels) or [32, 64]

    seed = args.seed if args.seed is not None else random.SystemRandom().randint(1, 2 ** 31 - 1)
    random.seed(seed)
    np.random.seed(seed % (2 ** 32 - 1))
    torch.manual_seed(seed)

    input_shape, input_dim, action_dim = env_dims(args.env)
    trainer = build_trainer(args, input_shape, input_dim, action_dim)
    config = build_config(args, input_shape, input_dim, action_dim)
    config["train_seed"] = seed
    config["train_episodes"] = args.episodes
    config["opponent"] = args.opponent

    print(f"[train] env={args.env}, algorithm={args.algorithm}, model_type={args.model_type}, "
          f"variant={args.dqn_variant}, device={DEVICE}, seed={seed}")

    recent_rewards = deque(maxlen=100)
    for ep in range(1, args.episodes + 1):
        ep_seed = seed + ep * 9973 + random.randint(0, 999)
        reward, outcome = play_training_episode(args.env, trainer, args, ep, ep_seed)
        recent_rewards.append(float(reward))

        if ep % args.eval_interval == 0 or ep == args.episodes:
            wins, loses, draws = evaluate_against_random(args.env, trainer, args.eval_episodes, seed + ep * 17)
            avg_reward = sum(recent_rewards) / max(1, len(recent_rewards))
            print(f"[eval] ep={ep}, avg_recent_reward={avg_reward:.3f}, "
                  f"vs_random={wins}W/{loses}L/{draws}D, epsilon={epsilon_by_episode(args, ep):.3f}")
            # 持续保存 latest，避免中途中断丢失。
            trainer.save(args.output_dir, config)

    trainer.save(args.output_dir, config)
    print(f"[done] model saved to {args.output_dir}")
    print(f"[done] files: {os.path.join(args.output_dir, 'config.json')} , {os.path.join(args.output_dir, 'model.pt')}")


if __name__ == "__main__":
    main()
