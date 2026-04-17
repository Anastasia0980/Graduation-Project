import math
import random
from collections import deque

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F


class ReplayBuffer:
    """ 经验回放池 """

    def __init__(self, capacity):
        """
        初始化经验回放池
        :param capacity: 回放池的最大容量
        """
        # deque 是一个双端队列，当容量满时，再添加新元素会自动从另一端移除旧元素
        self.buffer = deque(maxlen=int(capacity))

    def add(self, state, action, reward, next_state, done):
        """
        向回放池中添加一条经验
        """
        # 将经验以元组形式存储
        experience = (state, action, reward, next_state, done)
        self.buffer.append(experience)

    def sample(self, batch_size):
        """
        从回放池中随机采样一个批次的经验
        :param batch_size: 批次大小
        :return: 一个包含 state, action, reward, next_state, done 张量的元组
        """
        # 从 buffer 中随机采样 batch_size 条经验
        batch = random.sample(self.buffer, batch_size)

        # 将经验解包并转换为 NumPy 数组
        # zip(*batch) 会将 [(s1,a1,r1,...), (s2,a2,r2,...)] 转换为 ([s1,s2,...], [a1,a2,...], ...)
        states, actions, rewards, next_states, dones = map(np.array, zip(*batch))

        # 将 NumPy 数组转换为 PyTorch 张量，并移动到合适的设备（例如 GPU）
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        states = torch.FloatTensor(states).to(device)
        actions = torch.LongTensor(actions).unsqueeze(1).to(device)  # unsqueeze(1) 增加一个维度以匹配网络输出
        rewards = torch.FloatTensor(rewards).unsqueeze(1).to(device)
        next_states = torch.FloatTensor(next_states).to(device)
        dones = torch.FloatTensor(dones).unsqueeze(1).to(device)

        return states, actions, rewards, next_states, dones

    def __len__(self):
        """
        返回当前回放池中的经验数量
        """
        return len(self.buffer)


class PrioritizedReplayBuffer:
    """基于优先级采样的经验回放池（简化实现，O(N) 采样）。"""

    def __init__(self, capacity, alpha=0.6, eps=1e-6, max_priority_clip=None):
        self.capacity = int(capacity)
        self.alpha = float(alpha)
        self.eps = float(eps)
        self.max_priority_clip = (
            float(max_priority_clip) if max_priority_clip is not None else None
        )
        # 可由外部注入：基于当前网络为新样本计算 priority（标量）。
        self.priority_fn = None
        self.buffer = []
        self.priorities = np.zeros((self.capacity,), dtype=np.float32)
        self.pos = 0

    def add(self, state, action, reward, next_state, done):
        experience = (state, action, reward, next_state, done)
        priority = float(self.priority_fn(state, action, reward, next_state, done))
        if len(self.buffer) < self.capacity:
            self.buffer.append(experience)
        else:
            self.buffer[self.pos] = experience
        priority = max(priority, self.eps)
        if self.max_priority_clip is not None:
            priority = min(priority, self.max_priority_clip)
        self.priorities[self.pos] = priority
        self.pos = (self.pos + 1) % self.capacity

    def sample(self, batch_size, beta=0.4):
        if len(self.buffer) == self.capacity:
            prios = self.priorities
        else:
            prios = self.priorities[: self.pos]
        probs = prios ** self.alpha
        probs /= probs.sum()

        indices = np.random.choice(len(self.buffer), batch_size, p=probs)
        batch = [self.buffer[idx] for idx in indices]
        states, actions, rewards, next_states, dones = map(np.array, zip(*batch))

        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        states = torch.FloatTensor(states).to(device)
        actions = torch.LongTensor(actions).unsqueeze(1).to(device)
        rewards = torch.FloatTensor(rewards).unsqueeze(1).to(device)
        next_states = torch.FloatTensor(next_states).to(device)
        dones = torch.FloatTensor(dones).unsqueeze(1).to(device)

        total = len(self.buffer)
        weights = (total * probs[indices]) ** (-beta)
        weights /= weights.max()
        weights = torch.FloatTensor(weights).unsqueeze(1).to(device)

        return states, actions, rewards, next_states, dones, indices, weights

    def update_priorities(self, indices, td_errors):
        td_errors = np.abs(td_errors).reshape(-1)
        for idx, err in zip(indices, td_errors):
            priority = float(err + self.eps)
            if self.max_priority_clip is not None:
                priority = min(priority, self.max_priority_clip)
            self.priorities[idx] = priority

    def __len__(self):
        return len(self.buffer)


