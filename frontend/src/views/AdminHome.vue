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
          <h2>{{ pageTitle }}</h2>
          <button v-if='activeMenu !== "user"' class='primary-btn header-btn' type='button' @click='openCreateDialog'>
            {{ activeMenu === 'announcement' ? '发布公告' : '发布说明' }}
          </button>
        </div>

        <template v-if='activeMenu === "user"'>
          <div class='card filter-card'>
            <div class='card-title'>用户筛选</div>
            <div class='filter-row'>
              <div class='filter-item'>
                <label>关键词</label>
                <input
                  v-model.trim='userQuery.keyword'
                  type='text'
                  placeholder='请输入用户名、昵称或邮箱'
                  @keyup.enter='searchUsers'
                />
              </div>
              <div class='filter-item'>
                <label>角色</label>
                <select v-model='userQuery.role'>
                  <option value=''>全部角色</option>
                  <option value='STUDENT'>学生</option>
                  <option value='TEACHER'>教师</option>
                  <option value='ADMIN'>管理员</option>
                </select>
              </div>
              <div class='filter-item'>
                <label>账号状态</label>
                <select v-model='userQuery.isDeleted'>
                  <option value='false'>正常</option>
                  <option value='true'>已删除</option>
                  <option value=''>全部</option>
                </select>
              </div>
              <div class='filter-actions'>
                <button class='primary-btn' type='button' @click='searchUsers'>查询</button>
                <button class='secondary-btn' type='button' @click='resetUserQuery'>重置</button>
              </div>
            </div>
          </div>
        </template>

        <div class='card'>
          <div class='card-title'>{{ cardTitle }}</div>

          <div v-if='loading' class='empty-box'>正在加载数据...</div>
          <div v-else-if='currentItems.length === 0' class='empty-box'>
            当前暂无{{ emptyText }}。
          </div>
          <div v-else class='table-wrap'>
            <table class='table'>
              <thead>
                <tr v-if='activeMenu === "announcement"'>
                  <th>公告内容</th>
                  <th>发布时间</th>
                  <th>操作</th>
                </tr>
                <tr v-else-if='activeMenu === "help"'>
                  <th>标题</th>
                  <th>更新时间</th>
                  <th>操作</th>
                </tr>
                <tr v-else>
                  <th>用户名</th>
                  <th>邮箱</th>
                  <th>角色</th>
                  <th>所属班级</th>
                  <th>账号状态</th>
                  <th>创建时间</th>
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
                <template v-else-if='activeMenu === "help"'>
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
                <template v-else>
                  <tr v-for='item in currentItems' :key='`user_${item.id}`'>
                    <td>{{ item.username || '--' }}</td>
                    <td>{{ item.email || '--' }}</td>
                    <td>
                      <span class='role-tag' :class='roleClass(item.role)'>{{ roleText(item.role) }}</span>
                    </td>
                    <td>{{ className(item) }}</td>
                    <td>
                      <span class='status-tag' :class='{ deleted: item.isDeleted }'>{{ item.isDeleted ? '已删除' : '正常' }}</span>
                    </td>
                    <td>{{ formatDateTime(item.createTime) }}</td>
                    <td>
                      <button class='mini-btn' type='button' @click='viewUser(item)'>查看</button>
                      <button
                        class='mini-btn'
                        type='button'
                        :disabled='item.isDeleted'
                        @click='openRoleDialog(item)'
                      >
                        修改角色
                      </button>
                      <button
                        class='mini-btn delete-btn'
                        type='button'
                        :disabled='item.isDeleted'
                        @click='deleteUser(item)'
                      >
                        删除
                      </button>
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
            <label>内容</label>
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

    <div v-if='userDetailVisible' class='dialog-mask' @click.self='userDetailVisible = false'>
      <div class='dialog-card detail-dialog-card'>
        <div class='dialog-title'>用户信息</div>
        <div class='dialog-body detail-grid'>
          <div class='detail-item'>
            <span>用户名</span>
            <strong>{{ activeUser.username || '--' }}</strong>
          </div>
          <div class='detail-item'>
            <span>昵称</span>
            <strong>{{ activeUser.nickname || '--' }}</strong>
          </div>
          <div class='detail-item'>
            <span>邮箱</span>
            <strong>{{ activeUser.email || '--' }}</strong>
          </div>
          <div class='detail-item'>
            <span>角色</span>
            <strong>{{ roleText(activeUser.role) }}</strong>
          </div>
          <div class='detail-item'>
            <span>所属班级</span>
            <strong>{{ className(activeUser) }}</strong>
          </div>
          <div class='detail-item'>
            <span>账号状态</span>
            <strong>{{ activeUser.isDeleted ? '已删除' : '正常' }}</strong>
          </div>
          <div class='detail-item'>
            <span>创建时间</span>
            <strong>{{ formatDateTime(activeUser.createTime) }}</strong>
          </div>
          <div class='detail-item'>
            <span>更新时间</span>
            <strong>{{ formatDateTime(activeUser.updateTime) }}</strong>
          </div>
        </div>
        <div class='dialog-actions'>
          <button class='primary-btn' type='button' @click='userDetailVisible = false'>关闭</button>
        </div>
      </div>
    </div>

    <div v-if='roleDialogVisible' class='dialog-mask' @click.self='roleDialogVisible = false'>
      <div class='dialog-card role-dialog-card'>
        <div class='dialog-title'>修改用户角色</div>
        <div class='dialog-body'>
          <div class='form-item readonly-item'>
            <label>用户</label>
            <div class='readonly-box'>{{ roleForm.username || '--' }}（{{ roleForm.email || '--' }}）</div>
          </div>
          <div class='form-item'>
            <label>角色</label>
            <select v-model='roleForm.role'>
              <option value='STUDENT'>学生</option>
              <option value='TEACHER'>教师</option>
              <option value='ADMIN'>管理员</option>
            </select>
          </div>
          <div class='notice-box'>修改角色会影响该用户后续登录后的页面入口和系统权限，请确认后再提交。</div>
        </div>
        <div class='dialog-actions'>
          <button class='secondary-btn' type='button' @click='roleDialogVisible = false'>取消</button>
          <button class='primary-btn' type='button' :disabled='submitting' @click='submitRoleChange'>{{ submitting ? '提交中...' : '确定' }}</button>
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
import { apiRequest, getApiBaseUrl } from '../utils/http'

