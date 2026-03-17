<template>
  <div class='page'>
    <AppTopbar
      :logged-in='true'
      :user-name='displayUserName'
      current-role='student'
      active-nav='home'
      @platform-click='goHomeOpenTasks'
      @user-click='goProfile'
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
              请输入教师提供的真实班级码
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
                <span class='value'>{{ classInfo.teacher || '—' }}</span>
              </div>

              <div class='info-item'>
                <span class='label'>班级人数</span>
                <span class='value'>{{ classInfo.studentCount }}</span>
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
                <tr v-if='classInfo.students.length === 0'>
                  <td colspan='3' class='empty-cell'>当前后端暂未提供学生端班级成员列表接口</td>
                </tr>
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
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import StudentSidebar from '../components/StudentSidebar.vue'

const API_BASE = 'http://localhost:8080'

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
      loading: false,
      displayUserName: localStorage.getItem('auth_name') || '学生',
      classInfo: {
        id: null,
        name: '',
        classCode: '',
        teacher: '',
        studentCount: 0,
        students: []
      }
    }
  },
  created () {
    this.loadCurrentClassInfo()
  },
  methods: {
    getAuthHeaders () {
      const token = localStorage.getItem('auth_token')
      return token ? { Authorization: `Bearer ${token}` } : {}
    },
    async loadCurrentClassInfo () {
      try {
        const response = await fetch(`${API_BASE}/user/me/class`, {
          method: 'GET',
          headers: {
            ...this.getAuthHeaders()
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0 || !result.data) {
          this.joinedClass = false
          this.classInfo = {
            id: null,
            name: '',
            classCode: '',
            teacher: '',
            studentCount: 0,
            students: []
          }
          return
        }

        const className = result.data
        this.joinedClass = true
        this.classInfo.name = className || ''
        await this.fillClassInfoByName(className)
      } catch (error) {
        this.joinedClass = false
      }
    },
    async fillClassInfoByName (className) {
      try {
        const response = await fetch(`${API_BASE}/class?pageNum=0&pageSize=100&isDeleted=false`, {
          method: 'GET',
          headers: {
            ...this.getAuthHeaders()
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0 || !result.data || !result.data.content) {
          this.classInfo.classCode = ''
          this.classInfo.teacher = ''
          this.classInfo.studentCount = 0
          this.classInfo.students = []
          return
        }

        const matchedClass = result.data.content.find(item => item.name === className)
        if (!matchedClass) {
          this.classInfo.id = null
          this.classInfo.classCode = ''
          this.classInfo.teacher = ''
          this.classInfo.studentCount = 0
          this.classInfo.students = []
          return
        }

        this.classInfo.id = matchedClass.id
        this.classInfo.classCode = matchedClass.code || ''
        this.classInfo.teacher = ''
        this.classInfo.studentCount = 0
        this.classInfo.students = []
      } catch (error) {
        this.classInfo.classCode = ''
        this.classInfo.teacher = ''
        this.classInfo.studentCount = 0
        this.classInfo.students = []
      }
    },
    async joinClass () {
      if (!this.joinCode.trim()) {
        ElMessage.warning('请输入班级码')
        return
      }

      this.loading = true
      try {
        const params = new URLSearchParams()
        params.append('code', this.joinCode.trim())

        const response = await fetch(`${API_BASE}/user/me/class`, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
            ...this.getAuthHeaders()
          },
          body: params.toString()
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '加入班级失败')
          return
        }

        await this.loadCurrentClassInfo()
        this.showSuccessDialog = true
        ElMessage.success('加入班级成功')
        this.joinCode = ''
      } catch (error) {
        ElMessage.error('加入班级失败，请检查后端是否已启动')
      } finally {
        this.loading = false
      }
    },
    async leaveClass () {
      try {
        const response = await fetch(`${API_BASE}/user/me/class/quit`, {
          method: 'PATCH',
          headers: {
            ...this.getAuthHeaders()
          }
        })
        const result = await response.json()

        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '退出班级失败')
          return
        }

        this.showLeaveDialog = false
        this.joinedClass = false
        ElMessage.success('已退出班级')
        this.classInfo = {
          id: null,
          name: '',
          classCode: '',
          teacher: '',
          studentCount: 0,
          students: []
        }
      } catch (error) {
        ElMessage.error('退出班级失败，请检查后端是否已启动')
      }
    },
    closeSuccessDialog () {
      this.showSuccessDialog = false
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
    goHomeOpenTasks () {
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    goHomeEndedTasks () {
      this.$router.push({ path: '/', query: { tab: 'ended' } })
    },
    goHistory () {
      this.$router.push('/student/history')
    },
    logout () {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_role')
      localStorage.removeItem('auth_name')
      localStorage.removeItem('auth_email')
      this.$router.push('/login')
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
  color: #303133;
  font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
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
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.06);
  padding: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.join-box {
  max-width: 520px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.form-item input {
  width: 100%;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 14px;
  outline: none;
}

.form-item input:focus {
  border-color: #1f4e8c;
}

.tip-box {
  background: #f4f7fb;
  border: 1px solid #e4ebf3;
  color: #606266;
  padding: 12px 14px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.info-item {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
}

.label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.common-table {
  width: 100%;
  border-collapse: collapse;
  background: #ffffff;
}

.common-table th,
.common-table td {
  border: 1px solid #ebeef5;
  padding: 12px;
  text-align: center;
  font-size: 14px;
}

.common-table th {
  background: #f7f9fc;
  color: #1f2d3d;
  font-weight: 700;
}

.empty-cell {
  color: #909399;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.left-row {
  justify-content: flex-start;
}

.primary-btn,
.secondary-btn,
.danger-btn {
  min-width: 90px;
  height: 36px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.primary-btn {
  background: #1f4e8c;
  color: #ffffff;
  border: none;
}

.primary-btn:hover {
  background: #173b69;
}

.secondary-btn {
  background: #ffffff;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.secondary-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.danger-btn {
  background: #c45656;
  color: #ffffff;
  border: none;
}

.danger-btn:hover {
  background: #a63f3f;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 2000;
}

.dialog-box {
  width: 420px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #dcdfe6;
  box-shadow: 0 12px 30px rgba(31, 45, 61, 0.18);
}

.small-box {
  width: 380px;
}

.dialog-header {
  padding: 16px 18px;
  border-bottom: 1px solid #ebeef5;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.dialog-body {
  padding: 20px 18px;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.dialog-footer {
  padding: 0 18px 18px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
