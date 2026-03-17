"""
DQN算法实现井字棋智能体
这是学生可以参考的示例代码
"""

import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
import random
from collections import deque
import gymnasium as gym
from pettingzoo.classic import tic_tac_toe_v3


# ==================== DQN网络定义 ====================

class DQNNetwork(nn.Module):
    """DQN的神经网络"""

    def __init__(self, input_dim=9, hidden_dim=128, output_dim=9):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, output_dim)
        )

    def forward(self, x):
        return self.net(x)


# ==================== 经验回放缓冲区 ====================

class ReplayBuffer:
    """经验回放缓冲区"""

    def __init__(self, capacity=10000):
        self.buffer = deque(maxlen=capacity)

    def push(self, state, action, reward, next_state, done):
        self.buffer.append((state, action, reward, next_state, done))

    def sample(self, batch_size):
        return random.sample(self.buffer, batch_size)

    def __len__(self):
        return len(self.buffer)


# ==================== DQN智能体 ====================

class DQNAgent:
    """DQN智能体（用于训练）"""

    def __init__(self,
                 player="player_0",
                 learning_rate=0.001,
                 gamma=0.99,
                 epsilon=1.0,
                 epsilon_min=0.01,
                 epsilon_decay=0.995,
                 batch_size=32,
                 memory_size=10000,
                 target_update=100):

        self.player = player
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        # DQN参数
        self.learning_rate = learning_rate
        self.gamma = gamma
        self.epsilon = epsilon
        self.epsilon_min = epsilon_min
        self.epsilon_decay = epsilon_decay
        self.batch_size = batch_size
        self.target_update = target_update
        self.update_counter = 0

        # 网络
        self.q_network = DQNNetwork().to(self.device)
        self.target_network = DQNNetwork().to(self.device)
        self.target_network.load_state_dict(self.q_network.state_dict())

        # 优化器
        self.optimizer = optim.Adam(self.q_network.parameters(), lr=learning_rate)

        # 经验回放
        self.memory = ReplayBuffer(memory_size)

    def act(self, observation, eval_mode=False):
        """
        选择动作

        Args:
            observation: 环境观察
            eval_mode: True表示评估模式（不探索）
        """
        board = observation['observation'].flatten()
        action_mask = observation['action_mask']

        # 探索
        if not eval_mode and random.random() < self.epsilon:
            available = [i for i, valid in enumerate(action_mask) if valid]
            return random.choice(available)

        # 利用
        board_tensor = torch.FloatTensor(board).unsqueeze(0).to(self.device)
        with torch.no_grad():
            q_values = self.q_network(board_tensor).cpu().numpy().flatten()

        # 屏蔽非法动作
        q_values = q_values * action_mask + (1 - action_mask) * -1e9
        return int(np.argmax(q_values))

    def learn(self):
        """从经验回放中学习"""
        if len(self.memory) < self.batch_size:
            return

        # 采样
        batch = self.memory.sample(self.batch_size)
        states, actions, rewards, next_states, dones = zip(*batch)

        # 转换为张量
        states = torch.FloatTensor(np.array(states)).to(self.device)
        actions = torch.LongTensor(actions).unsqueeze(1).to(self.device)
        rewards = torch.FloatTensor(rewards).unsqueeze(1).to(self.device)
        next_states = torch.FloatTensor(np.array(next_states)).to(self.device)
        dones = torch.FloatTensor(dones).unsqueeze(1).to(self.device)

        # 计算当前Q值
        current_q = self.q_network(states).gather(1, actions)

        # 计算目标Q值
        with torch.no_grad():
            next_q = self.target_network(next_states).max(1, keepdim=True)[0]
            target_q = rewards + self.gamma * next_q * (1 - dones)

        # 计算损失
        loss = nn.MSELoss()(current_q, target_q)

        # 优化
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        # 更新目标网络
        self.update_counter += 1
        if self.update_counter % self.target_update == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())

        # 衰减epsilon
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

    def save(self, path):
        """保存模型"""
        torch.save({
            'q_network': self.q_network.state_dict(),
            'target_network': self.target_network.state_dict(),
            'optimizer': self.optimizer.state_dict(),
            'epsilon': self.epsilon
        }, path)
        print(f"模型已保存到 {path}")

    def load(self, path):
        """加载模型"""
        checkpoint = torch.load(path)
        self.q_network.load_state_dict(checkpoint['q_network'])
        self.target_network.load_state_dict(checkpoint['target_network'])
        self.optimizer.load_state_dict(checkpoint['optimizer'])
        self.epsilon = checkpoint['epsilon']
        print(f"模型已从 {path} 加载")


