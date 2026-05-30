import os
import json
import time
import random
import argparse
from pathlib import Path

import numpy as np
import torch

try:
    import gymnasium as gym
except ImportError:
    import gym

from agents.dqn_agent import DQNAgent


def set_seed(seed: int):
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def reset_env(env, seed=None):
    if seed is not None:
        out = env.reset(seed=seed)
    else:
        out = env.reset()
    if isinstance(out, tuple):
        state, _ = out
    else:
        state = out
    return state


def step_env(env, action):
    out = env.step(action)
    if len(out) == 5:
        next_state, reward, terminated, truncated, info = out
        done = terminated or truncated
    else:
        next_state, reward, done, info = out
    return next_state, reward, done, info


def evaluate_agent(env_id: str, agent: DQNAgent, episodes: int = 5, seed: int = 42) -> float:
    eval_env = gym.make(env_id)
    rewards = []

    old_epsilon = agent.epsilon
    agent.epsilon = 0.0

    for i in range(episodes):
        state = reset_env(eval_env, seed + i)
        done = False
        total_reward = 0.0

        while not done:
            action = agent.choose_action(state)
            next_state, reward, done, _ = step_env(eval_env, action)
            total_reward += reward
            state = next_state

        rewards.append(total_reward)

    agent.epsilon = old_epsilon
    eval_env.close()
    return float(np.mean(rewards))


def save_config(
    save_dir: Path,
    env_id: str,
    algorithm: str,
    train_episodes: int,
    batch_size: int,
    gamma: float,
    lr: float,
    epsilon_start: float,
    epsilon_end: float,
    epsilon_decay: float,
    target_update_freq: int,
    buffer_capacity: int,
    seed: int,
    best_model_name: str,
):
    config = {
        "algorithm": algorithm,
        "env": env_id,
        "train_episodes": train_episodes,
        "batch_size": batch_size,
        "gamma": gamma,
        "lr": lr,
        "epsilon_start": epsilon_start,
        "epsilon_end": epsilon_end,
        "epsilon_decay": epsilon_decay,
        "target_update_freq": target_update_freq,
        "buffer_capacity": buffer_capacity,
        "seed": seed,
        "best_model": best_model_name
    }

    config_path = save_dir / "config.json"
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(config, f, ensure_ascii=False, indent=2)

    return config_path


def main():
    parser = argparse.ArgumentParser(description="Train a single-agent DQN model for LunarLander-v3")
    parser.add_argument("--env", type=str, default="LunarLander-v3")
    parser.add_argument("--episodes", type=int, default=500)
    parser.add_argument("--max_steps", type=int, default=1000)
    parser.add_argument("--batch_size", type=int, default=64)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--gamma", type=float, default=0.99)
    parser.add_argument("--epsilon_start", type=float, default=1.0)
    parser.add_argument("--epsilon_end", type=float, default=0.05)
    parser.add_argument("--epsilon_decay", type=float, default=0.995)
    parser.add_argument("--target_update_freq", type=int, default=100)
    parser.add_argument("--buffer_capacity", type=int, default=50000)
    parser.add_argument("--warmup_steps", type=int, default=1000)
    parser.add_argument("--eval_every", type=int, default=20)
    parser.add_argument("--eval_episodes", type=int, default=5)
    parser.add_argument("--save_dir", type=str, default="trained_single/dqn_lunar")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    set_seed(args.seed)

    save_dir = Path(args.save_dir)
    save_dir.mkdir(parents=True, exist_ok=True)

    env = gym.make(args.env)
    state_dim = env.observation_space.shape[0]
    action_dim = env.action_space.n

    agent = DQNAgent(
        state_dim=state_dim,
        action_dim=action_dim,
        lr=args.lr,
        gamma=args.gamma,
        epsilon=args.epsilon_start,
        target_update_freq=args.target_update_freq,
        buffer_capacity=args.buffer_capacity,
    )

    best_eval_reward = -float("inf")
    global_step = 0
    reward_history = []

    start_time = time.time()

    for episode in range(1, args.episodes + 1):
        state = reset_env(env, args.seed + episode)
        done = False
        episode_reward = 0.0
        steps = 0

        while not done and steps < args.max_steps:
            action = agent.choose_action(state)
            next_state, reward, done, _ = step_env(env, action)

            agent.buffer.add(state, action, reward, next_state, done)

            if global_step > args.warmup_steps:
                agent.update(args.batch_size)

            state = next_state
            episode_reward += reward
            steps += 1
            global_step += 1

        reward_history.append(episode_reward)

        agent.epsilon = max(args.epsilon_end, agent.epsilon * args.epsilon_decay)

        if episode % 10 == 0:
            avg10 = float(np.mean(reward_history[-10:]))
            print(f"[Train] Episode {episode:4d} | reward={episode_reward:8.2f} | avg10={avg10:8.2f} | epsilon={agent.epsilon:.4f}")

        if episode % args.eval_every == 0:
            eval_reward = evaluate_agent(args.env, agent, episodes=args.eval_episodes, seed=args.seed + 10000 + episode)
            print(f"[Eval ] Episode {episode:4d} | mean_reward={eval_reward:8.2f}")

            latest_model_path = save_dir / "latest_model.pth"
            torch.save(agent.q_network.state_dict(), latest_model_path)

            if eval_reward > best_eval_reward:
                best_eval_reward = eval_reward
                best_model_path = save_dir / "best_model.pth"
                torch.save(agent.q_network.state_dict(), best_model_path)
                print(f"[Save ] New best model saved: {best_model_path}")

    env.close()

    elapsed = time.time() - start_time
    print(f"Training finished in {elapsed:.2f}s")
    print(f"Best eval reward: {best_eval_reward:.2f}")

    config_path = save_config(
        save_dir=save_dir,
        env_id=args.env,
        algorithm="dqn",
        train_episodes=args.episodes,
        batch_size=args.batch_size,
        gamma=args.gamma,
        lr=args.lr,
        epsilon_start=args.epsilon_start,
        epsilon_end=args.epsilon_end,
        epsilon_decay=args.epsilon_decay,
        target_update_freq=args.target_update_freq,
        buffer_capacity=args.buffer_capacity,
        seed=args.seed,
        best_model_name="best_model.pth",
    )
    print(f"Config saved to: {config_path}")


if __name__ == "__main__":
    main()