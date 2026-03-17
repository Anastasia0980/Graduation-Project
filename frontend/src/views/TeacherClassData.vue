<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      :user-name="displayUserName"
      current-role="teacher"
      active-nav="home"
      @platform-click="goTeacherHome"
      @user-click="goTeacherHome"
      @logout="logout"
    />

    <div class="layout">
      <TeacherSidebar
        active-menu="class-data"
        @teacher-home-click="goTeacherHome"
        @publish-click="goPublishTask"
        @manage-click="goTaskManage"
        @class-data-click="goClassData"
        @export-click="goExportScore"
      />

      <main class="content-area">
        <div class="page-header">
          <h2>班级管理</h2>
        </div>

        <div class="class-layout">
          <div class="class-list-card">
            <div class="card-header">
              <div class="card-title">班级列表</div>
              <button class="primary-btn small-btn" @click="openCreateClassDialog">创建班级</button>
            </div>

            <div class="class-list">
              <div
                v-for="item in classList"
                :key="item.id"
                class="class-item"
                :class="{ active: selectedClass && selectedClass.id === item.id }"
                @click="selectClass(item)"
              >
                <div class="class-name">{{ item.name }}</div>
                <div class="class-meta">班级码：{{ item.code }}</div>
                <div class="class-meta">人数：{{ item.studentCount }}</div>
              </div>
            </div>
          </div>

          <div class="class-detail-card" v-if="selectedClass">
            <div class="card-header detail-header">
              <div>
                <div class="card-title">班级详情</div>
                <div class="detail-subtitle">
                  当前班级：{{ selectedClass.name }} | 班级码：{{ selectedClass.code || '—' }} | 教师：{{ selectedClass.teacher || '—' }}
                </div>
              </div>

              <button class="danger-btn small-btn" @click="showDismissDialog = true">
                解散班级
              </button>
            </div>

            <table class="common-table">
              <thead>
                <tr>
                  <th>学号</th>
                  <th>姓名</th>
                  <th>邮箱</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="displayedStudents.length === 0">
                  <td colspan="3" class="empty-cell">当前班级暂无学生数据</td>
                </tr>
                <tr v-for="stu in displayedStudents" :key="stu.id || stu.studentId">
                  <td>{{ stu.studentId || stu.id || "—" }}</td>
                  <td>
                    <span class="student-name-link" @click="goStudentDetail(stu)">
                      {{ stu.username || stu.name || "未命名用户" }}
                    </span>
                  </td>
                  <td>{{ stu.email || "—" }}</td>
                </tr>
              </tbody>
            </table>

            <div class="expand-row" v-if="selectedClass.students.length > 3">
              <button class="text-btn" @click="toggleExpand">
                {{ expandAll ? '收起' : '展开全部' }}
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>

    <div v-if="showCreateDialog" class="dialog-mask" @click="closeCreateClassDialog">
      <div class="dialog-box small-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">创建班级</div>
          <button class="close-btn" @click="closeCreateClassDialog">关闭</button>
        </div>

        <div class="dialog-body">
          <div class="form-item">
            <label>班级名称</label>
            <input
              v-model="createClassForm.name"
              type="text"
              placeholder="请输入班级名称"
              maxlength="50"
            >
          </div>
        </div>

        <div class="dialog-footer">
          <button class="secondary-btn" @click="closeCreateClassDialog">取消</button>
          <button class="primary-btn" @click="createClass" :disabled="creatingClass">
            {{ creatingClass ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showDismissDialog" class="dialog-mask" @click="showDismissDialog = false">
      <div class="dialog-box small-dialog" @click.stop>
        <div class="dialog-header">
          <div class="dialog-title">解散班级</div>
          <button class="close-btn" @click="showDismissDialog = false">关闭</button>
        </div>

        <div class="dialog-body">
          确定要解散班级“{{ selectedClass ? selectedClass.name : '' }}”吗？解散后当前页面中的班级信息将被移除。
        </div>

        <div class="dialog-footer">
          <button class="secondary-btn" @click="showDismissDialog = false">取消</button>
          <button class="danger-btn" @click="dismissClass">确定解散</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

const API_BASE = 'http://localhost:8080'

export default {
  name: 'TeacherClassDataView',
  components: {
    AppTopbar,
    TeacherSidebar
  },
  data () {
    return {
      expandAll: false,
      showDismissDialog: false,
      showCreateDialog: false,
      creatingClass: false,
      classList: [],
      selectedClass: null,
      displayUserName: localStorage.getItem('auth_name') || '教师',
      createClassForm: {
        name: ''
      }
    }
  },
  computed: {
    displayedStudents () {
      if (!this.selectedClass || !Array.isArray(this.selectedClass.students)) return []
      return this.expandAll ? this.selectedClass.students : this.selectedClass.students.slice(0, 3)
    }
  },
  created () {
    this.loadClassList()
  },
  methods: {
    getAuthHeaders () {
      const token = localStorage.getItem('auth_token')
      return token ? { Authorization: `Bearer ${token}` } : {}
    },
    async loadClassList () {
      try {
        const response = await fetch(`${API_BASE}/class?pageNum=0&pageSize=100&isDeleted=false`, {
          method: 'GET',
          headers: {
            ...this.getAuthHeaders()
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0 || !result.data || !Array.isArray(result.data.content)) {
          this.classList = []
          this.selectedClass = null
          return
        }

        this.classList = result.data.content.map(item => ({
          id: item.id,
          name: item.name || '',
          code: item.code || '',
          teacher: '',
          studentCount: 0,
          students: []
        }))

        if (this.classList.length > 0) {
          await this.selectClass(this.classList[0])
        } else {
          this.selectedClass = null
        }
      } catch (error) {
        this.classList = []
        this.selectedClass = null
      }
    },
    openCreateClassDialog () {
      this.createClassForm.name = ''
      this.showCreateDialog = true
    },
    closeCreateClassDialog () {
      if (this.creatingClass) return
      this.showCreateDialog = false
      this.createClassForm.name = ''
    },
    async createClass () {
      const className = this.createClassForm.name.trim()
      if (!className) {
        ElMessage.warning('请输入班级名称')
        return
      }

      this.creatingClass = true
      try {
        const response = await fetch(`${API_BASE}/class`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...this.getAuthHeaders()
          },
          body: JSON.stringify({
            name: className,
            code: ''
          })
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '创建班级失败')
          return
        }

        this.showCreateDialog = false
        this.createClassForm.name = ''
        await this.loadClassList()
        ElMessage.success('创建班级成功')
      } catch (error) {
        ElMessage.error('创建班级失败，请检查后端是否已启动')
      } finally {
        this.creatingClass = false
      }
    },
    async selectClass (item) {
      this.expandAll = false
      const target = {
        ...item,
        students: []
      }
      this.selectedClass = target
      await this.loadClassStudents(target.id)
    },
    async loadClassStudents (classId) {
      try {
        const response = await fetch(`${API_BASE}/class/${classId}/users?pageNum=0&pageSize=100&isDeleted=false`, {
          method: 'GET',
          headers: {
            ...this.getAuthHeaders()
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0 || !result.data || !Array.isArray(result.data.content)) {
          if (this.selectedClass && this.selectedClass.id === classId) {
            this.selectedClass.students = []
            this.selectedClass.studentCount = 0
          }
          this.classList = this.classList.map(item => item.id === classId ? { ...item, students: [], studentCount: 0 } : item)
          return
        }
        const students = result.data.content
        if (this.selectedClass && this.selectedClass.id === classId) {
          this.selectedClass.students = students
          this.selectedClass.studentCount = students.length
        }
        this.classList = this.classList.map(item => item.id === classId ? { ...item, students, studentCount: students.length } : item)
      } catch (error) {
        if (this.selectedClass && this.selectedClass.id === classId) {
          this.selectedClass.students = []
          this.selectedClass.studentCount = 0
        }
      }
    },
    toggleExpand () {
      this.expandAll = !this.expandAll
    },
    async dismissClass () {
      if (!this.selectedClass) return
      try {
        const response = await fetch(`${API_BASE}/class/${this.selectedClass.id}`, {
          method: 'DELETE',
          headers: {
            ...this.getAuthHeaders()
          }
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          ElMessage.error(result.message || '解散班级失败')
          return
        }
        this.showDismissDialog = false
        await this.loadClassList()
        ElMessage.success('解散班级成功')
      } catch (error) {
        ElMessage.error('解散班级失败，请检查后端是否已启动')
      }
    },
    goStudentDetail (stu) {
      const studentId = stu.id || stu.studentId
      if (!studentId) return
      this.$router.push(`/teacher/student-detail/${studentId}`)
    },
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

.class-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
}

.class-list-card,
.class-detail-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.06);
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.detail-header {
  align-items: flex-start;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.detail-subtitle {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.class-item {
  border: 1px solid #e5e9f0;
  border-radius: 8px;
  background: #fafcff;
  padding: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.class-item:hover {
  border-color: #1f4e8c;
  background: #f4f8fd;
}

.class-item.active {
  border-color: #1f4e8c;
  background: #eef5fc;
}

.class-name {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.class-meta {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
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

.student-name-link {
  color: #1f4e8c;
  cursor: pointer;
  font-weight: 600;
}

.student-name-link:hover {
  text-decoration: underline;
}

.expand-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.text-btn {
  background: transparent;
  border: none;
  color: #1f4e8c;
  cursor: pointer;
  font-size: 14px;
}

.form-item {
  margin-bottom: 4px;
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

.primary-btn,
.secondary-btn,
.danger-btn,
.close-btn {
  height: 36px;
  min-width: 90px;
  border-radius: 6px;
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

.primary-btn:disabled {
  background: #90a4c3;
  cursor: not-allowed;
}

.secondary-btn,
.close-btn {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  color: #606266;
}

.secondary-btn:hover,
.close-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.danger-btn {
  border: none;
  background: #c45656;
  color: #ffffff;
}

.danger-btn:hover {
  background: #a63f3f;
}

.small-btn {
  min-width: 84px;
  height: 34px;
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

.small-dialog {
  width: 400px;
}

.dialog-header {
  padding: 16px 18px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
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

@media (max-width: 1100px) {
  .class-layout {
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
}
</style>
