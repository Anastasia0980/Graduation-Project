<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      user-name='王老师'
      current-role='teacher'
      active-nav='home'
      @platform-click='goTeacherHome'
      @user-click='goTeacherHome'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <TeacherSidebar
        active-menu='publish-task'
        @teacher-home-click='goTeacherHome'
        @publish-click='goPublishTask'
        @manage-click='goTaskManage'
        @class-data-click='goClassData'
        @export-click='goExportScore'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>发布任务</h2>
        </div>

        <div class='card'>
          <div class='card-title'>任务基础信息</div>

          <div class='form-grid'>
            <div class='form-item full-width'>
              <label>任务名称</label>
              <input type='text' placeholder='请输入任务名称' />
            </div>

            <div class='form-item'>
              <label>适用班级</label>
              <select>
                <option>人工智能 2201</option>
                <option>人工智能 2202</option>
                <option>软件工程 2201</option>
              </select>
            </div>

            <div class='form-item'>
              <label>测评环境</label>
              <select>
                <option>PettingZoo 井字棋环境</option>
                <option>PettingZoo Connect Four 环境</option>
              </select>
            </div>

            <div class='form-item'>
              <label>开始时间</label>
              <input type='datetime-local' />
            </div>

            <div class='form-item'>
              <label>截止时间</label>
              <input type='datetime-local' />
            </div>
          </div>
        </div>

        <div class='card section-space'>
          <div class='card-title'>任务模式配置</div>

          <div class='mode-grid'>
            <div class='form-item'>
              <label>任务类型</label>
              <select v-model='taskMode'>
                <option value='single'>单人模式</option>
                <option value='battle'>对战模式</option>
                <option value='tournament'>团队锦标赛模式</option>
              </select>
            </div>
          </div>

          <div class='mode-tip-box'>
            <div class='mode-tip-title'>当前任务说明</div>
            <div class='mode-tip-text'>
              <template v-if='taskMode === "single"'>
                当前为单人模式。学生提交模型后，平台在预设环境中独立完成测评，并返回奖励、结果和相关记录。
              </template>
              <template v-else-if='taskMode === "battle"'>
                当前为对战模式。学生进入任务后可选择真人对战；若教师上传了任意难度的人机模型，学生端还将开放人机对战入口。
              </template>
              <template v-else>
                当前为团队锦标赛模式。学生需先完成组队，再由队伍统一提交模型，平台将按淘汰赛机制逐轮决出最终胜者。
              </template>
            </div>
          </div>
        </div>

        <div v-if='taskMode === "battle"' class='card section-space'>
          <div class='card-title'>人机模型配置</div>

          <div class='difficulty-box'>
            <div class='difficulty-item'>
              <div class='difficulty-header'>
                <div class='difficulty-title'>简单模式人机模型</div>
                <div class='difficulty-status'>未接入上传逻辑</div>
              </div>
              <div class='upload-box'>
                <button class='upload-btn' type='button'>上传简单模型</button>
                <span class='upload-text'>当前未选择文件</span>
              </div>
            </div>

            <div class='difficulty-item'>
              <div class='difficulty-header'>
                <div class='difficulty-title'>中等模式人机模型</div>
                <div class='difficulty-status'>未接入上传逻辑</div>
              </div>
              <div class='upload-box'>
                <button class='upload-btn' type='button'>上传中等模型</button>
                <span class='upload-text'>当前未选择文件</span>
              </div>
            </div>

            <div class='difficulty-item'>
              <div class='difficulty-header'>
                <div class='difficulty-title'>困难模式人机模型</div>
                <div class='difficulty-status'>未接入上传逻辑</div>
              </div>
              <div class='upload-box'>
                <button class='upload-btn' type='button'>上传困难模型</button>
                <span class='upload-text'>当前未选择文件</span>
              </div>
            </div>
          </div>

          <div class='upload-tip top-gap'>
            说明：只要教师上传了任意一个难度的人机模型，学生端“人机对战”按钮就会由灰色变为可点击。
          </div>
        </div>

        <div v-if='taskMode === "tournament"' class='card section-space'>
          <div class='card-title'>团队锦标赛配置</div>

          <div class='form-grid'>
            <div class='form-item'>
              <label>队伍最少人数</label>
              <select v-model='teamMin'>
                <option value='1'>1 人</option>
                <option value='2'>2 人</option>
                <option value='3'>3 人</option>
              </select>
            </div>

            <div class='form-item'>
              <label>队伍最多人数</label>
              <select v-model='teamMax'>
                <option value='1'>1 人</option>
                <option value='2'>2 人</option>
                <option value='3'>3 人</option>
              </select>
            </div>

            <div class='form-item full-width'>
              <label>模型提交策略</label>
              <select v-model='submitStrategy'>
                <option value='once'>只提交一次模型，系统自动完成全部轮次</option>
                <option value='round'>晋级后允许重新提交模型（当前仅做界面预留）</option>
              </select>
            </div>

            <div class='form-item full-width'>
              <label>淘汰赛说明</label>
              <textarea
                rows='5'
                placeholder='请输入淘汰赛机制说明，例如晋级规则、对局轮次、决赛方式、冠军判定方式等'
              ></textarea>
            </div>
          </div>
        </div>

        <div class='card section-space'>
          <div class='card-title'>任务说明</div>

          <div class='form-grid'>
            <div class='form-item full-width'>
              <label>简介</label>
              <textarea rows='5' placeholder='请输入任务简介，例如任务背景、教学目标、适用场景等'></textarea>
            </div>

            <div class='form-item full-width'>
              <label>规则</label>
              <textarea rows='6' placeholder='请输入任务规则，例如对战规则、胜负判定规则、非法动作限制等'></textarea>
            </div>

            <div class='form-item full-width'>
              <label>观测</label>
              <textarea rows='5' placeholder='请输入环境观测说明，例如观测维度、状态表示、输入格式等'></textarea>
            </div>

            <div class='form-item full-width'>
              <label>动作空间</label>
              <textarea rows='4' placeholder='请输入动作空间说明，例如离散动作数量、动作编号与具体操作的映射关系等'></textarea>
            </div>

            <div class='form-item full-width'>
              <label>奖励</label>
              <textarea rows='5' placeholder='请输入奖励设计说明，例如获胜奖励、平局奖励、非法动作惩罚等'></textarea>
            </div>

            <div class='form-item full-width'>
              <label>评测说明</label>
              <textarea rows='6' placeholder='请输入评测说明，例如测评环境、时间限制、提交要求、结果记录方式等'></textarea>
            </div>

            <div class='form-item full-width'>
              <label>算法模板文件</label>

              <div class='upload-box'>
                <button class='upload-btn' type='button'>选择模板文件</button>
                <span class='upload-text'>当前未选择文件</span>
              </div>

              <div class='upload-tip'>
                可上传算法模板文件，供学生下载参考。当前按钮仅为界面展示，上传功能暂未接入。
              </div>
            </div>
          </div>
        </div>

        <div class='bottom-action-row'>
          <button class='secondary-btn' type='button'>保存草稿</button>
          <button class='primary-btn' type='button'>发布任务</button>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