class QNetwork(nn.Module):
    """ Q值网络 """
    def __init__(self, state_dim, action_dim, hidden_dim=128):
        """
        初始化Q网络
        :param state_dim: 状态空间的维度
        :param action_dim: 动作空间的维度
        :param hidden_dim: 隐藏层的维度
        """
        super(QNetwork, self).__init__()
        self.network = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, action_dim)
        )

    def forward(self, state):
        """
        前向传播
        :param state: 状态张量
        :return: 每个动作的Q值
        """
        return self.network(state)


class C51Network(nn.Module):
    """C51 分布式 Q 网络：输出 [B, A, N_atoms] logits。"""

    def __init__(self, state_dim, action_dim, num_atoms=51, hidden_dim=128):
        super(C51Network, self).__init__()
        self.action_dim = int(action_dim)
        self.num_atoms = int(num_atoms)
        self.feature = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
        )
        self.head = nn.Linear(hidden_dim, self.action_dim * self.num_atoms)

    def forward(self, state):
        x = self.feature(state)
        logits = self.head(x).view(-1, self.action_dim, self.num_atoms)
        return logits


class NoisyLinear(nn.Module):
    """Factorized Gaussian 噪声线性层（训练时随机探索，eval 时仅用均值）。"""

    def __init__(self, in_features, out_features, std_init=0.4):
        super(NoisyLinear, self).__init__()
        self.in_features = int(in_features)
        self.out_features = int(out_features)
        self.std_init = float(std_init)
        self.weight_mu = nn.Parameter(torch.empty(out_features, in_features))
        self.weight_sigma = nn.Parameter(torch.empty(out_features, in_features))
        self.bias_mu = nn.Parameter(torch.empty(out_features))
        self.bias_sigma = nn.Parameter(torch.empty(out_features))
        self.register_buffer("weight_epsilon", torch.empty(out_features, in_features))
        self.register_buffer("bias_epsilon", torch.empty(out_features))
        self.reset_parameters()
        self.reset_noise()

    def reset_parameters(self):
        bound = 1.0 / math.sqrt(self.in_features)
        self.weight_mu.data.uniform_(-bound, bound)
        self.weight_sigma.data.fill_(
            self.std_init / math.sqrt(self.in_features)
        )
        self.bias_mu.data.uniform_(-bound, bound)
        self.bias_sigma.data.fill_(
            self.std_init / math.sqrt(self.out_features)
        )

    @staticmethod
    def _scale_noise(size, device):
        x = torch.randn(size, device=device)
        return x.sign() * x.abs().sqrt()

    def reset_noise(self):
        device = self.weight_mu.device
        eps_in = self._scale_noise(self.in_features, device)
        eps_out = self._scale_noise(self.out_features, device)
        self.weight_epsilon.copy_(torch.outer(eps_out, eps_in))
        self.bias_epsilon.copy_(eps_out)

    def forward(self, x):
        if self.training:
            w = self.weight_mu + self.weight_sigma * self.weight_epsilon
            b = self.bias_mu + self.bias_sigma * self.bias_epsilon
        else:
            w = self.weight_mu
            b = self.bias_mu
        return F.linear(x, w, b)


