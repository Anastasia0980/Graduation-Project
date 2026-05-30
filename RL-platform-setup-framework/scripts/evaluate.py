#!/usr/bin/env python3
"""
统一评测脚本：
- 支持 Mujoco HalfCheetah (DDPG)
- 支持 LunarLander (DQN / PPO / PPO-GAE)
- 统一输出 JSON 到 stdout，便于后端调用。
- 调用示例：
    python evaluate.py --env LunarLander-v3 --agent dqn --model_name DDQN_LLdV2_250.pth --episodes 10 --workspace E:\2025-2026\GP\LunarLander-RL-Comparison --render_video --baseline_model_path LunarLander-v3\easy\dqn\dqn_episode_100.pth
"""

import os
import sys
import argparse
import json
from datetime import datetime

# 后端异步测评不需要弹出 SDL/pygame 窗口；录制视频时也使用无窗口渲染。
os.environ.setdefault("PYGAME_HIDE_SUPPORT_PROMPT", "1")
os.environ.setdefault("SDL_VIDEODRIVER", "dummy")
os.environ.setdefault("SDL_AUDIODRIVER", "dummy")

import numpy as np

# 优先使用 gymnasium，如不可用再退回 gym
try:
    import gymnasium as gym
except ImportError:  # pragma: no cover - 运行环境中若无 gymnasium 则退回 gym
    import gym

import torch
import ffmpeg
import glob
import shutil
from pathlib import Path
from typing import Optional

from agents.dqn_agent import DQNAgent
from agents.dqn_agents import (
    DoubleDQNAgent,
    DistribDQNAgent,
    PrioritizedDoubleDQNAgent,
    RainbowAgent,
)
from agents.ppo_agent import PPOAgent
from agents.ppo_gae_agent import PPO_GAE_Agent
from agents.ddpg_agent import DDPGAgent
from policy_wrapper import _PolicyWrapper, DQNPolicy, PPOPolicy, PPOGAEPolicy, DDPGPolicy


STEPS_PER_EPISODE = 1000

SUPPORTED_AGENT_PREFIXES = (
    "distribdqn",
    "priorddqn",
    "rainbow",
    "ppo_gae",
    "ppogae",
    "ddqn",
    "ddpg",
    "dqn",
    "ppo",
)


def _maybe_add_mujoco_dll_directory() -> None:
    """
    仅在本地存在 mujoco200 路径时添加 DLL 目录。
    保留 evaluate_cheetah.py 中的行为以便 HalfCheetah 正常运行。
    """
    mujoco_bin = r"C:\Users\qtq\.mujoco\mujoco200\bin"
    if os.path.isdir(mujoco_bin):
        try:
            os.add_dll_directory(mujoco_bin)
        except Exception:
            # 在非 Windows 或无权限时静默忽略
            pass


# =========================
# 最小化新增 1：统一结果路径辅助函数
# =========================
def normalize_result_base(result_base: str) -> str:
    return (result_base or "").strip().replace("\\", "/")


def build_baseline_result_base(result_base: str) -> str:
    base = normalize_result_base(result_base)
    parent = os.path.dirname(base)
    return (os.path.join(parent, "baseline_video")).replace("\\", "/")


def make_env(env_id: str,
             model_name: str,
             realtime_render: bool = False,
             render_video: bool = False,
             task_id: Optional[str] = None,
             stage_spec_path: Optional[str] = None,
             result_base: Optional[str] = None,
             workspace: Optional[str] = None):
    """
    统一的环境创建函数，支持三种模式：
    - 实时渲染（human）
    - 录制视频（RecordVideo，rgb_array）
    - 无渲染（默认）

    返回 (env, result_dir)：
    - env: gym 环境实例
    - result_dir: 若录制视频，则为相对 workspace 的视频基路径；否则为 None
    """
    result_dir = None

    render_mode = "human" if realtime_render else ("rgb_array" if render_video else None)
    if env_id == "LunarLander-v3":
        spec_path = (stage_spec_path or "").strip()
        if spec_path:
            try:
                import lunar_task_env  # type: ignore
            except ImportError as e:
                raise ImportError(
                    "找不到 lunar_task_env.py。"
                    "请确认 --workspace 指向包含该文件的目录。"
                ) from e
            abs_spec = os.path.abspath(spec_path)
            with open(abs_spec, encoding="utf-8") as f:
                spec = json.load(f)
            if not isinstance(spec, dict):
                raise ValueError("stage_spec JSON 必须是对象")
            env = lunar_task_env.make_lunar_env_from_spec(spec, render_mode=render_mode)
        else:
            normalized_task_id = (task_id or "").strip().upper()
            if not normalized_task_id:
                raise ValueError("闯关模式要求传入 --task_id（T1...T10）或 --stage_spec_path")
            try:
                import lunar_task_env  # type: ignore
                env = lunar_task_env.make_lunar_env(normalized_task_id, render_mode=render_mode)
            except ImportError as e:
                raise ImportError(
                    "找不到 lunar_task_env.py。"
                    "请确认 --workspace 指向包含该文件的目录。"
                ) from e
    else:
        raise ValueError(f"Unsupported environment: {env_id}")

    # =========================
    # 最小化修改 2：录屏目录不再自己猜，严格使用后端传入的 result_base
    # =========================
    if render_video:
        result_base = normalize_result_base(result_base)
        if not result_base:
            raise ValueError("render_video 模式下必须提供 --result_base")

        if workspace is None:
            raise ValueError("render_video 模式下必须提供 workspace")

        video_folder = os.path.join(workspace, os.path.dirname(result_base))
        name_prefix = os.path.basename(result_base)

        os.makedirs(video_folder, exist_ok=True)
        env = gym.wrappers.RecordVideo(
            env,
            video_folder=video_folder,
            episode_trigger=lambda eid: eid < 4,
            name_prefix=name_prefix,
        )
        # 注意：这里返回的是相对 workspace 的“基路径”，供后端 later 拼接 .mp4
        result_dir = result_base

    return env, result_dir


