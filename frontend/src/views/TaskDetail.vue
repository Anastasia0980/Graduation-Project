<template>
  <div class='detail-page'>
    <AppTopbar
      :logged-in='true'
      user-name='张三'
      current-role='student'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='goProfile'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='detail-topbar'>
      <button class='back-btn' @click='goBack'>返回</button>
      <div class='page-title'>任务详情</div>
    </div>

    <div class='detail-body'>
      <div class='main-content'>
        <div class='content-card'>
          <div class='task-header'>
            <div>
              <h1 class='task-title'>井字棋对战游戏</h1>
              <div class='task-subtitle'>PettingZoo 环境下的双智能体对战测评任务</div>
            </div>
            <div class='task-status'>开放中</div>
          </div>

          <section class='detail-section'>
            <h2>简介</h2>
            <div class='intro-block'>
              <div class='intro-text'>
                <p>
                  本任务基于 PettingZoo 提供的井字棋环境开展双智能体对战测评。学生需要提交自己的强化学习智能体模型及相关配置文件，
                  平台将在统一环境下自动完成对战、统计胜负结果并记录测评数据。
                </p>
                <p>
                  井字棋属于典型的双人轮流决策问题，状态空间规模较小，规则清晰，适合作为多智能体强化学习与对战评测的入门任务。
                  在本平台中，智能体需要在有限时间内完成动作决策，并遵守环境规则，系统将依据对局结果、非法动作情况及任务约束生成最终成绩。
                </p>
                <p>
                  该任务重点考察学生对离散动作空间建模、基础策略学习、对战流程适配以及模型提交规范的掌握情况。
                </p>
              </div>

              <div class='intro-image-box'>
                <img :src='tictactoeImage' alt='井字棋环境示意图' class='intro-image' />
              </div>
            </div>
          </section>

          <section class='detail-section'>
            <h2>规则</h2>
            <p>
              井字棋棋盘由 3 × 3 的九个格子组成，两名智能体轮流在空位置落子。一方使用 X，另一方使用 O。
              任意一方若先在横向、纵向或对角线上形成连续三个相同标记，则该方获胜，当前对局立即结束。
              若棋盘全部填满且无人形成三连，则判定为平局。
            </p>
            <p>
              智能体只能在尚未被占用的位置执行动作。若提交的动作非法，例如落在已被占用的位置，环境会判定该动作无效，
              平台在评测时也会将其作为违规行为记录。
            </p>
          </section>

          <section class='detail-section'>
            <h2>观测</h2>
            <p>
              环境观测由当前棋盘状态构成，可理解为对 9 个位置的离散描述。每个位置通常对应三种状态：
              空位、己方落子、对方落子。智能体在每个决策时刻需要根据当前观测判断最优落子位置。
            </p>
            <p>
              在具体实现中，你可以将观测编码为向量、矩阵或适配模型输入的张量形式，但提交时需要与平台测评脚本要求保持一致。
            </p>
          </section>

          <section class='detail-section'>
            <h2>动作空间</h2>
            <p>动作空间为离散动作空间，可表示为：</p>
            <div class='formula-box'>
              <span class='formula'>A = {0, 1, 2, 3, 4, 5, 6, 7, 8}</span>
            </div>
            <p>
              其中每个动作表示在棋盘某一个位置落子。动作编号与棋盘位置的映射由平台测评程序统一定义。
              智能体输出的动作必须为合法编号，并且对应位置必须为空。
            </p>
          </section>

          <section class='detail-section'>
            <h2>奖励</h2>
            <p>平台测评时可采用简化奖励设计来衡量智能体对局表现。一个常见的奖励形式如下：</p>
            <div class='formula-box'>
              <span class='formula'>R = winReward · I_win + drawReward · I_draw - invalidPenalty · I_invalid</span>
            </div>
            <p>其中：</p>
            <ul class='detail-list'>
              <li><span class='var-name'>winReward</span>：获胜奖励</li>
              <li><span class='var-name'>drawReward</span>：平局奖励</li>
              <li><span class='var-name'>invalidPenalty</span>：非法动作惩罚</li>
              <li><span class='var-name'>I_win</span>、<span class='var-name'>I_draw</span>、<span class='var-name'>I_invalid</span>：指示变量，满足条件时取 1，否则取 0</li>
            </ul>
            <p>
              在最终测评中，平台更关注整场对局结果、总胜率及提交是否符合规则要求，因此奖励公式主要用于说明环境设计思想，
              具体得分统计以平台评测逻辑为准。
            </p>
          </section>

          <section class='detail-section'>
            <h2>评测说明</h2>
            <p>
              本任务采用 PettingZoo 井字棋环境进行统一测评。学生提交模型后，平台会在预设环境中自动执行对战过程，
              并记录每局结果、胜负信息及必要的明细数据。
            </p>
            <ul class='detail-list'>
              <li>测评环境：PettingZoo 井字棋对战环境</li>
              <li>任务类型：双智能体对战测评</li>
              <li>模型要求：需提交可被平台测评脚本正常加载的模型文件及配置文件</li>
              <li>动作限制：仅允许输出合法离散动作，非法动作将被记录</li>
              <li>时间限制：单步决策时间与单次任务总执行时间均受平台控制</li>
              <li>结果输出：平台保存对战结果、任务状态及相关视频文件</li>
            </ul>
            <p>
              请确保提交内容命名规范、路径结构清晰，并与教师提供的实验要求保持一致。若模型无法加载、配置缺失或运行异常，
              平台可能将该次任务标记为失败。
            </p>
          </section>
        </div>
      </div>

      <aside class='right-sidebar'>
        <div class='side-card'>
          <div class='side-title'>提交</div>
          <div class='side-desc'>
            点击下方按钮进入提交页面，上传模型文件与配置文件并发起测评任务。
          </div>
          <button class='primary-btn submit-action-btn' @click='goSubmit'>提交任务</button>
        </div>

        <div class='side-card'>
          <div class='side-title'>排行榜</div>

          <div class='mini-ranking'>
            <div
              v-for='item in topRanking'
              :key='item.rank'
              class='ranking-row'
            >
              <div class='ranking-left'>
                <span class='ranking-no'>{{ item.rank }}</span>
                <span class='ranking-name'>{{ item.name }}</span>
              </div>
              <div class='ranking-score'>{{ item.score }}</div>
            </div>
          </div>

          <div class='expand-link' @click='goRanking'>展开全部</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import tictactoeImage from '../assets/tictactoe.png'

