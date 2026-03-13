<template>
  <div class="page">
    <AppTopbar
      :logged-in="true"
      user-name="王老师"
      current-role="teacher"
      active-nav="home"
      @platform-click="goTeacherHome"
      @user-click="goTeacherHome"
      @switch-role="switchRole"
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
              <button class="primary-btn small-btn" @click="createClass">创建班级</button>
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
                <div class="class-meta">人数：{{ item.students.length }}</div>
              </div>
            </div>
          </div>

          <div class="class-detail-card" v-if="selectedClass">
            <div class="card-header detail-header">
              <div>
                <div class="card-title">班级详情</div>
                <div class="detail-subtitle">
                  当前班级：{{ selectedClass.name }} | 班级码：{{ selectedClass.code }} | 教师：{{ selectedClass.teacher }}
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
                <tr v-for="stu in displayedStudents" :key="stu.studentId">
                  <td>{{ stu.studentId }}</td>
                  <td>
                    <span class="student-name-link" @click="goStudentDetail(stu)">
                      {{ stu.name }}
                    </span>
                  </td>
                  <td>{{ stu.email }}</td>
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
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'

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
      classList: [
        {
          id: 1,
          name: '人工智能 2201',
          code: 'AI2201',
          teacher: '王老师',
          students: [
            { studentId: '202201001', name: '张三', email: 'zhangsan@example.com' },
            { studentId: '202201002', name: '李四', email: 'lisi@example.com' },
            { studentId: '202201003', name: '王五', email: 'wangwu@example.com' },
            { studentId: '202201004', name: '赵六', email: 'zhaoliu@example.com' }
          ]
        },
        {
          id: 2,
          name: '人工智能 2202',
          code: 'AI2202',
          teacher: '王老师',
          students: [
            { studentId: '202202001', name: '陈一', email: 'chenyi@example.com' },
            { studentId: '202202002', name: '周二', email: 'zhouer@example.com' },
            { studentId: '202202003', name: '吴三', email: 'wusan@example.com' }
          ]
        }
      ],
      selectedClass: null
    }
  },
  computed: {
    displayedStudents () {
      if (!this.selectedClass) return []
      return this.expandAll ? this.selectedClass.students : this.selectedClass.students.slice(0, 3)
    }
  },
  created () {
    if (this.classList.length > 0) {
      this.selectedClass = this.classList[0]
    }
  },
  methods: {
    createClass () {
      const nextIndex = this.classList.length + 1
      const newClass = {
        id: nextIndex,
        name: `新建班级 ${nextIndex}`,
        code: `CLASS${2020 + nextIndex}`,
        teacher: '王老师',
        students: []
      }
      this.classList.push(newClass)
      this.selectedClass = newClass
      this.expandAll = false
    },
    selectClass (item) {
      this.selectedClass = item
      this.expandAll = false
    },
    toggleExpand () {
      this.expandAll = !this.expandAll
    },
    dismissClass () {
      if (!this.selectedClass) return
      const currentId = this.selectedClass.id
      this.classList = this.classList.filter(item => item.id !== currentId)
      this.selectedClass = this.classList.length > 0 ? this.classList[0] : null
      this.expandAll = false
      this.showDismissDialog = false
    },
    goStudentDetail (stu) {
      this.$router.push(`/teacher/student-detail/${stu.studentId}`)
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

.class-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
}

.class-list-card,
.class-detail-card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
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
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.class-item {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
  cursor: pointer;
}

.class-item:hover,
.class-item.active {
  border-color: #1f4e8c;
  background: #ecf5ff;
}

.class-name {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.class-meta {
  font-size: 14px;
  color: #606266;
  margin-bottom: 6px;
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

.student-name-link {
  color: #1f4e8c;
  cursor: pointer;
  font-weight: 600;
}

.student-name-link:hover {
  text-decoration: underline;
}

.expand-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.primary-btn,
.secondary-btn,
.danger-btn,
.text-btn {
  height: 38px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.small-btn {
  min-width: 92px;
  padding: 0 14px;
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
  min-width: 92px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
}

.secondary-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.danger-btn {
  min-width: 92px;
  border: none;
  background: #d9534f;
  color: #ffffff;
}

.danger-btn:hover {
  background: #c9302c;
}

.text-btn {
  min-width: 92px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  padding: 0 14px;
}

.text-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
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
  width: 460px;
  max-width: 100%;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.dialog-header {
  min-height: 56px;
  padding: 0 20px;
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

.close-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
}

.close-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.dialog-body {
  padding: 20px;
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 980px) {
  .layout {
    flex-direction: column;
  }

  .content-area {
    padding: 16px;
  }

  .class-layout {
    grid-template-columns: 1fr;
  }
}
</style>