def load_policy(env, agent_name: str, model_path: str) -> _PolicyWrapper:
    # 按 agent_name 加载不同的策略，并统一成 act(state) 接口。
    agent_name = normalize_agent_name(agent_name)
    obs_space = env.observation_space
    act_space = env.action_space

    if agent_name == "ddpg":
        if not hasattr(act_space, "shape") or act_space.shape is None:
            raise ValueError("DDPG 需要连续动作空间（Box），当前环境动作空间不兼容。")

        state_dim = obs_space.shape[0]
        action_dim = act_space.shape[0]
        agent = DDPGAgent(state_dim, action_dim)
        agent.load(model_path)
        return DDPGPolicy(agent)

    state_dim = obs_space.shape[0]
    if hasattr(act_space, "n"):
        action_dim = act_space.n
    else:
        raise ValueError("当前环境的动作空间不是离散型，不能用于 DQN/PPO/PPO-GAE。")

    if agent_name == "dqn":
        agent = DQNAgent(state_dim, action_dim, epsilon=0.0)
        agent.q_network.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.q_network.eval()
        return DQNPolicy(agent)

    if agent_name == "ddqn":
        agent = DoubleDQNAgent(state_dim, action_dim, epsilon=0.0)
        agent.q_network.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.q_network.eval()
        return DQNPolicy(agent)

    if agent_name == "distribdqn":
        agent = DistribDQNAgent(state_dim, action_dim, epsilon=0.0)
        agent.q_network.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.q_network.eval()
        return DQNPolicy(agent)

    if agent_name == "priorddqn":
        agent = PrioritizedDoubleDQNAgent(state_dim, action_dim, epsilon=0.0)
        agent.q_network.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.q_network.eval()
        return DQNPolicy(agent)

    if agent_name == "rainbow":
        agent = RainbowAgent(state_dim, action_dim)
        agent.q_network.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.q_network.eval()
        agent.set_training_mode(False)
        return DQNPolicy(agent)

    if agent_name == "ppo":
        agent = PPOAgent(state_dim, action_dim)
        agent.policy.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.policy.eval()
        return PPOPolicy(agent)

    if agent_name in ("ppo_gae", "ppo-gae"):
        agent = PPO_GAE_Agent(state_dim, action_dim)
        agent.policy.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.policy.eval()
        return PPOGAEPolicy(agent)

    raise ValueError(f"Unsupported agent type: {agent_name}")


def normalize_agent_name(agent_name: str) -> str:
    """
    将输入算法名归一化为评测脚本支持的基础算法：
    - 允许输入带后缀的名字（如 ddqn_steps_50000）
    - 允许 ppo-gae / ppo_gae / ppogae
    """
    text = str(agent_name or "").strip().lower()
    if not text:
        raise ValueError("agent name is empty")

    canonical = text.replace("-", "_")
    if canonical == "ppogae":
        return "ppo_gae"

    for prefix in SUPPORTED_AGENT_PREFIXES:
        if canonical == prefix or canonical.startswith(prefix + "_"):
            return "ppo_gae" if prefix in ("ppo_gae", "ppogae") else prefix

    raise ValueError(
        f"Unsupported agent type: {agent_name}. "
        "Supported: dqn / ddqn / distribdqn / priorddqn / rainbow / ppo / ppo_gae / ddpg"
    )


