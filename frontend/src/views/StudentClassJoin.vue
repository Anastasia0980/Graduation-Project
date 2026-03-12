<template>
  <div class='page'>
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

    <div class='layout'>
      <StudentSidebar
        :logged-in='true'
        active-menu='class'
        :task-menu-open='false'
        @profile-click='goProfile'
        @class-click='goStudentClass'
        @tournament-click='goTournament'
        @toggle-task-menu='goHomeOpenTasks'
        @open-task-click='goHomeOpenTasks'
        @ended-task-click='goHomeEndedTasks'
        @history-click='goHistory'
      />

      <main class='content-area'>
        <div class='page-header'>
          <h2>我的班级</h2>
        </div>

        <div v-if='!joinedClass' class='card'>
          <div class='card-title'>加入班级</div>

          <div class='join-box'>
            <div class='form-item'>
              <label>班级码</label>
              <input v-model='joinCode' type='text' placeholder='请输入教师提供的班级码' />
            </div>

            <div class='tip-box'>
              当前可输入示例班级码：
              <span class='code-text'>RL2026A01</span>
            </div>

            <div class='action-row'>
              <button class='primary-btn' @click='joinClass'>加入班级</button>
            </div>
          </div>
        </div>

        <div v-else class='content-grid'>
          <div class='card'>
            <div class='card-title'>当前班级信息</div>

            <div class='info-grid'>
              <div class='info-item'>
                <span class='label'>班级名称</span>
                <span class='value'>{{ classInfo.name }}</span>
              </div>

              <div class='info-item'>
                <span class='label'>班级码</span>
                <span class='value'>{{ classInfo.classCode }}</span>
              </div>

              <div class='info-item'>
                <span class='label'>任课教师</span>
                <span class='value'>{{ classInfo.teacher }}</span>
              </div>

              <div class='info-item'>
                <span class='label'>班级人数</span>
                <span class='value'>{{ classInfo.students.length }}</span>
              </div>
            </div>

            <div class='action-row left-row'>
              <button class='danger-btn' @click='showLeaveDialog = true'>退出班级</button>
            </div>
          </div>

          <div class='card'>
            <div class='card-title'>班级成员</div>

            <table class='common-table'>
              <thead>
                <tr>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>身份</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in classInfo.students' :key='item.studentId'>
                  <td>{{ item.studentId }}</td>
                  <td>{{ item.name }}</td>
                  <td>{{ item.role }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>

    <div v-if='showSuccessDialog' class='dialog-mask' @click='closeSuccessDialog'>
      <div class='dialog-box small-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>加入成功</div>
        </div>
        <div class='dialog-body'>
          已成功加入班级 {{ classInfo.name }}。
        </div>
        <div class='dialog-footer'>
          <button class='primary-btn' @click='closeSuccessDialog'>确定</button>
        </div>
      </div>
    </div>

    <div v-if='showLeaveDialog' class='dialog-mask' @click='showLeaveDialog = false'>
      <div class='dialog-box small-box' @click.stop>
        <div class='dialog-header'>
          <div class='dialog-title'>退出班级</div>
        </div>
        <div class='dialog-body'>
          确定要退出当前班级吗？
        </div>
        <div class='dialog-footer'>
          <button class='secondary-btn' @click='showLeaveDialog = false'>取消</button>
          <button class='danger-btn' @click='leaveClass'>退出</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'

export default {
  name: 'StudentClassJoinView',
  components: {
    AppTopbar,
    StudentSidebar
  },
  data () {
    return {
      joinCode: '',
      joinedClass: false,
      showSuccessDialog: false,
      showLeaveDialog: false,
      classInfo: {
        name: '人工智能 2201',
        classCode: 'RL2026A01',
        teacher: '王老师',
        students: [
          { studentId: '2023123456', name: '张三', role: '学生' },
          { studentId: '2023123457', name: '李四', role: '学生' },
          { studentId: '2023123458', name: '王五', role: '学生' },
          { studentId: 'T20260001', name: '王老师', role: '教师' }
        ]
      }
    }
  },
  methods: {
    joinClass () {
      if (!this.joinCode.trim()) {
        alert('请输入班级码')
        return
      }
      this.joinedClass = true
      this.showSuccessDialog = true
    },
    leaveClass () {
      this.joinedClass = false
      this.joinCode = ''
      this.showLeaveDialog = false
    },
    closeSuccessDialog () {
      this.showSuccessDialog = false
    },
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goHomeEndedTasks () {
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goProfile () {
      this.$router.push('/student/profile')
    },
    goStudentClass () {
      this.$router.push('/student/class')
    },
    goTournament () {
      this.$router.push('/student/tournament')
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'teacher')
      this.$router.push('/teacher/home')
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

.card-title {
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.join-box {
  max-width: 520px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.form-item label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.form-item input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.tip-box {
  margin-bottom: 18px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 14px;
  color: #606266;
}

.code-text {
  color: #1f4e8c;
  font-weight: 700;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.left-row {
  justify-content: flex-start;
  margin-top: 18px;
}

.primary-btn,
.secondary-btn,
.danger-btn {
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

.danger-btn {
  border: none;
  background: #d9534f;
  color: #ffffff;
}

.danger-btn:hover {
  background: #c9302c;
}

.content-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
}

.label {
  font-size: 13px;
  color: #909399;
}

.value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.common-table {
  width: 100%;
  border-collapse: collapse;
}

.common-table th,
.common-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #ebeef5;
  text-align: left;
  font-size: 14px;
}

.common-table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(31, 45, 61, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  padding: 20px;
}

.dialog-box {
  width: 520px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.small-box {
  width: 420px;
}

.dialog-header {
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.dialog-body {
  padding: 20px;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1000px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .card {
    overflow-x: auto;
  }

  .common-table {
    min-width: 560px;
  }
}
</style>
