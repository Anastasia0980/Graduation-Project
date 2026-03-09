#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import json
import os
import sys
from datetime import datetime

import numpy as np
import imageio

from pettingzoo.classic.tictactoe_v3 import env as ttt_env
from template import AgentFactory


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


def ensure_dir(p: str):
    os.makedirs(p, exist_ok=True)


def load_agent_from_student_dir(student_dir_abs: str):
    """
    学生目录结构（由后端保存）：
      student_dir_abs/
        config.json
        model.pt
    """
    config_path = os.path.join(student_dir_abs, "config.json")
    model_path = os.path.join(student_dir_abs, "model.pt")

    if not os.path.isfile(config_path):
        raise FileNotFoundError(f"config.json not found in {student_dir_abs}")
    if not os.path.isfile(model_path):
        raise FileNotFoundError(f"model.pt not found in {student_dir_abs}")

    agent = AgentFactory.create_from_config(config_path)
    agent.load(model_path)
    return agent


def run_battle_tictactoe(agent1, agent2, games: int, video_path: str, fps: int = 5):
    """
    agent1 vs agent2
    轮换先手：
      偶数局 agent1 控 player_0
      奇数局 agent1 控 player_1
    胜负判定：比较双方最终 reward（通常 1/-1/0）
    """
    env = ttt_env(render_mode="rgb_array")
    names = env.possible_agents
    if len(names) != 2:
        raise RuntimeError(f"unexpected possible_agents: {names}")

    win1 = 0
    win2 = 0
    draw = 0
    frames = []

    for ep in range(games):
        env.reset()

        # 轮换先手：映射“agent1/agent2”到环境角色
        if ep % 2 == 0:
            a1_name, a2_name = names[0], names[1]
        else:
            a1_name, a2_name = names[1], names[0]

        controllers = {a1_name: agent1, a2_name: agent2}
        final_reward = {}

        for agent_name in env.agent_iter():
            frame = env.render()
            if frame is not None:
                frames.append(frame)

            obs, reward, termination, truncation, info = env.last()
            done = termination or truncation

            if done:
                final_reward[agent_name] = reward
                env.step(None)
                continue

            action = controllers[agent_name].act(obs)
            env.step(action)

        r1 = final_reward.get(a1_name, 0)  # agent1 在该局对应的角色 reward
        r2 = final_reward.get(a2_name, 0)

        if r1 > r2:
            win1 += 1
        elif r1 < r2:
            win2 += 1
        else:
            draw += 1

    if frames:
        ensure_dir(os.path.dirname(video_path))
        with imageio.get_writer(video_path, fps=fps) as writer:
            for fr in frames:
                writer.append_data(fr)

    return win1, win2, draw


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", required=True, help="workspace 绝对路径")
    parser.add_argument("--student1_dir", required=True, help="student1 相对 workspace 的目录，如 uploads/1/xxx")
    parser.add_argument("--student2_dir", required=True, help="student2 相对 workspace 的目录")
    parser.add_argument("--env", default="tictactoe_v3")
    parser.add_argument("--games", type=int, default=50)
    parser.add_argument("--result_dir", required=True, help="相对 workspace 的结果目录，如 results/123")
    args = parser.parse_args()

    try:
        base = args.workspace
        s1_abs = os.path.join(base, args.student1_dir)
        s2_abs = os.path.join(base, args.student2_dir)

        agent1 = load_agent_from_student_dir(s1_abs)
        agent2 = load_agent_from_student_dir(s2_abs)

        # 结果目录（务必写 video_0.mp4 以匹配你后端取视频逻辑）
        result_abs = os.path.join(base, args.result_dir)
        ensure_dir(result_abs)

        video_name = "video_0.mp4"
        summary_name = "summary.txt"
        video_path = os.path.join(result_abs, video_name)
        summary_path = os.path.join(result_abs, summary_name)

        if args.env not in ["tictactoe_v3", "tictactoe", "TicTacToe"]:
            eprint(f"[warn] env={args.env} not implemented, fallback to tictactoe_v3")

        win1, win2, draw = run_battle_tictactoe(agent1, agent2, args.games, video_path, fps=5)

        total = max(args.games, 1)
        winRate1 = win1 / total
        winRate2 = win2 / total

        # winner: 0 draw / 1 agent1 / 2 agent2
        if win1 > win2:
            winner = 1
        elif win2 > win1:
            winner = 2
        else:
            winner = 0

        with open(summary_path, "w", encoding="utf-8") as f:
            f.write(f"env: tictactoe_v3\n")
            f.write(f"games: {total}\n")
            f.write(f"win1: {win1}\n")
            f.write(f"win2: {win2}\n")
            f.write(f"draw: {draw}\n")
            f.write(f"winRate1: {winRate1:.3f}\n")
            f.write(f"winRate2: {winRate2:.3f}\n")
            f.write(f"time: {datetime.now()}\n")

        out = {
            "status": "FINISHED",
            "env": "tictactoe_v3",
            "games": total,
            "win1": win1,
            "win2": win2,
            "draw": draw,
            "winRate1": winRate1,
            "winRate2": winRate2,
            "winner": winner,
            "result_dir": args.result_dir.replace("\\", "/"),
            "video": video_name,
            "summary": summary_name
        }

        # stdout 最后一行 JSON（给 Java 写入 evaluation_result.detailed_results 用）
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(0)

    except Exception as e:
        out = {"status": "FAILED", "error": str(e)}
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(2)


if __name__ == "__main__":
    main()