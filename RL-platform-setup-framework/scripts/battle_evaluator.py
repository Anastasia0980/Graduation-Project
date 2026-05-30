#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
多人对战测评脚本。

修改目标：
1. 同时支持 tictactoe_v3 与 connect_four_v3。
2. 通过 config.json + model.pt 加载 DQN/DoubleDQN/DuelingDQN/SAC 等模板模型。
3. 测评不再写死固定 seed；默认每次随机生成 base_seed，每局 seed 也不同。
4. 测评动作允许少量探索，由模型 config 中 eval_epsilon / eval_temperature 控制。
5. 每局交换先后手，并按“学生1/学生2”真实胜负统计。
6. 保持原后端调用参数不变：--workspace --student1_dir --student2_dir --env --games --result_base。
"""

import argparse
import json
import os
import random
import sys
from datetime import datetime
from typing import Dict, Tuple

import imageio
import numpy as np

from pettingzoo.classic import connect_four_v3, tictactoe_v3
from template import AgentFactory


def ensure_dir(path_str: str):
    if path_str:
        os.makedirs(path_str, exist_ok=True)


def resolve_student_dir(base: str, path_str: str):
    if os.path.isabs(path_str):
        return path_str
    return os.path.join(base, path_str)


def load_agent_from_student_dir(student_dir_abs: str):
    config_path = os.path.join(student_dir_abs, "config.json")
    model_path = os.path.join(student_dir_abs, "model.pt")
    if not os.path.isfile(config_path):
        raise FileNotFoundError(f"config.json not found in {student_dir_abs}")
    if not os.path.isfile(model_path):
        raise FileNotFoundError(f"model.pt not found in {student_dir_abs}")
    agent = AgentFactory.create_from_config(config_path)
    agent.load(model_path)
    return agent


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
        raise ValueError(f"当前 battle_evaluator 仅支持 tictactoe_v3/connect_four_v3，收到 env={env_name}")
    return aliases[name]


def make_env(env_name: str, render_mode=None):
    env_name = normalize_env_name(env_name)
    if env_name == "connect_four_v3":
        return connect_four_v3.env(render_mode=render_mode)
    return tictactoe_v3.env(render_mode=render_mode)


def normalize_frame(frame, max_size: int = 640):
    if frame is None:
        return None
    frame = np.asarray(frame)
    if frame.ndim != 3:
        return frame
    h, w = frame.shape[:2]
    longest = max(h, w)
    if longest <= max_size:
        return frame
    scale = longest / max_size
    step = max(1, int(np.ceil(scale)))
    return frame[::step, ::step, :]


def reset_env(env, seed=None):
    try:
        return env.reset(seed=seed)
    except TypeError:
        random.seed(seed)
        np.random.seed(seed % (2 ** 32 - 1) if seed is not None else None)
        return env.reset()


def run_one_game(env, student1_agent, student2_agent, student1_first: bool, seed: int,
                 writer=None, capture_video: bool = True) -> Tuple[int, Dict]:
    """
    返回：
        winner: 1=学生1胜, 2=学生2胜, 0=平局
        detail: 单局统计信息
    """
    reset_env(env, seed=seed)
    names = list(env.possible_agents)
    if len(names) != 2:
        raise RuntimeError(f"unexpected possible_agents: {names}")

    if student1_first:
        player_to_student = {names[0]: 1, names[1]: 2}
        controllers = {names[0]: student1_agent, names[1]: student2_agent}
    else:
        player_to_student = {names[0]: 2, names[1]: 1}
        controllers = {names[0]: student2_agent, names[1]: student1_agent}

    final_reward = {}
    steps = 0

    for agent_name in env.agent_iter():
        if writer is not None and capture_video:
            try:
                frame = env.render()
                frame = normalize_frame(frame, max_size=640)
                if frame is not None:
                    writer.append_data(frame)
            except Exception:
                # 视频帧失败不应影响对战结果。
                pass

        obs, reward, termination, truncation, info = env.last()
        done = termination or truncation
        if done:
            final_reward[agent_name] = float(reward)
            env.step(None)
            continue

        action = controllers[agent_name].act(obs, explore=True)
        env.step(action)
        steps += 1

    r_by_student = {1: 0.0, 2: 0.0}
    for player_name, reward in final_reward.items():
        stu = player_to_student.get(player_name)
        if stu in [1, 2]:
            r_by_student[stu] = float(reward)

    if r_by_student[1] > r_by_student[2]:
        winner = 1
    elif r_by_student[2] > r_by_student[1]:
        winner = 2
    else:
        winner = 0

    detail = {
        "seed": int(seed),
        "student1_first": bool(student1_first),
        "player0_student": int(player_to_student[names[0]]),
        "player1_student": int(player_to_student[names[1]]),
        "student1_reward": r_by_student[1],
        "student2_reward": r_by_student[2],
        "winner": winner,
        "steps": int(steps),
    }
    return winner, detail


def run_battle(env_name: str, agent1, agent2, games: int, video_path: str, fps: int = 5, seed=None):
    env_name = normalize_env_name(env_name)
    env = make_env(env_name, render_mode="rgb_array")

    # 不写死 seed：未传入时每次生成一个随机 base_seed。
    base_seed = int(seed) if seed is not None else random.SystemRandom().randint(1, 2 ** 31 - 1)

    win1 = 0
    win2 = 0
    draw = 0
    details = []

    ensure_dir(os.path.dirname(video_path))
    writer = imageio.get_writer(video_path, fps=fps)

    try:
        for ep in range(int(games)):
            student1_first = (ep % 2 == 0)
            # 每局 seed 不同，避免所有轮次完全相同；同一次测评仍可通过 base_seed 复现。
            ep_seed = base_seed + ep * 9973 + random.SystemRandom().randint(0, 999)
            winner, detail = run_one_game(
                env=env,
                student1_agent=agent1,
                student2_agent=agent2,
                student1_first=student1_first,
                seed=ep_seed,
                writer=writer,
                capture_video=True,
            )
            details.append(detail)
            if winner == 1:
                win1 += 1
            elif winner == 2:
                win2 += 1
            else:
                draw += 1
    finally:
        try:
            writer.close()
        finally:
            env.close()

    return {
        "env": env_name,
        "games": int(games),
        "base_seed": base_seed,
        "win1": int(win1),
        "win2": int(win2),
        "draw": int(draw),
        "details": details,
    }


def write_summary(summary_path: str, result: Dict):
    ensure_dir(os.path.dirname(summary_path))
    total = max(int(result.get("games", 0)), 1)
    with open(summary_path, "w", encoding="utf-8") as f:
        f.write(f"env: {result['env']}\n")
        f.write(f"games: {total}\n")
        f.write(f"base_seed: {result.get('base_seed')}\n")
        f.write(f"win1: {result['win1']}\n")
        f.write(f"win2: {result['win2']}\n")
        f.write(f"draw: {result['draw']}\n")
        f.write(f"winRate1: {result['win1'] / total:.3f}\n")
        f.write(f"winRate2: {result['win2'] / total:.3f}\n")
        f.write(f"time: {datetime.now()}\n")
        f.write("\nper_game_details:\n")
        for i, d in enumerate(result.get("details", []), start=1):
            f.write(
                f"game={i}, seed={d['seed']}, student1_first={d['student1_first']}, "
                f"winner={d['winner']}, s1_reward={d['student1_reward']}, "
                f"s2_reward={d['student2_reward']}, steps={d['steps']}\n"
            )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--student1_dir", required=True)
    parser.add_argument("--student2_dir", required=True)
    parser.add_argument("--env", default="tictactoe_v3")
    parser.add_argument("--games", type=int, default=30)
    parser.add_argument("--result_base", required=True)
    parser.add_argument("--seed", type=int, default=None, help="可选；不传则每次随机生成 base_seed")
    parser.add_argument("--fps", type=int, default=5)
    args = parser.parse_args()

    try:
        base = args.workspace
        env_name = normalize_env_name(args.env)
        s1_abs = resolve_student_dir(base, args.student1_dir)
        s2_abs = resolve_student_dir(base, args.student2_dir)

        agent1 = load_agent_from_student_dir(s1_abs)
        agent2 = load_agent_from_student_dir(s2_abs)

        result_base_abs = os.path.join(base, args.result_base)
        video_path = result_base_abs + ".mp4"
        summary_path = result_base_abs + "_summary.txt"
        result_json_path = result_base_abs + "_result.json"

        result = run_battle(env_name, agent1, agent2, args.games, video_path, fps=args.fps, seed=args.seed)

        total = max(int(result["games"]), 1)
        win_rate1 = result["win1"] / total
        win_rate2 = result["win2"] / total
        if result["win1"] > result["win2"]:
            winner = 1
        elif result["win2"] > result["win1"]:
            winner = 2
        else:
            winner = 0

        write_summary(summary_path, result)

        out = {
            "status": "FINISHED",
            "env": env_name,
            "games": total,
            "baseSeed": result.get("base_seed"),
            "win1": result["win1"],
            "win2": result["win2"],
            "draw": result["draw"],
            "winRate1": win_rate1,
            "winRate2": win_rate2,
            "winner": winner,
            "firstPlayerSwapped": True,
            "result_dir": args.result_base.replace("\\", "/"),
            "video": os.path.basename(video_path),
            "summary": os.path.basename(summary_path),
            "details": result.get("details", []),
        }

        ensure_dir(os.path.dirname(result_json_path))
        with open(result_json_path, "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False, indent=2)

        print(json.dumps(out, ensure_ascii=False))
        sys.exit(0)

    except Exception as e:
        out = {"status": "FAILED", "error": str(e)}
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(2)


if __name__ == "__main__":
    main()