# ==================== 自我对弈训练 ====================

def self_play_train(episodes=1000):
    """
    使用自我对弈训练DQN
    """
    # 创建两个智能体（一个训练，一个作为对手）
    agent_x = DQNAgent(player="player_0")  # 先手 X，被训练
    agent_o = DQNAgent(player="player_1")  # 后手 O，作为对手

    # 训练统计
    wins_x = 0
    wins_o = 0
    draws = 0

    for episode in range(episodes):
        env = tic_tac_toe_v3.env(render_mode=None)
        env.reset()

        current_agent = agent_x
        current_player = "player_0"
        episode_reward_x = 0
        episode_reward_o = 0
        moves = 0

        # 存储经验
        states = []
        actions = []
        rewards = []

        while True:
            observation, reward, termination, truncation, _ = env.last()

            if termination or truncation:
                # 游戏结束，计算最终奖励
                if reward > 0:
                    if current_player == "player_0":
                        wins_x += 1
                    else:
                        wins_o += 1
                elif reward == 0 and moves == 9:
                    draws += 1

                # 分配延迟奖励
                for i in range(len(states)):
                    if states[i] is not None:
                        agent_x.memory.push(
                            states[i], actions[i],
                            rewards[i] if rewards[i] is not None else reward,
                            None, True
                        )
                break

            # 选择动作
            action = current_agent.act(observation, eval_mode=False)

            # 保存经验（先手X）
            if current_player == "player_0":
                board = observation['observation'].flatten().copy()
                states.append(board)
                actions.append(action)
                rewards.append(None)  # 延迟奖励

            # 执行动作
            env.step(action)
            moves += 1

            # 切换玩家
            current_player = "player_1" if current_player == "player_0" else "player_0"
            current_agent = agent_o if current_player == "player_1" else agent_x

        # 学习
        agent_x.learn()

        env.close()

        # 每100轮打印一次结果
        if (episode + 1) % 100 == 0:
            total = wins_x + wins_o + draws
            print(f"\nEpisode {episode + 1}")
            print(f"X胜: {wins_x} ({wins_x / total * 100:.1f}%)")
            print(f"O胜: {wins_o} ({wins_o / total * 100:.1f}%)")
            print(f"平局: {draws} ({draws / total * 100:.1f}%)")
            print(f"epsilon: {agent_x.epsilon:.3f}")

            # 重置统计
            wins_x = wins_o = draws = 0

    # 保存训练好的模型
    agent_x.save("dqn_tic_tac_toe_x.pt")
    return agent_x


# ==================== 测试训练好的模型 ====================

def test_agent(agent_path="dqn_tic_tac_toe_x.pt", num_games=10):
    """
    测试训练好的DQN智能体
    """
    # 加载模型
    agent = DQNAgent(player="player_0")
    agent.load(agent_path)
    agent.epsilon = 0  # 测试时不探索

    env = tic_tac_toe_v3.env(render_mode="human")  # 显示对局

    wins = 0
    draws = 0
    losses = 0

    for game in range(num_games):
        env.reset()
        current_agent = agent
        current_player = "player_0"
        moves = 0

        print(f"\n=== 游戏 {game + 1} ===")

        while True:
            observation, reward, termination, truncation, _ = env.last()

            if termination or truncation:
                if reward > 0:
                    print(f"游戏结束，智能体获胜！")
                    wins += 1
                elif reward < 0:
                    print(f"游戏结束，智能体失败")
                    losses += 1
                elif reward == 0 and moves == 9:
                    print(f"游戏结束，平局")
                    draws += 1
                break

            action = current_agent.act(observation, eval_mode=True)
            env.step(action)
            moves += 1

            current_player = "player_1" if current_player == "player_0" else "player_0"
            current_agent = agent if current_player == "player_0" else None

    print(f"\n=== 测试结果 ===")
    print(f"胜: {wins}, 负: {losses}, 平: {draws}")
    print(f"胜率: {wins / num_games * 100:.1f}%")


# ==================== 主程序 ====================

if __name__ == "__main__":
    # 训练
    print("开始训练DQN智能体...")
    trained_agent = self_play_train(episodes=500)

    # 测试
    print("\n开始测试训练好的智能体...")
    test_agent("dqn_tic_tac_toe_x.pt", num_games=5)