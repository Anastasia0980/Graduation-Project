import json
import torch
import numpy as np
from pettingzoo.classic.tictactoe_v3 import env as ttt_env

from template import AgentFactory

# 读取配置
with open("config.json", "r") as f:
    config = json.load(f)

agent = AgentFactory.create_from_config("config.json")

env = ttt_env()

optimizer = torch.optim.Adam(agent.q_net.parameters(), lr=1e-3)
loss_fn = torch.nn.MSELoss()

gamma = 0.99
episodes = 50

for episode in range(episodes):

    env.reset()

    for agent_name in env.agent_iter():

        obs, reward, terminated, truncated, info = env.last()
        done = terminated or truncated

        if done:
            env.step(None)
            continue

        state = torch.FloatTensor(obs["observation"].flatten()).unsqueeze(0).to(agent.device)
        action_mask = obs["action_mask"]

        # epsilon-greedy
        if np.random.rand() < 0.1:
            valid_actions = np.where(action_mask == 1)[0]
            action = np.random.choice(valid_actions)
        else:
            action = agent.act(obs)

        env.step(action)

        target = torch.tensor([reward], dtype=torch.float32).to(agent.device)

        q_values = agent.q_net(state)
        loss = loss_fn(q_values[0, action], target)

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

print("训练完成，保存模型...")

# 保存模型
torch.save(agent.q_net.state_dict(), "model.pt")

print("模型已保存为 model.pt")