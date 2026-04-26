<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='adminName'
      current-role='admin'
      active-nav='home'
      @platform-click='goAdminHome'
      @user-click='goAdminHome'
      @logout='logout'
      @login='goLogin'
      @register='goRegister'
    />

    <div class='layout'>
      <AdminSidebar :active-menu='activeMenu' @change-menu='handleMenuChange' />

      <main class='content-area'>
        <div class='page-header page-header-row'>
          <h2>{{ activeMenu === 'announcement' ? '公告管理' : '说明管理' }}</h2>
          <button class='primary-btn header-btn' type='button' @click='openCreateDialog'>
            {{ activeMenu === 'announcement' ? '发布公告' : '发布说明' }}
          </button>
        </div>

        <div class='card'>
          <div class='card-title'>{{ activeMenu === 'announcement' ? '公告列表' : '说明列表' }}</div>

          <div v-if='loading' class='empty-box'>正在加载数据...</div>
          <div v-else-if='currentItems.length === 0' class='empty-box'>
            当前暂无{{ activeMenu === 'announcement' ? '公告' : '说明' }}。
          </div>
          <div v-else class='table-wrap'>
            <table class='table'>
              <thead>
                <tr v-if='activeMenu === "announcement"'>
                  <th>公告内容</th>
                  <th>发布时间</th>
                  <th>操作</th>
                </tr>
                <tr v-else>
                  <th>标题</th>
                  <th>更新时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <template v-if='activeMenu === "announcement"'>
                  <tr v-for='item in currentItems' :key='`announcement_${item.id}`'>
                    <td class='single-line-cell'>{{ item.content || '--' }}</td>
                    <td>{{ formatDateTime(item.updateTime || item.createTime) }}</td>
                    <td>
                      <button class='mini-btn' type='button' @click='viewAnnouncement(item)'>查看</button>
                      <button class='mini-btn' type='button' @click='editAnnouncement(item)'>修改</button>
                      <button class='mini-btn delete-btn' type='button' @click='deleteAnnouncement(item)'>删除</button>
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for='item in currentItems' :key='`help_${item.id}`'>
                    <td>{{ item.title || '--' }}</td>
                    <td>{{ formatDateTime(item.updateTime || item.createTime) }}</td>
                    <td>
                      <button class='mini-btn' type='button' @click='viewHelp(item)'>查看</button>
                      <button class='mini-btn' type='button' @click='editHelp(item)'>修改</button>
                      <button class='mini-btn delete-btn' type='button' @click='deleteHelp(item)'>删除</button>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>

            <CommonPagination
              v-model:currentPage='currentPage'
              v-model:pageSize='pageSize'
              :total='total'
              :page-size-options='[5, 10, 20]'
              @change='loadCurrentList'
            />
          </div>
        </div>
      </main>
    </div>

    <div v-if='editorVisible' class='dialog-mask' @click.self='closeEditor'>
      <div class='dialog-card admin-dialog-card'>
        <div class='dialog-title'>{{ dialogMode === 'create' ? (activeMenu === 'announcement' ? '发布公告' : '发布说明') : (activeMenu === 'announcement' ? '修改公告' : '修改说明') }}</div>
        <div class='dialog-body'>
          <div v-if='activeMenu === "help"' class='form-item'>
            <label>标题</label>
            <input v-model.trim='editorForm.title' type='text' placeholder='请输入说明标题' />
          </div>
          <div class='form-item'>
            <label>{{ activeMenu === 'announcement' ? '内容' : '内容' }}</label>
            <textarea v-model.trim='editorForm.content' :rows='activeMenu === "announcement" ? 6 : 8' :placeholder='activeMenu === "announcement" ? "请输入公告内容" : "请输入说明内容"' />
          </div>
        </div>
        <div class='dialog-actions'>
          <button class='secondary-btn' type='button' @click='closeEditor'>取消</button>
          <button class='primary-btn' type='button' :disabled='submitting' @click='submitEditor'>{{ submitting ? '提交中...' : '确定' }}</button>
        </div>
      </div>
    </div>

    <div v-if='announcementDetailVisible' class='dialog-mask' @click.self='announcementDetailVisible = false'>
      <div class='dialog-card detail-dialog-card'>
        <div class='dialog-title'>公告详情</div>
        <div class='dialog-body detail-content'>{{ activeAnnouncement.content || '--' }}</div>
        <div class='dialog-actions'>
          <button class='primary-btn' type='button' @click='announcementDetailVisible = false'>关闭</button>
        </div>
      </div>
    </div>

    <div v-if='helpDetailVisible' class='dialog-mask' @click.self='helpDetailVisible = false'>
      <div class='dialog-card detail-dialog-card'>
        <div class='dialog-title'>{{ activeHelp.title || '说明详情' }}</div>
        <div class='dialog-body detail-content help-detail'>
          <div class='help-detail-title'>{{ activeHelp.title || '--' }}</div>
          <div class='help-detail-text'>{{ activeHelp.content || '--' }}</div>
        </div>
        <div class='dialog-actions'>
          <button class='primary-btn' type='button' @click='helpDetailVisible = false'>关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import AdminSidebar from '../components/AdminSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'