class DuelingC51Network(nn.Module):
    """Dueling + C51：输出 [B, A, N_atoms] 的 logits（最后一维 softmax 为原子分布）。"""

    def __init__(
        self,
        state_dim,
        action_dim,
        num_atoms=51,
        hidden_dim=128,
        use_noisy=True,
    ):
        super(DuelingC51Network, self).__init__()
        self.action_dim = int(action_dim)
        self.num_atoms = int(num_atoms)
        self.use_noisy = bool(use_noisy)
        self.trunk = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
        )
        if self.use_noisy:
            self.value = NoisyLinear(hidden_dim, self.num_atoms)
            self.advantage = NoisyLinear(hidden_dim, self.action_dim * self.num_atoms)
        else:
            self.value = nn.Linear(hidden_dim, self.num_atoms)
            self.advantage = nn.Linear(hidden_dim, self.action_dim * self.num_atoms)

    def forward(self, x):
        f = self.trunk(x)
        v = self.value(f).view(-1, self.num_atoms)
        a = self.advantage(f).view(-1, self.action_dim, self.num_atoms)
        q_atoms = v.unsqueeze(1) + a - a.mean(dim=1, keepdim=True)
        return q_atoms

    def reset_noise(self):
        if not self.use_noisy:
            return
        for m in self.modules():
            if isinstance(m, NoisyLinear):
                m.reset_noise()


class PrioritizedRainbowBuffer:
    """PER，存储 (s,a,R,s',done,bootstrap_gamma)，bootstrap_gamma 为 n-step 后对 Z 的折扣 gamma^k。"""

    def __init__(self, capacity, alpha=0.6, eps=1e-6, max_priority_clip=None):
        self.capacity = int(capacity)
        self.alpha = float(alpha)
        self.eps = float(eps)
        self.max_priority_clip = (
            float(max_priority_clip) if max_priority_clip is not None else None
        )
        self.buffer = []
        self.priorities = np.zeros((self.capacity,), dtype=np.float32)
        self.pos = 0

    def add(self, state, action, reward, next_state, done, bootstrap_gamma):
        experience = (state, action, reward, next_state, done, float(bootstrap_gamma))
        max_prio = float(self.priorities.max()) if len(self.buffer) > 0 else 1.0
        if len(self.buffer) < self.capacity:
            self.buffer.append(experience)
        else:
            self.buffer[self.pos] = experience
        p = max(max_prio, self.eps)
        if self.max_priority_clip is not None:
            p = min(p, self.max_priority_clip)
        self.priorities[self.pos] = p
        self.pos = (self.pos + 1) % self.capacity

    def sample(self, batch_size, beta=0.4):
        if len(self.buffer) == self.capacity:
            prios = self.priorities
        else:
            prios = self.priorities[: self.pos]
        probs = prios ** self.alpha
        probs /= probs.sum()

        indices = np.random.choice(len(self.buffer), batch_size, p=probs)
        batch = [self.buffer[idx] for idx in indices]
        states, actions, rewards, next_states, dones, boot_g = map(np.array, zip(*batch))

        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        states = torch.FloatTensor(states).to(device)
        actions = torch.LongTensor(actions).unsqueeze(1).to(device)
        rewards = torch.FloatTensor(rewards).unsqueeze(1).to(device)
        next_states = torch.FloatTensor(next_states).to(device)
        dones = torch.FloatTensor(dones).unsqueeze(1).to(device)
        bootstrap_gamma = torch.FloatTensor(boot_g).unsqueeze(1).to(device)

        total = len(self.buffer)
        weights = (total * probs[indices]) ** (-beta)
        weights /= weights.max()
        weights = torch.FloatTensor(weights).unsqueeze(1).to(device)

        return (
            states,
            actions,
            rewards,
            next_states,
            dones,
            bootstrap_gamma,
            indices,
            weights,
        )

    def update_priorities(self, indices, priorities):
        priorities = np.abs(priorities).reshape(-1)
        for idx, pr in zip(indices, priorities):
            p = float(pr + self.eps)
            if self.max_priority_clip is not None:
                p = min(p, self.max_priority_clip)
            self.priorities[idx] = p

    def __len__(self):
        return len(self.buffer)


