import os
import json
import numpy as np
import imageio
import sys
from datetime import datetime
from pettingzoo.classic.tictactoe_v3 import env as ttt_env

from template import AgentFactory


# =========================
# 创建结果目录
# =========================

def create_result_folder():
    base_dir = "result"
    os.makedirs(base_dir, exist_ok=True)

    existing = [int(d) for d in os.listdir(base_dir) if d.isdigit()]
    next_id = max(existing) + 1 if existing else 1

    new_folder = os.path.join(base_dir, str(next_id))
    os.makedirs(new_folder)

    return new_folder


# =========================
# 加载模型
# =========================

def load_agent(student_folder):
    config_path = os.path.join(student_folder, "config.json")

    with open(config_path, "r") as f:
        config = json.load(f)

    algorithm = config.get("algorithm")
    agent = AgentFactory.create_from_config(config_path)

    if algorithm == "DQN":
        model_path = os.path.join(student_folder, "model.pt")
    elif algorithm == "QLearning":
        model_path = os.path.join(student_folder, "q_table.pkl")
    else:
        raise ValueError("未知算法")

    agent.load(model_path)
    return agent


# =========================
# 对战 + 录像
# =========================

def evaluate(student1_folder, student2_folder, games=50):

    result_folder = create_result_folder()
    video_path = os.path.join(result_folder, "game.mp4")
    summary_path = os.path.join(result_folder, "summary.txt")

    agent1 = load_agent(student1_folder)
    agent2 = load_agent(student2_folder)

    env = ttt_env(render_mode="rgb_array")

    win1 = 0
    win2 = 0
    draw = 0

    frames = []

    for game in range(games):

        env.reset()
        agent_names = env.possible_agents

        # 🔥 公平轮换先手
        if game % 2 == 0:
            current_agents = {
                agent_names[0]: agent1,
                agent_names[1]: agent2
            }
            player1_name = agent_names[0]
            player2_name = agent_names[1]
        else:
            current_agents = {
                agent_names[0]: agent2,
                agent_names[1]: agent1
            }
            player1_name = agent_names[1]
            player2_name = agent_names[0]

        final_reward_dict = {}

        for agent_name in env.agent_iter():

            frame = env.render()
            frames.append(frame)

            obs, reward, termination, truncation, info = env.last()
            done = termination or truncation

            if done:
                # 🔥 记录最终 reward
                final_reward_dict[agent_name] = reward
                env.step(None)
                continue

            action = current_agents[agent_name].act(obs)
            env.step(action)

        # 🔥 正确统计胜负
        r1 = final_reward_dict.get(player1_name, 0)
        r2 = final_reward_dict.get(player2_name, 0)

        if r1 > r2:
            win1 += 1
        elif r1 < r2:
            win2 += 1
        else:
            draw += 1

    # 保存视频
    with imageio.get_writer(video_path, fps=5) as writer:
        for frame in frames:
            writer.append_data(frame)

    # 保存结果
    with open(summary_path, "w", encoding="utf-8") as f:
        f.write(f"总局数: {games}\n")
        f.write(f"学生1胜利: {win1}\n")
        f.write(f"学生2胜利: {win2}\n")
        f.write(f"平局: {draw}\n")
        f.write(f"学生1胜率: {win1 / games:.3f}\n")
        f.write(f"学生2胜率: {win2 / games:.3f}\n")
        f.write(f"时间: {datetime.now()}\n")

    print("对战完成")
    print("结果已保存到:", result_folder)


# =========================
# 主函数
# =========================

if __name__ == "__main__":

    if len(sys.argv) < 3:
        print("错误：没有接收到学生路径参数")
        sys.exit(1)

    student1_path = sys.argv[1]
    student2_path = sys.argv[2]

    print("student1路径:", student1_path)
    print("student2路径:", student2_path)

    evaluate(student1_path, student2_path, games=50)