const API_BASE = getApiBaseUrl()

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
      userQuery: {
        keyword: '',
        role: '',
        isDeleted: 'false'
      },
      announcementDetailVisible: false,
      helpDetailVisible: false,
      userDetailVisible: false,
      roleDialogVisible: false,
      activeAnnouncement: {},
      activeHelp: {},
      activeUser: {},
      roleForm: {
        id: null,
        username: '',
        email: '',
        role: 'STUDENT'
      }
    }
  },
  computed: {
    pageTitle () {
      if (this.activeMenu === 'announcement') return '公告管理'
      if (this.activeMenu === 'help') return '说明管理'
      return '用户管理'
    },
    cardTitle () {
      if (this.activeMenu === 'announcement') return '公告列表'
      if (this.activeMenu === 'help') return '说明列表'
      return '用户列表'
    },
    emptyText () {
      if (this.activeMenu === 'announcement') return '公告'
      if (this.activeMenu === 'help') return '说明'
      return '用户'
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
        if (this.activeMenu === 'user') {
          await this.loadUserList()
          return
        }

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
    async loadUserList () {
      const params = new URLSearchParams()
      params.append('pageNum', String(this.currentPage - 1))
      params.append('pageSize', String(this.pageSize))
      if (this.userQuery.role) params.append('role', this.userQuery.role)
      if (this.userQuery.keyword) params.append('keyword', this.userQuery.keyword)
      if (this.userQuery.isDeleted !== '') params.append('isDeleted', this.userQuery.isDeleted)

      const result = await apiRequest(`${API_BASE}/user/list?${params.toString()}`, {
        method: 'GET',
        router: this.$router
      })
      const pageData = result?.data || {}
      this.currentItems = Array.isArray(pageData.content) ? pageData.content : []
      this.total = Number(pageData.totalElements || 0)
    },
    searchUsers () {
      this.currentPage = 1
      this.loadCurrentList()
    },
    resetUserQuery () {
      this.userQuery = {
        keyword: '',
        role: '',
        isDeleted: 'false'
      }
      this.currentPage = 1
      this.loadCurrentList()
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
    viewUser (item) {
      this.activeUser = { ...item }
      this.userDetailVisible = true
    },
    openRoleDialog (item) {
      this.roleForm = {
        id: item.id,
        username: item.username || '',
        email: item.email || '',
        role: item.role || 'STUDENT'
      }
      this.roleDialogVisible = true
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
    async submitRoleChange () {
      if (!this.roleForm.id || !this.roleForm.role) {
        ElMessage.warning('请选择需要修改的用户角色')
        return
      }

      this.submitting = true
      try {
        await apiRequest(`${API_BASE}/user/${this.roleForm.id}/role?role=${encodeURIComponent(this.roleForm.role)}`, {
          method: 'PATCH',
          router: this.$router
        })
        ElMessage.success('角色修改成功')
        this.roleDialogVisible = false
        await this.loadCurrentList()
      } catch (error) {
        ElMessage.error(error.message || '角色修改失败')
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
    async deleteUser (item) {
      try {
        await ElMessageBox.confirm(`确认删除用户“${item.username || item.email || item.id}”吗？删除后该账号将无法正常登录，历史提交和排行榜关联数据不会被物理清除。`, '删除确认', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await apiRequest(`${API_BASE}/user/${item.id}`, {
          method: 'DELETE',
          router: this.$router
        })
        ElMessage.success('用户删除成功')
        await this.loadCurrentList()
      } catch (error) {
        if (error === 'cancel' || error === 'close') return
        ElMessage.error(error.message || '删除失败')
      }
    },
    roleText (role) {
      const map = {
        STUDENT: '学生',
        TEACHER: '教师',
        ADMIN: '管理员'
      }
      return map[role] || role || '--'
    },
    roleClass (role) {
      if (role === 'ADMIN') return 'admin'
      if (role === 'TEACHER') return 'teacher'
      return 'student'
    },
    className (user) {
      return user?.studentClass?.name || '--'
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

.filter-card {
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 14px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 180px;
}

.filter-item:first-child {
  min-width: 260px;
}

.filter-item label {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.filter-item input,
.filter-item select,
.form-item select {
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 14px;
  color: #303133;
  background: #ffffff;
  outline: none;
}

.filter-item input:focus,
.filter-item select:focus,
.form-item select:focus {
  border-color: #1f4e8c;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.mini-btn:disabled,
.delete-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.delete-btn {
  border: 1px solid #f56c6c;
  background: #fff5f5;
  color: #f56c6c;
}

.delete-btn:hover:not(:disabled) {
  background: #f56c6c;
  color: #ffffff;
}

.header-btn {
  min-width: 96px;
}

.role-tag,
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 24px;
  padding: 0 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.role-tag.student {
  background: #eef6ff;
  color: #1f4e8c;
}

.role-tag.teacher {
  background: #f0f9eb;
  color: #3f7f2f;
}

.role-tag.admin {
  background: #fdf6ec;
  color: #a86613;
}

.status-tag {
  background: #f0f9eb;
  color: #3f7f2f;
}

.status-tag.deleted {
  background: #f4f4f5;
  color: #909399;
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

.admin-dialog-card,
.detail-dialog-card {
  width: 760px;
}

.role-dialog-card {
  width: 560px;
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
.form-item textarea,
.form-item select {
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

.readonly-box {
  min-height: 36px;
  display: flex;
  align-items: center;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 0 12px;
  background: #f8f9fb;
  color: #606266;
  font-size: 14px;
}

.notice-box {
  border: 1px solid #ebeef5;
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  padding: 12px;
}

.detail-item span {
  display: block;
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}

.detail-item strong {
  display: block;
  color: #303133;
  font-size: 14px;
  word-break: break-all;
}
</style>