class DQNAgent:
    """ DQN Agent """

    def __init__(self, state_dim, action_dim, lr=1e-3, gamma=0.99, epsilon=0.9,
                 target_update_freq=100, buffer_capacity=5000, hidden_dim=128):
        """
        初始化 DQN Agent
        :param state_dim: 状态维度
        :param action_dim: 动作维度
        :param lr: 学习率 (learning rate)
        :param gamma: 折扣因子 (discount factor)
        :param epsilon: 探索率 (exploration rate)
        :param target_update_freq: 目标网络更新频率
        :param buffer_capacity: 经验回放池容量
        :param hidden_dim: Q 网络隐藏层宽度
        """
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        self.action_dim = action_dim
        self.gamma = gamma
        self.epsilon = epsilon
        self.target_update_freq = target_update_freq

        # 创建主网络和目标网络
        self.q_network = QNetwork(state_dim, action_dim, hidden_dim=hidden_dim).to(self.device)
        self.target_network = QNetwork(state_dim, action_dim, hidden_dim=hidden_dim).to(self.device)

        # 将主网络的权重复制到目标网络
        self.target_network.load_state_dict(self.q_network.state_dict())
        self.target_network.eval()  # 目标网络不进行梯度下降，只用于计算目标Q值

        # 创建优化器
        self.optimizer = torch.optim.Adam(self.q_network.parameters(), lr=lr)

        # 创建经验回放池
        self.buffer = ReplayBuffer(buffer_capacity)

        # 记录更新次数，用于控制目标网络的更新
        self.update_count = 0

    def choose_action(self, state):
        """
        使用 epsilon-greedy 策略选择动作
        :param state: 当前状态 (numpy array)
        :return: 选择的动作 (int)
        """
        # epsilon 概率进行随机探索
        if random.random() < self.epsilon:
            return random.randrange(self.action_dim)

        # 1-epsilon 概率进行利用
        else:
            state = torch.FloatTensor(state).unsqueeze(0).to(self.device)  # 增加 batch 维度
            # 不计算梯度，以加速计算
            with torch.no_grad():
                q_values = self.q_network(state)
            # 选择 Q 值最大的动作
            return q_values.argmax().item()

    def update(self, batch_size):
        """
        采样经验并更新网络参数
        :param batch_size: 批次大小
        """
        # 如果经验池中的样本数量不足一个批次，则不进行更新
        if len(self.buffer) < batch_size:
            return

        # 从经验池采样
        states, actions, rewards, next_states, dones = self.buffer.sample(batch_size)

        # --- 计算当前Q值 ---
        # 根据 q_network 计算出在 states 状态下，采取 actions 动作的 Q 值
        # q_network(states) -> [batch_size, action_dim]
        # actions -> [batch_size, 1]
        # .gather(1, actions) -> 得到每个样本对应动作的Q值
        current_q_values = self.q_network(states).gather(1, actions)

        # --- 计算目标Q值 ---
        # 使用 target_network 计算下一个状态的最大Q值
        # .max(1) -> 返回 (values, indices) 元组，[0] 取出 values
        # .unsqueeze(1) -> 增加一个维度以匹配 current_q_values 的形状
        # .detach() -> 不计算梯度
        next_q_values = self.target_network(next_states).max(1)[0].unsqueeze(1)

        # 计算目标Q值 (Bellman方程)
        # 如果一个状态是终止状态 (done=1)，那么它的未来奖励为0
        target_q_values = rewards + (1 - dones) * self.gamma * next_q_values

        # --- 计算损失并更新 ---
        loss = nn.MSELoss()(current_q_values, target_q_values)

        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        # --- 更新目标网络 ---
        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())