import { apiRequest } from '../utils/http'

const API_BASE = (process.env.VUE_APP_API_BASE && process.env.VUE_APP_API_BASE.trim()) ||
  (typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : 'http://localhost:8080')

export default {
  name: 'AdminHomeView',
  components: {
    AppTopbar,
    AdminSidebar,
    CommonPagination
  },
  data () {
    return {
      isLoggedIn: hasAuthToken(),
      adminName: localStorage.getItem('auth_name') || '管理员',
      activeMenu: 'announcement',
      loading: false,
      submitting: false,
      currentPage: 1,
      pageSize: 5,
      total: 0,
      currentItems: [],
      editorVisible: false,
      dialogMode: 'create',
      editTargetId: null,
      editorForm: {
        title: '',
        content: ''
      },
      announcementDetailVisible: false,
      helpDetailVisible: false,
      activeAnnouncement: {},
      activeHelp: {}
    }
  },
  created () {
    this.loadCurrentList()
  },
  methods: {
    goAdminHome () {
      if (this.$route.path !== '/admin/home') {
        this.$router.push('/admin/home')
      }
    },
    goLogin () {
      this.$router.push('/login')
    },
    goRegister () {
      this.$router.push('/register')
    },
    logout () {
      clearAuthState()
      this.$router.push('/login')
    },
    handleMenuChange (menu) {
      if (this.activeMenu === menu) return
      this.activeMenu = menu
      this.currentPage = 1
      this.pageSize = 5
      this.loadCurrentList()
    },
    async loadCurrentList () {
      this.loading = true
      try {
        const endpoint = this.activeMenu === 'announcement' ? 'announcements' : 'help-docs'
        const result = await apiRequest(`${API_BASE}/${endpoint}?pageNum=${this.currentPage - 1}&pageSize=${this.pageSize}`, {
          method: 'GET',
          router: this.$router
        })
        const pageData = result?.data || {}
        this.currentItems = Array.isArray(pageData.content) ? pageData.content : []
        this.total = Number(pageData.totalElements || 0)
      } catch (error) {
        this.currentItems = []
        this.total = 0
        ElMessage.error(error.message || '数据加载失败')
      } finally {
        this.loading = false
      }
    },
    openCreateDialog () {
      this.dialogMode = 'create'
      this.editTargetId = null
      this.editorForm = { title: '', content: '' }
      this.editorVisible = true
    },
    editAnnouncement (item) {
      this.dialogMode = 'edit'
      this.editTargetId = item.id
      this.editorForm = { title: '', content: item.content || '' }
      this.editorVisible = true
    },
    editHelp (item) {
      this.dialogMode = 'edit'
      this.editTargetId = item.id
      this.editorForm = { title: item.title || '', content: item.content || '' }
      this.editorVisible = true
    },
    viewAnnouncement (item) {
      this.activeAnnouncement = { ...item }
      this.announcementDetailVisible = true
    },
    viewHelp (item) {
      this.activeHelp = { ...item }
      this.helpDetailVisible = true
    },
    closeEditor () {
      this.editorVisible = false
      this.editorForm = { title: '', content: '' }
      this.editTargetId = null
    },
    async submitEditor () {
      if (this.activeMenu === 'announcement') {
        if (!this.editorForm.content) {
          ElMessage.warning('请输入公告内容')
          return
        }
      } else {
        if (!this.editorForm.title) {
          ElMessage.warning('请输入说明标题')
          return
        }
        if (!this.editorForm.content) {
          ElMessage.warning('请输入说明内容')
          return
        }
      }

      this.submitting = true
      try {
        const isCreate = this.dialogMode === 'create'
        const endpoint = this.activeMenu === 'announcement'
          ? `${API_BASE}/announcements${isCreate ? '' : `/${this.editTargetId}`}`
          : `${API_BASE}/help-docs${isCreate ? '' : `/${this.editTargetId}`}`
        const payload = this.activeMenu === 'announcement'
          ? { content: this.editorForm.content }
          : { title: this.editorForm.title, content: this.editorForm.content }

        await apiRequest(endpoint, {
          method: isCreate ? 'POST' : 'PATCH',
          router: this.$router,
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        })
        ElMessage.success(isCreate ? '发布成功' : '修改成功')
        this.closeEditor()
        await this.loadCurrentList()
      } catch (error) {
        ElMessage.error(error.message || '提交失败')
      } finally {
        this.submitting = false
      }
    },
    async deleteAnnouncement (item) {
      try {
        await ElMessageBox.confirm('确认删除该公告吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await apiRequest(`${API_BASE}/announcements/${item.id}`, {
          method: 'DELETE',
          router: this.$router
        })
        ElMessage.success('删除成功')
        await this.loadCurrentList()
      } catch (error) {
        if (error === 'cancel' || error === 'close') return
        ElMessage.error(error.message || '删除失败')
      }
    },
    async deleteHelp (item) {
      try {
        await ElMessageBox.confirm('确认删除该说明吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await apiRequest(`${API_BASE}/help-docs/${item.id}`, {
          method: 'DELETE',
          router: this.$router
        })
        ElMessage.success('删除成功')
        await this.loadCurrentList()
      } catch (error) {
        if (error === 'cancel' || error === 'close') return
        ElMessage.error(error.message || '删除失败')
      }
    },
    formatDateTime (value) {
      if (!value) return '--'
      return String(value).replace('T', ' ').slice(0, 19)
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
}