export default {
  name: 'TeacherPublishTaskView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      taskMode: 'single',
      teamMin: '1',
      teamMax: '3',
      submitStrategy: 'once'
    }
  },
  methods: {
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goPublishTask () {
      this.$router.push('/teacher/publish')
    },
    goTaskManage () {
      this.$router.push('/teacher/tasks')
    },
    goClassData () {
      this.$router.push('/teacher/classes')
    },
    goExportScore () {
      this.$router.push('/teacher/export')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'student')
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    logout () {
      sessionStorage.setItem('mock_logged_out_view', 'true')
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.page {
  min-height: 100vh;
  background: #f5f7fa;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
  color: #303133;
}

.layout {
  display: flex;
  min-height: calc(100vh - 64px);
}

.content-area {
  flex: 1;
  padding: 20px;
}

.page-header {
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.section-space {
  margin-top: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #1f2d3d;
}

.form-grid,
.mode-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.form-item input,
.form-item select,
.form-item textarea {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 14px;
  background: #ffffff;
  color: #303133;
}

.form-item input:focus,
.form-item select:focus,
.form-item textarea:focus {
  outline: none;
  border-color: #1f4e8c;
}

.form-item textarea {
  resize: vertical;
  line-height: 1.8;
  min-height: 110px;
}

.mode-tip-box {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
}

.mode-tip-title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
}

.mode-tip-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.difficulty-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.difficulty-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
}

.difficulty-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.difficulty-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2d3d;
}

.difficulty-status {
  font-size: 13px;
  color: #909399;
}

.upload-box {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 42px;
}

.upload-btn {
  height: 38px;
  min-width: 120px;
  border: 1px solid #1f4e8c;
  border-radius: 4px;
  background: #ffffff;
  color: #1f4e8c;
  font-size: 14px;
  cursor: pointer;
}

.upload-btn:hover {
  background: #ecf5ff;
}

.upload-text {
  font-size: 14px;
  color: #909399;
}

.upload-tip {
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
}

.top-gap {
  margin-top: 12px;
}

.bottom-action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.primary-btn,
.secondary-btn {
  height: 38px;
  min-width: 96px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn {
  border: none;
  background: #1f4e8c;
  color: #ffffff;
}

.primary-btn:hover {
  background: #173b69;
}

.secondary-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
}

.secondary-btn:hover {
  border-color: #1f4e8c;
  color: #1f4e8c;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .form-grid,
  .mode-grid {
    grid-template-columns: 1fr;
  }

  .difficulty-header,
  .upload-box {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