export default {
  name: 'TaskDetailView',
  components: {
    AppTopbar
  },
  data () {
    return {
      tictactoeImage,
      topRanking: [
        { rank: 1, name: '李四', score: '1280' },
        { rank: 2, name: '张三', score: '1205' },
        { rank: 3, name: '王五', score: '1150' }
      ]
    }
  },
  methods: {
    goBack () {
      this.$router.back()
    },
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/')
    },
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.$router.push('/')
    },
    goSubmit () {
      this.$router.push('/battle')
    },
    goRanking () {
      this.$router.push('/student/ranking')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  color: #303133;
}

.detail-topbar {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}

.back-btn {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.detail-body {
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 20px;
  padding: 20px;
  align-items: start;
}

.main-content {
  min-width: 0;
}

.content-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 24px;
}

.task-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 24px;
}

.task-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: #1f2d3d;
}

.task-subtitle {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.task-status {
  min-width: 72px;
  height: 32px;
  padding: 0 12px;
  background: #ecf5ff;
  color: #1f4e8c;
  border: 1px solid #bfd7f4;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.detail-section {
  margin-bottom: 28px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h2 {
  margin: 0 0 14px;
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.detail-section p {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.9;
  color: #303133;
}

.intro-block {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;
  align-items: start;
}

.intro-image-box {
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.intro-image {
  width: 100%;
  border-radius: 6px;
  display: block;
}

.formula-box {
  margin: 12px 0 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow-x: auto;
}

.formula {
  font-size: 15px;
  color: #1f2d3d;
  font-family: 'Times New Roman', serif;
}

.detail-list {
  margin: 0 0 12px 18px;
  padding: 0;
  color: #303133;
}

.detail-list li {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.8;
}

.var-name {
  font-family: 'Times New Roman', serif;
}

.right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 84px;
}

.side-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.side-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 14px;
}

.side-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 16px;
}

.primary-btn {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 4px;
  background: #1f4e8c;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.primary-btn:hover {
  background: #173b69;
}

.mini-ranking {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.ranking-row:last-child {
  border-bottom: none;
  padding-bottom: 4px;
}

.ranking-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ranking-no {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #1f4e8c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.ranking-name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.ranking-score {
  font-size: 14px;
  color: #606266;
}

.expand-link {
  margin-top: 14px;
  font-size: 14px;
  color: #1f4e8c;
  cursor: pointer;
}

.expand-link:hover {
  color: #173b69;
  text-decoration: underline;
}

@media (max-width: 1100px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .right-sidebar {
    position: static;
  }
}

@media (max-width: 900px) {
  .intro-block {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .detail-topbar {
    height: auto;
    padding: 14px 16px;
    flex-wrap: wrap;
  }

  .detail-body {
    padding: 16px;
  }

  .content-card,
  .side-card {
    padding: 16px;
  }

  .task-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-title {
    font-size: 24px;
  }
}
</style>