def run_episodes(env, policy: _PolicyWrapper, num_episodes: int, max_steps=None):
    rewards = []
    for i in range(num_episodes):
        reset_out = env.reset()
        if isinstance(reset_out, (list, tuple)):
            state = reset_out[0]
        else:
            state = reset_out

        total_reward = 0.0
        step = 0

        while True:
            action = policy.act(state)
            step_out = env.step(action)

            if len(step_out) == 5:
                state, reward, terminated, truncated, _ = step_out
                done = terminated or truncated
            else:
                state, reward, done, _ = step_out

            total_reward += float(reward)
            step += 1

            if done:
                break
            if max_steps is not None and step >= max_steps:
                break
        rewards.append(total_reward)
    return rewards


def videoConcat(result_dir):
    # 将 RecordVideo 生成的 episode 视频整理为后端可读取的 result_dir.mp4。
    # 注意：ffmpeg-python 仍然需要系统中存在 ffmpeg.exe；若未安装，则退化为直接使用第一段视频，
    # 避免因为 [WinError 2] 让整个单人测评失败。
    if result_dir is None:
        return

    video_files = sorted(glob.glob(result_dir + "-episode-*.mp4"))
    if not video_files:
        raise FileNotFoundError("No episode videos found!")

    output_path = result_dir + ".mp4"

    if shutil.which("ffmpeg") is None:
        if os.path.exists(output_path):
            os.remove(output_path)
        os.replace(video_files[0], output_path)
        for vf in video_files[1:]:
            try:
                os.remove(vf)
            except OSError:
                pass
        return

    filelist_path = result_dir + "_filelist.txt"
    with open(filelist_path, 'w', encoding="utf-8") as f:
        for vf in video_files:
            f.write(f"file '{os.path.abspath(vf)}'\n")

    (
        ffmpeg
        .input(filelist_path, format='concat', safe=0)
        .output(output_path)
        .run(overwrite_output=True)
    )

    for vf in video_files:
        os.remove(vf)
    try:
        os.remove(filelist_path)
    except OSError:
        pass


def video_side_by_side(student_video: str, baseline_video: str, output_path: str):
    # 将两个 mp4 水平拼接成一个对比视频：左侧 student，右侧 baseline。
    # 若系统未安装 ffmpeg.exe，则保留学生视频作为最终视频，避免测评失败。
    if not os.path.isfile(student_video):
        return

    if shutil.which("ffmpeg") is None or not os.path.isfile(baseline_video):
        if os.path.abspath(student_video) != os.path.abspath(output_path):
            if os.path.exists(output_path):
                os.remove(output_path)
            os.replace(student_video, output_path)
        if os.path.isfile(baseline_video):
            try:
                os.remove(baseline_video)
            except OSError:
                pass
        return

    v1 = ffmpeg.input(student_video)
    v2 = ffmpeg.input(baseline_video)

    joined = ffmpeg.filter([v1, v2], 'hstack', inputs=2)
    (
        ffmpeg
        .output(joined, output_path)
        .run(overwrite_output=True)
    )

    os.remove(student_video)
    os.remove(baseline_video)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Unified RL evaluation: run episodes and output JSON.")
    parser.add_argument(
        "--env",
        required=True,
        help=(
            "Gym/Gymnasium 环境 id，例如 HalfCheetah-v2、LunarLander-v3，"
            "或课程任务 LunarLander-T1-v0 … LunarLander-T6-v0（需 workspace 含 lunar_task_env.py）"
        ),
    )
    parser.add_argument("--agent", required=True,
                        help="Agent type: ddpg / dqn / ddqn / distribdqn / priorddqn / rainbow / ppo / ppo_gae")
    parser.add_argument("--baseline_agent", default=None,
                        help="Optional baseline agent type (defaults to --agent)")
    parser.add_argument("--model_name", required=True,
                        help="Path or name of the saved model weights.")
    parser.add_argument("--episodes", type=int, default=100,
                        help="Number of evaluation episodes.")
    parser.add_argument("--workspace", default=None,
                        help="Project root where code and models live (default: cwd).")
    parser.add_argument("--realtime_render", action="store_true",
                        help="Render in realtime (human).")
    parser.add_argument("--render_video", action="store_true",
                        help="Record video to result_base directory.")
    parser.add_argument("--baseline_model_path", default=None,
                        help="Optional baseline model path")
    parser.add_argument("--config_path", default=None,
                        help="Optional config file path associated with model")
    parser.add_argument("--task_id", default="",
                        help="Curriculum task id（legacy: T1…T10）；与 --stage_spec_path 二选一或同时用于标注")
    parser.add_argument("--stage_spec_path", default="",
                        help="关卡环境参数 JSON 文件路径（A2，与 lunar_task_env.make_lunar_env_from_spec 对齐）")
    # =========================
    # 最小化新增 3：由后端传入统一结果基路径
    # 例如 results/62/video_0
    # =========================
    parser.add_argument("--result_base", default="",
                        help="相对 workspace 的结果基路径，例如 results/62/video_0")
    return parser.parse_args(argv)