class DoubleDQNAgent(DQNAgent):
    """ Double DQN Agent """

    def __init__(self, state_dim, action_dim, lr=1e-3, gamma=0.99, epsilon=0.9,
                 target_update_freq=100, buffer_capacity=5000, hidden_dim=128):
        # 完全继承父类的初始化方法
        super().__init__(state_dim, action_dim, lr, gamma, epsilon,
                         target_update_freq, buffer_capacity, hidden_dim=hidden_dim)
        print("Initialized Double DQN Agent")

    def update(self, batch_size):
        """
        重写 update 方法以实现 Double DQN 的逻辑
        """
        if len(self.buffer) < batch_size:
            return

        states, actions, rewards, next_states, dones = self.buffer.sample(batch_size)

        # --- Double DQN 的核心改动在这里 ---

        # 1. 使用主网络(q_network)选择下一个状态的最佳动作
        # torch.argmax a_max' Q_main(s', a')
        next_actions = self.q_network(next_states).argmax(1).unsqueeze(1)

        # 2. 使用目标网络(target_network)评估这些动作的Q值
        # .gather() 根据 next_actions 中提供的索引，在 dim=1 上选取Q值
        next_q_values = self.target_network(next_states).gather(1, next_actions)

        # --- 后续计算与标准DQN相同 ---

        target_q_values = rewards + (1 - dones) * self.gamma * next_q_values

        current_q_values = self.q_network(states).gather(1, actions)

        loss = nn.MSELoss()(current_q_values, target_q_values)

        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())


class PrioritizedDoubleDQNAgent(DQNAgent):
    """Prioritized Experience Replay + Double DQN."""

    def __init__(
        self,
        state_dim,
        action_dim,
        lr=1e-3,
        gamma=0.99,
        epsilon=0.9,
        target_update_freq=100,
        buffer_capacity=5000,
        per_alpha=0.6,
        per_beta_start=0.4,
        per_beta_increment=0.0,
        per_eps=1e-5,
        per_max_priority_clip=10.0,
        td_error_clip=None,
        grad_clip_norm=10.0,
        hidden_dim=256,
    ):
        super().__init__(
            state_dim,
            action_dim,
            lr,
            gamma,
            epsilon,
            target_update_freq,
            buffer_capacity,
            hidden_dim=hidden_dim,
        )
        self.buffer = PrioritizedReplayBuffer(
            buffer_capacity,
            alpha=per_alpha,
            eps=per_eps,
            max_priority_clip=per_max_priority_clip,
        )
        self.per_beta = float(per_beta_start)
        self.per_beta_increment = float(per_beta_increment)
        self.td_error_clip = float(td_error_clip) if td_error_clip is not None else None
        self.grad_clip_norm = float(grad_clip_norm) if grad_clip_norm is not None else None
        # 新样本 priority 直接由当前 TD-error 决定。
        self.buffer.priority_fn = self._compute_transition_priority

    def _compute_transition_priority(self, state, action, reward, next_state, done):
        with torch.no_grad():
            state_t = torch.FloatTensor(state).unsqueeze(0).to(self.device)
            next_state_t = torch.FloatTensor(next_state).unsqueeze(0).to(self.device)
            action_t = torch.LongTensor([[action]]).to(self.device)
            reward_t = torch.FloatTensor([[reward]]).to(self.device)
            done_t = torch.FloatTensor([[float(done)]]).to(self.device)

            next_action = self.q_network(next_state_t).argmax(1, keepdim=True)
            next_q = self.target_network(next_state_t).gather(1, next_action)
            target_q = reward_t + (1.0 - done_t) * self.gamma * next_q
            current_q = self.q_network(state_t).gather(1, action_t)
            td_error = target_q - current_q

            if self.td_error_clip is not None:
                td_error = td_error.clamp(-self.td_error_clip, self.td_error_clip)

            priority = td_error.abs().item() + self.buffer.eps
            if self.buffer.max_priority_clip is not None:
                priority = min(priority, self.buffer.max_priority_clip)
            return priority

    def update(self, batch_size):
        if len(self.buffer) < batch_size:
            return

        (
            states,
            actions,
            rewards,
            next_states,
            dones,
            indices,
            weights,
        ) = self.buffer.sample(batch_size, beta=self.per_beta)

        # Double DQN: online 网络选动作，target 网络评估动作。
        next_actions = self.q_network(next_states).argmax(1).unsqueeze(1)
        next_q_values = self.target_network(next_states).gather(1, next_actions).detach()
        target_q_values = rewards + (1 - dones) * self.gamma * next_q_values

        current_q_values = self.q_network(states).gather(1, actions)
        td_errors = target_q_values - current_q_values
        if self.td_error_clip is not None:
            td_errors = td_errors.clamp(-self.td_error_clip, self.td_error_clip)

        loss = (weights * td_errors.pow(2)).mean()
        self.optimizer.zero_grad()
        loss.backward()
        if self.grad_clip_norm is not None:
            torch.nn.utils.clip_grad_norm_(self.q_network.parameters(), self.grad_clip_norm)
        self.optimizer.step()

        self.buffer.update_priorities(
            indices, td_errors.detach().cpu().numpy().squeeze(-1)
        )

        self.per_beta = min(1.0, self.per_beta + self.per_beta_increment)

        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())