.layout {
  display: flex;
  min-height: calc(100vh - 64px);
}

.content-area {
  flex: 1;
  padding: 24px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #1f2d3d;
}

.card {
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, 0.06);
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.empty-box {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  color: #909399;
  background: #fafafa;
}

.table-wrap {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 12px;
  font-size: 14px;
  color: #303133;
  text-align: left;
  vertical-align: middle;
}

.table th {
  background: #f8f9fb;
  color: #606266;
  font-weight: 600;
}

.single-line-cell {
  max-width: 520px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.primary-btn,
.secondary-btn,
.mini-btn,
.delete-btn {
  height: 34px;
  padding: 0 14px;
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

.secondary-btn,
.mini-btn {
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
}

.secondary-btn:hover,
.mini-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.delete-btn {
  border: 1px solid #f56c6c;
  background: #fff5f5;
  color: #f56c6c;
}

.delete-btn:hover {
  background: #f56c6c;
  color: #ffffff;
}

.header-btn {
  min-width: 96px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.dialog-card {
  width: 700px;
  max-width: calc(100vw - 32px);
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
  padding: 20px;
}

.admin-dialog-card {
  width: 760px;
}

.detail-dialog-card {
  width: 760px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.dialog-body {
  margin-bottom: 18px;
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

.form-item input,
.form-item textarea {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  color: #303133;
  resize: vertical;
  outline: none;
}

.form-item input:focus,
.form-item textarea:focus {
  border-color: #1f4e8c;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
  white-space: pre-wrap;
  line-height: 1.8;
  color: #303133;
}

.help-detail {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.help-detail-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 12px;
}

.help-detail-text {
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