def main(argv=None):
    _maybe_add_mujoco_dll_directory()

    args = parse_args(argv)

    if not (args.stage_spec_path or "").strip() and not (args.task_id or "").strip():
        print(json.dumps({"status": "FAILED", "error": "需要 --stage_spec_path 或 --task_id"}, ensure_ascii=False))
        return 1

    workspace = os.path.abspath(args.workspace or os.getcwd())
    if workspace not in sys.path:
        sys.path.insert(0, workspace)
    os.chdir(workspace)

    result = {
        "status": "FINISHED",
        "task_id": (args.task_id.strip() if args.task_id else None),
        "student_avg_reward": 0.0,
        "student_rewards": [],
        "baseline_avg_reward": 0.0,
        "baseline_rewards": [],
        "result_dir": None,
        "winner": None,
    }

    try:
        # =========================
        # 最小化新增 4：统一主视频 result_base
        # =========================
        primary_result_base = normalize_result_base(args.result_base) if args.render_video else None
        baseline_result_base = build_baseline_result_base(primary_result_base) if primary_result_base else None

        env, result_dir = make_env(
            env_id=args.env,
            model_name=args.model_name,
            realtime_render=bool(args.realtime_render),
            render_video=bool(args.render_video),
            task_id=args.task_id,
            stage_spec_path=args.stage_spec_path,
            result_base=primary_result_base,
            workspace=workspace,
        )

        # HalfCheetah 等连续环境默认给一个步数上限
        max_steps = STEPS_PER_EPISODE if "cheetah" in args.env.lower() else None

        policy = load_policy(env, args.agent, os.path.join("models", args.model_name))
        rewards = run_episodes(env, policy, args.episodes, max_steps=max_steps)
        result["student_avg_reward"] = float(sum(rewards) / len(rewards)) if rewards else 0.0
        result["student_rewards"] = rewards
        env.close()

        if args.render_video and result_dir is not None:
            videoConcat(result_dir)
            # 注意：返回给后端的一定是“相对 workspace 的基路径”
            result["result_dir"] = result_dir

        baseline_used = False
        result_dir_baseline = None
        if args.baseline_model_path:
            baseline_path = os.path.join("saved_models", args.baseline_model_path)
            if os.path.isfile(baseline_path):
                baseline_model_name = Path(args.baseline_model_path).stem
                env_baseline, result_dir_baseline = make_env(
                    env_id=args.env,
                    model_name=baseline_model_name,
                    realtime_render=bool(args.realtime_render),
                    render_video=bool(args.render_video),
                    task_id=args.task_id,
                    stage_spec_path=args.stage_spec_path,
                    result_base=baseline_result_base,
                    workspace=workspace,
                )
                baseline_agent = args.baseline_agent if args.baseline_agent else args.agent
                baseline_policy = load_policy(env_baseline, baseline_agent, baseline_path)
                baseline_rewards = run_episodes(env_baseline, baseline_policy, args.episodes, max_steps=max_steps)
                result["baseline_avg_reward"] = float(sum(baseline_rewards) / len(baseline_rewards)) if baseline_rewards else 0.0
                result["baseline_rewards"] = baseline_rewards
                baseline_used = True
                env_baseline.close()

                if args.render_video and result_dir_baseline is not None:
                    videoConcat(result_dir_baseline)
        else:
            print("Warning: No baseline model path provided")

        if baseline_used:
            if result["student_avg_reward"] > result["baseline_avg_reward"]:
                result["winner"] = 1
            else:
                result["winner"] = 0
        else:
            print("Warning: No baseline model used")

        # =========================
        # 最小化修改 5：
        # 对比视频最终仍然覆盖写回 video_0.mp4
        # result["result_dir"] 始终保持 primary_result_base，不再返回 _vs_baseline
        # =========================
        if baseline_used and args.render_video and primary_result_base and result_dir_baseline is not None:
            student_video_path = os.path.join(workspace, primary_result_base + ".mp4")
            baseline_video_path = os.path.join(workspace, result_dir_baseline + ".mp4")
            compare_video_path = student_video_path

            if os.path.isfile(student_video_path) and os.path.isfile(baseline_video_path):
                student_tmp_path = os.path.join(
                    workspace,
                    os.path.join(os.path.dirname(primary_result_base), "student_video.mp4")
                )
                os.replace(student_video_path, student_tmp_path)
                video_side_by_side(student_tmp_path, baseline_video_path, compare_video_path)

            result["result_dir"] = primary_result_base

    except Exception as e:
        result["status"] = "FAILED"
        result["error"] = str(e)
        print(json.dumps(result, ensure_ascii=False))
        return 1

    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())