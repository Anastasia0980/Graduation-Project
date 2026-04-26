"""
LunarLander 课程任务 T1–T10：在 Gymnasium 1.1.x 上通过 kwargs + 子类 reset 实现。
"""

from __future__ import annotations

from typing import Optional

import gymnasium as gym
import numpy as np
from gymnasium.envs.registration import register, registry
from gymnasium.wrappers import TimeLimit

# 与 Gymnasium 官方 LunarLander 一致；课程环境直接构造时默认无 TimeLimit，需显式包裹
LUNAR_MAX_EPISODE_STEPS = 1000

from gymnasium.envs.box2d.lunar_lander import (
    INITIAL_RANDOM,
    LANDER_POLY,
    LEG_AWAY,
    LEG_DOWN,
    LEG_H,
    LEG_SPRING_TORQUE,
    LEG_W,
    LunarLander,
    SCALE,
    VIEWPORT_H,
    VIEWPORT_W,
    ContactDetector,
)
from Box2D.b2 import edgeShape, fixtureDef, polygonShape, revoluteJointDef


class LunarLanderTask(LunarLander):
    """在官方 LunarLander 上增加初始高度比例与初始随机冲量比例（其余与父类一致）。"""

    def __init__(
        self,
        render_mode: Optional[str] = None,
        continuous: bool = False,
        gravity: float = -10.0,
        enable_wind: bool = False,
        wind_power: float = 15.0,
        turbulence_power: float = 1.5,
        *,
        height_scale: float = 1.0,
        impulse_scale: float = 1.0,
        initial_angle_deg: float = 0.0,
    ):
        self._height_scale = float(height_scale)
        self._impulse_scale = float(impulse_scale)
        self._initial_angle_deg = float(initial_angle_deg)
        super().__init__(
            render_mode=render_mode,
            continuous=continuous,
            gravity=gravity,
            enable_wind=enable_wind,
            wind_power=wind_power,
            turbulence_power=turbulence_power,
        )

    def reset(
        self,
        *,
        seed: Optional[int] = None,
        options: Optional[dict] = None,
    ):
        import Box2D

        # 跳过 LunarLander.reset，仅初始化 Gym 侧 seed / np_random
        super(LunarLander, self).reset(seed=seed)
        self._destroy()

        self.world = Box2D.b2World(gravity=(0, self.gravity))
        self.world.contactListener_keepref = ContactDetector(self)
        self.world.contactListener = self.world.contactListener_keepref
        self.game_over = False
        self.prev_shaping = None

        W = VIEWPORT_W / SCALE
        H = VIEWPORT_H / SCALE

        CHUNKS = 11
        height = self.np_random.uniform(0, H / 2, size=(CHUNKS + 1,))
        chunk_x = [W / (CHUNKS - 1) * i for i in range(CHUNKS)]
        self.helipad_x1 = chunk_x[CHUNKS // 2 - 1]
        self.helipad_x2 = chunk_x[CHUNKS // 2 + 1]
        self.helipad_y = H / 4
        height[CHUNKS // 2 - 2] = self.helipad_y
        height[CHUNKS // 2 - 1] = self.helipad_y
        height[CHUNKS // 2 + 0] = self.helipad_y
        height[CHUNKS // 2 + 1] = self.helipad_y
        height[CHUNKS // 2 + 2] = self.helipad_y
        smooth_y = [
            0.33 * (height[i - 1] + height[i + 0] + height[i + 1])
            for i in range(CHUNKS)
        ]

        self.moon = self.world.CreateStaticBody(
            shapes=edgeShape(vertices=[(0, 0), (W, 0)])
        )
        self.sky_polys = []
        for i in range(CHUNKS - 1):
            p1 = (chunk_x[i], smooth_y[i])
            p2 = (chunk_x[i + 1], smooth_y[i + 1])
            self.moon.CreateEdgeFixture(vertices=[p1, p2], density=0, friction=0.1)
            self.sky_polys.append([p1, p2, (p2[0], H), (p1[0], H)])

        self.moon.color1 = (0.0, 0.0, 0.0)
        self.moon.color2 = (0.0, 0.0, 0.0)

        initial_y = (VIEWPORT_H / SCALE) * self._height_scale
        initial_x = VIEWPORT_W / SCALE / 2
        self.lander = self.world.CreateDynamicBody(
            position=(initial_x, initial_y),
            angle=np.deg2rad(self._initial_angle_deg),
            fixtures=fixtureDef(
                shape=polygonShape(
                    vertices=[(x / SCALE, y / SCALE) for x, y in LANDER_POLY]
                ),
                density=5.0,
                friction=0.1,
                categoryBits=0x0010,
                maskBits=0x001,
                restitution=0.0,
            ),
        )
        self.lander.color1 = (128, 102, 230)
        self.lander.color2 = (77, 77, 128)

        impulse = INITIAL_RANDOM * self._impulse_scale
        self.lander.ApplyForceToCenter(
            (
                self.np_random.uniform(-impulse, impulse),
                self.np_random.uniform(-impulse, impulse),
            ),
            True,
        )

        if self.enable_wind:
            self.wind_idx = self.np_random.integers(-9999, 9999)
            self.torque_idx = self.np_random.integers(-9999, 9999)

        self.legs = []
        for i in [-1, +1]:
            leg = self.world.CreateDynamicBody(
                position=(initial_x - i * LEG_AWAY / SCALE, initial_y),
                angle=(i * 0.05),
                fixtures=fixtureDef(
                    shape=polygonShape(box=(LEG_W / SCALE, LEG_H / SCALE)),
                    density=1.0,
                    restitution=0.0,
                    categoryBits=0x0020,
                    maskBits=0x001,
                ),
            )
            leg.ground_contact = False
            leg.color1 = (128, 102, 230)
            leg.color2 = (77, 77, 128)
            rjd = revoluteJointDef(
                bodyA=self.lander,
                bodyB=leg,
                localAnchorA=(0, 0),
                localAnchorB=(i * LEG_AWAY / SCALE, LEG_DOWN / SCALE),
                enableMotor=True,
                enableLimit=True,
                maxMotorTorque=LEG_SPRING_TORQUE,
                motorSpeed=+0.3 * i,
            )
            if i == -1:
                rjd.lowerAngle = +0.9 - 0.5
                rjd.upperAngle = +0.9
            else:
                rjd.lowerAngle = -0.9
                rjd.upperAngle = -0.9 + 0.5
            leg.joint = self.world.CreateJoint(rjd)
            self.legs.append(leg)

        self.drawlist = [self.lander] + self.legs

        if self.render_mode == "human":
            self.render()
        return self.step(np.array([0, 0]) if self.continuous else 0)[0], {}


# 与课程表对齐的默认超参（可按实验微调）
TASK_ENV_KWARGS = {
    # T1: wind=0, turbulence=0, height=low, velocity=0, angle=0
    "T1": dict(
        enable_wind=False,
        wind_power=0.0,
        turbulence_power=0.0,
        height_scale=1.0,
        impulse_scale=0.0,
        initial_angle_deg=0.0,
    ),
    # T2: wind=5, turbulence=0, height=low, velocity=0, angle=0
    "T2": dict(
        enable_wind=True,
        wind_power=20.0,
        turbulence_power=2.0,
        height_scale=2.0,
        impulse_scale=3.0,
        initial_angle_deg=30.0,
    ),
    # T3: wind=5, turbulence=0.5, height=low, velocity=0, angle=0
    "T3": dict(
        enable_wind=True,
        wind_power=15.0,
        turbulence_power=1.5,
        height_scale=1.0,
        impulse_scale=0.0,
        initial_angle_deg=0.0,
    ),
    # T4: wind=0, turbulence=0, height=mid, velocity=0, angle=0
    "T4": dict(
        enable_wind=False,
        wind_power=10.0,
        turbulence_power=1.0,
        height_scale=1.2,
        impulse_scale=0.0,
        initial_angle_deg=0.0,
    ),
    # T5: wind=5, turbulence=0.5, height=mid, velocity=0, angle=0
    "T5": dict(
        enable_wind=True,
        wind_power=5.0,
        turbulence_power=0.5,
        height_scale=1.0,
        impulse_scale=0.0,
        initial_angle_deg=0.0,
    ),
    # T6: wind=0, turbulence=0, height=mid, velocity=1, angle=10
    "T6": dict(
        enable_wind=True,
        wind_power=20.0,
        turbulence_power=2.0,
        height_scale=3.0,
        impulse_scale=3.0,
        initial_angle_deg=30.0,
    ),
    # T7: wind=5, turbulence=0.5, height=mid, velocity=1, angle=10
    "T7": dict(
        enable_wind=True,
        wind_power=5.0,
        turbulence_power=0.5,
        height_scale=1.2,
        impulse_scale=1.0,
        initial_angle_deg=10.0,
    ),
    # T8: wind=10, turbulence=1, height=mid, velocity=1, angle=10
    "T8": dict(
        enable_wind=True,
        wind_power=10.0,
        turbulence_power=1.0,
        height_scale=1.2,
        impulse_scale=1.0,
        initial_angle_deg=10.0,
    ),
    # T9: wind=5, turbulence=0.5, height=mid, velocity=2, angle=20
    "T9": dict(
        enable_wind=True,
        wind_power=5.0,
        turbulence_power=0.5,
        height_scale=1.2,
        impulse_scale=2.0,
        initial_angle_deg=20.0,
    ),
    # T10: wind=15, turbulence=1.5, height=high, velocity=2, angle=20
    "T10": dict(
        enable_wind=True,
        wind_power=15.0,
        turbulence_power=1.5,
        height_scale=1.5,
        impulse_scale=2.0,
        initial_angle_deg=20.0,
    ),
}

_LUNAR_TASKS_REGISTERED = False


def register_lunar_tasks() -> None:
    """向 gymnasium 注册 LunarLander-T1-v0 … T10-v0（幂等）。"""
    global _LUNAR_TASKS_REGISTERED
    if _LUNAR_TASKS_REGISTERED:
        return

    for tid, kw in TASK_ENV_KWARGS.items():
        env_id = f"LunarLander-{tid}-v0"
        if env_id in registry:
            continue
        register(
            id=env_id,
            entry_point="lunar_task_env:LunarLanderTask",
            kwargs=kw,
        )

    _LUNAR_TASKS_REGISTERED = True


def make_lunar_env_from_spec(
    env_spec: dict,
    *,
    render_mode: Optional[str] = None,
) -> gym.Env:
    """
    由 JSON 描述构造 LunarLanderTask（A2），键名与 TASK_ENV_KWARGS 中条目一致。
    """
    defaults = dict(
        enable_wind=False,
        wind_power=0.0,
        turbulence_power=0.0,
        height_scale=1.0,
        impulse_scale=0.0,
        initial_angle_deg=0.0,
    )
    kw = dict(defaults)
    for key, val in env_spec.items():
        if key in defaults:
            kw[key] = val
    env = LunarLanderTask(render_mode=render_mode, **kw)
    return TimeLimit(env, max_episode_steps=LUNAR_MAX_EPISODE_STEPS)


def make_lunar_env(
    task: str,
    *,
    render_mode: Optional[str] = None,
) -> gym.Env:
    """
    task: T1–T10 或 vanilla（官方 LunarLander-v3，无课程改动）
    """
    register_lunar_tasks()
    t = task.strip().upper()
    if t == "VANILLA":
        return gym.make("LunarLander-v3", render_mode=render_mode)
    if t not in TASK_ENV_KWARGS:
        raise ValueError(f"未知 task={task!r}，可选: T1…T10, VANILLA")
    kw = dict(TASK_ENV_KWARGS[t])
    env = LunarLanderTask(render_mode=render_mode, **kw)
    return TimeLimit(env, max_episode_steps=LUNAR_MAX_EPISODE_STEPS)


def task_result_subdir(task: str) -> str:
    """用于 saved_models / results 子目录名。"""
    return task.strip().upper()