class DistribDQNAgent(DQNAgent):
    """C51 Distributional DQN（动作选择基于分布期望值）。"""

    def __init__(
        self,
        state_dim,
        action_dim,
        lr=1e-3,
        gamma=0.99,
        epsilon=0.9,
        target_update_freq=100,
        buffer_capacity=5000,
        num_atoms=51,
        v_min=-300.0,
        v_max=300.0,
        hidden_dim=256,
    ):
        super().__init__(
            state_dim,
            action_dim,
            lr,
            gamma,
            epsilon,
            target_update_freq,
            buffer_capacity,
            hidden_dim=hidden_dim,
        )
        self.num_atoms = int(num_atoms)
        self.v_min = float(v_min)
        self.v_max = float(v_max)
        self.delta_z = (self.v_max - self.v_min) / float(self.num_atoms - 1)
        self.support = torch.linspace(
            self.v_min, self.v_max, self.num_atoms, device=self.device
        )

        self.q_network = C51Network(
            state_dim, action_dim, self.num_atoms, hidden_dim=hidden_dim
        ).to(self.device)
        self.target_network = C51Network(
            state_dim, action_dim, self.num_atoms, hidden_dim=hidden_dim
        ).to(self.device)
        self.target_network.load_state_dict(self.q_network.state_dict())
        self.target_network.eval()
        self.optimizer = torch.optim.Adam(self.q_network.parameters(), lr=lr)

    def _dist(self, net, states):
        logits = net(states)
        probs = torch.softmax(logits, dim=-1)
        return probs.clamp(min=1e-6)

    def _q_values(self, probs):
        return torch.sum(probs * self.support.view(1, 1, -1), dim=-1)

    def choose_action(self, state):
        if random.random() < self.epsilon:
            return random.randrange(self.action_dim)
        state_t = torch.FloatTensor(state).unsqueeze(0).to(self.device)
        with torch.no_grad():
            probs = self._dist(self.q_network, state_t)
            q_vals = self._q_values(probs)
        return q_vals.argmax(dim=1).item()

    def update(self, batch_size):
        if len(self.buffer) < batch_size:
            return

        states, actions, rewards, next_states, dones = self.buffer.sample(batch_size)

        with torch.no_grad():
            next_probs_online = self._dist(self.q_network, next_states)
            next_q_online = self._q_values(next_probs_online)
            next_actions = next_q_online.argmax(dim=1)  # [B]

            next_probs_target = self._dist(self.target_network, next_states)
            next_dist = next_probs_target[
                torch.arange(batch_size, device=self.device), next_actions
            ]  # [B, N]

            tz = rewards + (1.0 - dones) * self.gamma * self.support.view(1, -1)
            tz = tz.clamp(self.v_min, self.v_max)
            b = (tz - self.v_min) / self.delta_z
            l = b.floor().long()
            u = b.ceil().long()

            proj_dist = torch.zeros(batch_size, self.num_atoms, device=self.device)
            offset = (
                torch.arange(batch_size, device=self.device)
                .unsqueeze(1)
                .expand(batch_size, self.num_atoms)
            )

            proj_dist.view(-1).index_add_(
                0,
                (l + offset * self.num_atoms).view(-1),
                (next_dist * (u.float() - b)).view(-1),
            )
            proj_dist.view(-1).index_add_(
                0,
                (u + offset * self.num_atoms).view(-1),
                (next_dist * (b - l.float())).view(-1),
            )

            eq_mask = (u == l)
            if eq_mask.any():
                proj_dist[eq_mask] += next_dist[eq_mask]

        logits = self.q_network(states)
        log_probs = torch.log_softmax(logits, dim=-1)
        chosen_log_probs = log_probs[
            torch.arange(batch_size, device=self.device), actions.squeeze(1)
        ]  # [B, N]

        loss = -(proj_dist * chosen_log_probs).sum(dim=1).mean()
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())


class _RainbowBufferProxy:
    """将环境交互的 (s,a,r,s2,d) 交给 RainbowAgent 做 n-step 后再写入 PER。"""

    def __init__(self, agent):
        self._agent = agent

    def add(self, state, action, reward, next_state, done):
        self._agent._on_env_transition(state, action, reward, next_state, done)

    def __len__(self):
        return len(self._agent._per_buffer)


class RainbowAgent:
    """
    Rainbow 核心组合：Double C51 + Dueling + NoisyNet + PER + n-step。
    探索仅靠 NoisyLinear（不使用 epsilon-greedy）。
    """

    def __init__(
        self,
        state_dim,
        action_dim,
        lr=1e-4,
        gamma=0.99,
        target_update_freq=100,
        buffer_capacity=200_000,
        num_atoms=51,
        v_min=-300.0,
        v_max=300.0,
        hidden_dim=256,
        n_step=3,
        per_alpha=0.6,
        per_beta_start=0.4,
        per_beta_increment=0.0,
        per_eps=1e-6,
        per_max_priority_clip=10.0,
        grad_clip_norm=10.0,
    ):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.action_dim = int(action_dim)
        self.gamma = float(gamma)
        self.n_step = int(n_step)
        self.target_update_freq = int(target_update_freq)
        self.update_count = 0
        self.per_beta = float(per_beta_start)
        self.per_beta_increment = float(per_beta_increment)
        self.grad_clip_norm = (
            float(grad_clip_norm) if grad_clip_norm is not None else None
        )

        self.num_atoms = int(num_atoms)
        self.v_min = float(v_min)
        self.v_max = float(v_max)
        self.delta_z = (self.v_max - self.v_min) / float(self.num_atoms - 1)
        self.support = torch.linspace(
            self.v_min, self.v_max, self.num_atoms, device=self.device
        )

        self.q_network = DuelingC51Network(
            state_dim,
            action_dim,
            self.num_atoms,
            hidden_dim=hidden_dim,
            use_noisy=True,
        ).to(self.device)
        self.target_network = DuelingC51Network(
            state_dim,
            action_dim,
            self.num_atoms,
            hidden_dim=hidden_dim,
            use_noisy=True,
        ).to(self.device)
        self.target_network.load_state_dict(self.q_network.state_dict())
        self.target_network.eval()

        self.optimizer = torch.optim.Adam(self.q_network.parameters(), lr=lr)
        self._per_buffer = PrioritizedRainbowBuffer(
            buffer_capacity,
            alpha=per_alpha,
            eps=per_eps,
            max_priority_clip=per_max_priority_clip,
        )
        self.buffer = _RainbowBufferProxy(self)
        self.n_deque = deque()
        self._training = True
        self.epsilon = 0.0

    def set_training_mode(self, training: bool):
        self._training = bool(training)
        self.q_network.train(training)

    def _on_env_transition(self, state, action, reward, next_state, done):
        self.n_deque.append((state, action, reward, next_state, done))
        if done:
            while len(self.n_deque) > 0:
                self._emit_n_step()
        elif len(self.n_deque) >= self.n_step:
            self._emit_n_step()

    def _emit_n_step(self):
        acc_r = 0.0
        g = 1.0
        s_next_final = None
        done_final = False
        k = 0
        for i, (_, _, r, s2, di) in enumerate(self.n_deque):
            acc_r += g * r
            g *= self.gamma
            k = i + 1
            s_next_final = s2
            done_final = di
            if i == self.n_step - 1 or di:
                break
        s0, a0 = self.n_deque[0][0], self.n_deque[0][1]
        boot = 0.0 if done_final else float(self.gamma ** k)
        self._per_buffer.add(s0, a0, acc_r, s_next_final, done_final, boot)
        self.n_deque.popleft()

    def flush_pending(self):
        while len(self.n_deque) > 0:
            self._emit_n_step()

    def _dist(self, net, states):
        logits = net(states)
        probs = torch.softmax(logits, dim=-1)
        return probs.clamp(min=1e-6)

    def _q_values(self, probs):
        return torch.sum(probs * self.support.view(1, 1, -1), dim=-1)

    def choose_action(self, state):
        if self._training:
            self.q_network.train(True)
            self.q_network.reset_noise()
        state_t = torch.FloatTensor(state).unsqueeze(0).to(self.device)
        with torch.no_grad():
            probs = self._dist(self.q_network, state_t)
            q_vals = self._q_values(probs)
        return int(q_vals.argmax(dim=1).item())

    def update(self, batch_size):
        if len(self._per_buffer) < batch_size:
            return

        (
            states,
            actions,
            rewards,
            next_states,
            dones,
            bootstrap_gamma,
            indices,
            weights,
        ) = self._per_buffer.sample(batch_size, beta=self.per_beta)

        batch_size = states.size(0)

        with torch.no_grad():
            next_probs_online = self._dist(self.q_network, next_states)
            next_q_online = self._q_values(next_probs_online)
            next_actions = next_q_online.argmax(dim=1)

            next_probs_target = self._dist(self.target_network, next_states)
            next_dist = next_probs_target[
                torch.arange(batch_size, device=self.device), next_actions
            ]

            tz = rewards + (1.0 - dones) * bootstrap_gamma * self.support.view(1, -1)
            tz = tz.clamp(self.v_min, self.v_max)
            b = (tz - self.v_min) / self.delta_z
            l = b.floor().long()
            u = b.ceil().long()

            proj_dist = torch.zeros(batch_size, self.num_atoms, device=self.device)
            offset = (
                torch.arange(batch_size, device=self.device)
                .unsqueeze(1)
                .expand(batch_size, self.num_atoms)
            )

            proj_dist.view(-1).index_add_(
                0,
                (l + offset * self.num_atoms).view(-1),
                (next_dist * (u.float() - b)).view(-1),
            )
            proj_dist.view(-1).index_add_(
                0,
                (u + offset * self.num_atoms).view(-1),
                (next_dist * (b - l.float())).view(-1),
            )

            eq_mask = u == l
            if eq_mask.any():
                proj_dist[eq_mask] += next_dist[eq_mask]

        self.q_network.reset_noise()
        logits = self.q_network(states)
        log_probs = torch.log_softmax(logits, dim=-1)
        chosen_log_probs = log_probs[
            torch.arange(batch_size, device=self.device), actions.squeeze(1)
        ]
        cross_entropy = -(proj_dist * chosen_log_probs).sum(dim=1)
        loss = (weights.squeeze(1) * cross_entropy).mean()

        self.optimizer.zero_grad()
        loss.backward()
        if self.grad_clip_norm is not None:
            torch.nn.utils.clip_grad_norm_(
                self.q_network.parameters(), self.grad_clip_norm
            )
        self.optimizer.step()

        self._per_buffer.update_priorities(
            indices, cross_entropy.detach().cpu().numpy()
        )

        self.per_beta = min(1.0, self.per_beta + self.per_beta_increment)

        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())