<template>
  <div class='page'>
    <AppTopbar
      :logged-in='isLoggedIn'
      :user-name='teacherName'
      current-role='teacher'
      active-nav='home'
      @platform-click='goTaskHall'
      @user-click='goTaskHall'
      @switch-role='switchRole'
      @logout='logout'
    />

    <div class='layout'>
      <TeacherSidebar active-menu='environment-manage' />

      <main class='content-area'>
        <div class='page-header page-header-row'>
          <h2>环境管理</h2>
          <button class='primary-btn header-btn' type='button' @click='openUploadDialog'>文件上传</button>
        </div>

        <div class='card'>
          <div class='card-title'>新建多人对战环境</div>

          <div class='form-grid'>
            <div class='form-item'>
              <label>环境名称</label>
              <input v-model='createForm.name' type='text' placeholder='例如：PettingZoo Boxing 对战环境' />
            </div>

            <div class='form-item'>
              <label>环境编码</label>
              <input v-model='createForm.code' type='text' placeholder='例如：boxing_v2' />
            </div>

            <div class='form-item'>
              <label>是否启用 GPU</label>
              <select v-model='createForm.isGpu'>
                <option :value='false'>否</option>
                <option :value='true'>是</option>
              </select>
            </div>

            <div class='form-item'>
              <label>CUDA 设备号</label>
              <input v-model='createForm.cudaDevice' type='text' placeholder='如 0，可留空' />
            </div>

            <div class='form-item full-width'>
              <label>requirements.txt</label>
              <div class='upload-row'>
                <button class='upload-btn' type='button' @click='triggerFileInput("requirementsInput")'>选择 requirements.txt</button>
                <span class='upload-text'>{{ requirementsFileName }}</span>
                <input
                  ref='requirementsInput'
                  class='hidden-file-input'
                  type='file'
                  accept='.txt'
                  @change='handleFileChange($event, "requirements")'
                />
              </div>
            </div>

            <div class='form-item full-width'>
              <label>对战测评脚本</label>
              <div class='upload-row'>
                <button class='upload-btn' type='button' @click='triggerFileInput("scriptInput")'>选择 Python 脚本</button>
                <span class='upload-text'>{{ scriptFileName }}</span>
                <input
                  ref='scriptInput'
                  class='hidden-file-input'
                  type='file'
                  accept='.py'
                  @change='handleFileChange($event, "script")'
                />
              </div>
            </div>
          </div>

          <div class='action-bar'>
            <button class='primary-btn' :disabled='submitting' @click='handleCreate'>{{ submitting ? '创建中...' : '创建环境' }}</button>
          </div>
        </div>

        <div class='card section-space'>
          <div class='card-title'>环境列表</div>

          <div v-if='loading' class='empty-box'>正在加载环境列表...</div>
          <div v-else-if='envList.length === 0' class='empty-box'>当前暂无已创建环境。</div>
          <div v-else class='table-wrap'>
            <table class='table'>
              <thead>
                <tr>
                  <th>环境名称</th>
                  <th>环境编码</th>
                  <th>状态</th>
                  <th>Conda 环境名</th>
                  <th>Python 路径</th>
                  <th>GPU</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in pagedEnvList' :key='item.id'>
                  <td>{{ item.name }}</td>
                  <td>{{ item.code }}</td>
                  <td>
                    <span class='status-tag' :class='statusClass(item.status)'>{{ formatStatus(item.status) }}</span>
                  </td>
                  <td>{{ item.condaEnvName || '--' }}</td>
                  <td class='path-cell'>{{ item.pythonPath || '--' }}</td>
                  <td>{{ item.isGpu ? `是${item.cudaDevice ? `（${item.cudaDevice}）` : ''}` : '否' }}</td>
                  <td>
                    <button class='mini-btn' type='button' @click='showLog(item)'>查看日志</button>

                    <button
                      v-if='canDisable(item)'
                      class='mini-btn danger-btn'
                      type='button'
                      @click='disableEnvironment(item)'
                    >停用</button>

                    <button
                      v-if='canDelete(item)'
                      class='mini-btn delete-btn'
                      type='button'
                      @click='deleteEnvironment(item)'
                    >删除</button>
                  </td>
                </tr>
              </tbody>
            </table>

            <CommonPagination
              v-model:currentPage='envCurrentPage'
              v-model:pageSize='envPageSize'
              :total='envList.length'
              :page-size-options='[5, 10, 20]'
            />
          </div>
        </div>

        <div class='card section-space'>
          <div class='card-title'>文件列表</div>

          <div v-if='resourceLoading' class='empty-box'>正在加载文件列表...</div>
          <div v-else-if='resourceList.length === 0' class='empty-box'>当前暂无教师上传文件。</div>
          <div v-else class='table-wrap'>
            <table class='table'>
              <thead>
                <tr>
                  <th>名称</th>
                  <th>文件名</th>
                  <th>状态</th>
                  <th>上传时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for='item in pagedResourceList' :key='item.id'>
                  <td>{{ item.name }}</td>
                  <td class='path-cell'>{{ item.originalFilename || '--' }}</td>
                  <td>
                    <span class='status-tag' :class='statusClass(item.status)'>{{ formatStatus(item.status) }}</span>
                  </td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td>
                    <button
                      v-if='item.status === "SUCCESS" && !item.isDeleted'
                      class='mini-btn'
                      type='button'
                      @click='downloadResource(item)'
                    >下载</button>
                    <button
                      v-if='item.status !== "DELETED"'
                      class='mini-btn delete-btn'
                      type='button'
                      @click='deleteResource(item)'
                    >删除</button>
                  </td>
                </tr>
              </tbody>
            </table>

            <CommonPagination
              v-model:currentPage='resourceCurrentPage'
              v-model:pageSize='resourcePageSize'
              :total='resourceList.length'
              :page-size-options='[5, 10, 20]'
            />
          </div>
        </div>

        <div v-if='logDialogVisible' class='dialog-mask' @click.self='closeLogDialog'>
          <div class='dialog-card'>
            <div class='dialog-title'>安装日志 - {{ activeLogTitle }}</div>
            <pre class='log-box'>{{ activeLogContent }}</pre>
            <div class='dialog-actions'>
              <button class='primary-btn' type='button' @click='closeLogDialog'>关闭</button>
            </div>
          </div>
        </div>

        <div v-if='uploadDialogVisible' class='dialog-mask' @click.self='closeUploadDialog'>
          <div class='dialog-card upload-dialog-card'>
            <div class='dialog-title'>上传文件</div>
            <div class='dialog-body'>
              <div class='form-item'>
                <label>名称</label>
                <input v-model='uploadForm.name' type='text' placeholder='请输入文件名称' />
              </div>

              <div class='form-item'>
                <label>文件</label>
                <div class='selected-file-list' v-if='uploadForm.files.length > 0'>
                  <div class='selected-file-item' v-for='(file, index) in uploadForm.files' :key='`${file.name}_${index}`'>
                    <span class='selected-file-name'>{{ file.name }}</span>
                    <button class='text-delete-btn' type='button' @click='removeSelectedFile(index)'>删除</button>
                  </div>
                </div>
                <div v-else class='empty-file-tip'>当前未选择文件</div>
                <input
                  ref='extraFileInput'
                  class='hidden-file-input'
                  type='file'
                  multiple
                  @change='handleExtraFilesChange'
                />
              </div>
            </div>
            <div class='dialog-actions'>
              <button class='upload-btn' type='button' @click='triggerFileInput("extraFileInput")'>新增文件</button>
              <button class='primary-btn' type='button' :disabled='uploadSubmitting' @click='submitResourceUpload'>{{ uploadSubmitting ? '上传中...' : '上传' }}</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { getApiBaseUrl } from '../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTopbar from '../components/AppTopbar.vue'
import TeacherSidebar from '../components/TeacherSidebar.vue'
import CommonPagination from '../components/CommonPagination.vue'
import { clearAuthState, hasAuthToken } from '../utils/auth'

const API_BASE = getApiBaseUrl()

export default {
  name: 'TeacherEnvironmentManageView',
  components: {
    AppTopbar,
    TeacherSidebar,
    CommonPagination
  },
  data () {
    return {
      teacherName: localStorage.getItem('auth_name') || '教师',
      loading: false,
      resourceLoading: false,
      submitting: false,
      uploadSubmitting: false,
      envList: [],
      resourceList: [],
      envCurrentPage: 1,
      envPageSize: 5,
      resourceCurrentPage: 1,
      resourcePageSize: 5,
      requirementsFile: null,
      scriptFile: null,
      requirementsFileName: '当前未选择文件',
      scriptFileName: '当前未选择文件',
      createForm: {
        name: '',
        code: '',
        isGpu: false,
        cudaDevice: ''
      },
      logDialogVisible: false,
      activeLogTitle: '',
      activeLogContent: '',
      uploadDialogVisible: false,
      uploadForm: {
        name: '',
        files: []
      }
    }
  },
  computed: {
    isLoggedIn () {
      return hasAuthToken()
    },
    pagedEnvList () {
      const start = (this.envCurrentPage - 1) * this.envPageSize
      return this.envList.slice(start, start + this.envPageSize)
    },
    pagedResourceList () {
      const start = (this.resourceCurrentPage - 1) * this.resourcePageSize
      return this.resourceList.slice(start, start + this.resourcePageSize)
    }
  },
  created () {
    this.loadAllData()
  },
  methods: {
    tokenHeader () {
      const token = localStorage.getItem('auth_token')
      return token ? { Authorization: `Bearer ${token}` } : {}
    },
    async loadAllData () {
      await Promise.all([this.loadEnvironmentList(), this.loadResourceList()])
    },
    async loadEnvironmentList () {
      this.loading = true
      try {
        const response = await fetch(`${API_BASE}/battle-environments`, {
          method: 'GET',
          headers: this.tokenHeader()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '环境列表加载失败')
        }
        this.envList = Array.isArray(result.data) ? result.data : []
        if ((this.envCurrentPage - 1) * this.envPageSize >= this.envList.length && this.envCurrentPage > 1) {
          this.envCurrentPage = Math.max(1, Math.ceil(this.envList.length / this.envPageSize))
        }
      } catch (error) {
        this.envList = []
        ElMessage.error(error.message || '环境列表加载失败')
      } finally {
        this.loading = false
      }
    },
    async loadResourceList () {
      this.resourceLoading = true
      try {
        const response = await fetch(`${API_BASE}/battle-environments/resources`, {
          method: 'GET',
          headers: this.tokenHeader()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '文件列表加载失败')
        }
        this.resourceList = Array.isArray(result.data) ? result.data : []
        if ((this.resourceCurrentPage - 1) * this.resourcePageSize >= this.resourceList.length && this.resourceCurrentPage > 1) {
          this.resourceCurrentPage = Math.max(1, Math.ceil(this.resourceList.length / this.resourcePageSize))
        }
      } catch (error) {
        this.resourceList = []
        ElMessage.error(error.message || '文件列表加载失败')
      } finally {
        this.resourceLoading = false
      }
    },
    triggerFileInput (refName) {
      const input = this.$refs[refName]
      if (input) {
        input.click()
      }
    },
    handleFileChange (event, type) {
      const file = event.target.files && event.target.files[0]
      if (type === 'requirements') {
        this.requirementsFile = file || null
        this.requirementsFileName = file ? file.name : '当前未选择文件'
      } else {
        this.scriptFile = file || null
        this.scriptFileName = file ? file.name : '当前未选择文件'
      }
    },
    handleExtraFilesChange (event) {
      const files = Array.from((event.target && event.target.files) || [])
      if (files.length === 0) {
        return
      }
      this.uploadForm.files = [...this.uploadForm.files, ...files]
      if (this.$refs.extraFileInput) {
        this.$refs.extraFileInput.value = ''
      }
    },
    removeSelectedFile (index) {
      this.uploadForm.files.splice(index, 1)
    },
    validateCreateForm () {
      if (!this.createForm.name.trim()) {
        ElMessage.warning('请输入环境名称')
        return false
      }
      if (!this.createForm.code.trim()) {
        ElMessage.warning('请输入环境编码')
        return false
      }
      if (!this.requirementsFile) {
        ElMessage.warning('请上传 requirements.txt')
        return false
      }
      if (!this.scriptFile) {
        ElMessage.warning('请上传对战测评脚本')
        return false
      }
      return true
    },
    async handleCreate () {
      if (!this.validateCreateForm()) {
        return
      }
      this.submitting = true
      try {
        const formData = new FormData()
        formData.append('name', this.createForm.name.trim())
        formData.append('code', this.createForm.code.trim())
        formData.append('isGpu', this.createForm.isGpu)
        formData.append('cudaDevice', this.createForm.cudaDevice || '')
        formData.append('requirements', this.requirementsFile)
        formData.append('script', this.scriptFile)

        const response = await fetch(`${API_BASE}/battle-environments`, {
          method: 'POST',
          headers: this.tokenHeader(),
          body: formData
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '环境创建失败')
        }

        ElMessage.success('环境创建请求已提交，可在列表中查看安装状态')
        this.resetCreateForm()
        await this.loadEnvironmentList()
      } catch (error) {
        ElMessage.error(error.message || '环境创建失败')
      } finally {
        this.submitting = false
      }
    },
    resetCreateForm () {
      this.createForm = {
        name: '',
        code: '',
        isGpu: false,
        cudaDevice: ''
      }
      this.requirementsFile = null
      this.scriptFile = null
      this.requirementsFileName = '当前未选择文件'
      this.scriptFileName = '当前未选择文件'
      if (this.$refs.requirementsInput) {
        this.$refs.requirementsInput.value = ''
      }
      if (this.$refs.scriptInput) {
        this.$refs.scriptInput.value = ''
      }
    },
    openUploadDialog () {
      this.uploadDialogVisible = true
    },
    closeUploadDialog () {
      this.uploadDialogVisible = false
      this.uploadForm = {
        name: '',
        files: []
      }
      if (this.$refs.extraFileInput) {
        this.$refs.extraFileInput.value = ''
      }
    },
    async submitResourceUpload () {
      if (!this.uploadForm.name.trim()) {
        ElMessage.warning('请输入名称')
        return
      }
      if (this.uploadForm.files.length === 0) {
        ElMessage.warning('请至少选择一个文件')
        return
      }
      this.uploadSubmitting = true
      try {
        const formData = new FormData()
        formData.append('name', this.uploadForm.name.trim())
        this.uploadForm.files.forEach(file => formData.append('files', file))
        const response = await fetch(`${API_BASE}/battle-environments/resources/upload`, {
          method: 'POST',
          headers: this.tokenHeader(),
          body: formData
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '文件上传失败')
        }
        ElMessage.success('文件上传成功')
        this.closeUploadDialog()
        await this.loadResourceList()
      } catch (error) {
        ElMessage.error(error.message || '文件上传失败')
      } finally {
        this.uploadSubmitting = false
      }
    },
    showLog (item) {
      this.activeLogTitle = item.name || item.code
      this.activeLogContent = item.installLog || '暂无日志'
      this.logDialogVisible = true
    },
    closeLogDialog () {
      this.logDialogVisible = false
      this.activeLogTitle = ''
      this.activeLogContent = ''
    },
    canDisable (item) {
      return item && item.status === 'READY'
    },
    canDelete (item) {
      if (!item) {
        return false
      }
      return item.status === 'READY' || item.status === 'DISABLED' || item.status === 'FAILED'
    },
    async disableEnvironment (item) {
      try {
        await ElMessageBox.confirm(`确认停用环境“${item.name || item.code}”吗？`, '提示', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await fetch(`${API_BASE}/battle-environments/${item.id}/disable`, {
          method: 'POST',
          headers: this.tokenHeader()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '停用失败')
        }
        ElMessage.success('环境已停用')
        await this.loadEnvironmentList()
      } catch (error) {
        if (error === 'cancel' || error === 'close') {
          return
        }
        ElMessage.error(error.message || '停用失败')
      }
    },
    async deleteEnvironment (item) {
      try {
        await ElMessageBox.confirm(
          `确认删除环境“${item.name || item.code}”吗？这将删除其 Conda 环境及已上传文件。`,
          '第一次确认',
          {
            confirmButtonText: '继续删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        await ElMessageBox.confirm(
          '请再次确认：删除后该环境将变为“已删除”状态，且不再可用。',
          '第二次确认',
          {
            confirmButtonText: '确认删除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await fetch(`${API_BASE}/battle-environments/${item.id}`, {
          method: 'DELETE',
          headers: this.tokenHeader()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '删除失败')
        }
        ElMessage.success('环境已删除')
        await this.loadEnvironmentList()
      } catch (error) {
        if (error === 'cancel' || error === 'close') {
          return
        }
        ElMessage.error(error.message || '删除失败')
      }
    },
    async deleteResource (item) {
      try {
        await ElMessageBox.confirm(`确认删除文件“${item.originalFilename || item.name}”吗？删除后学生端将不再显示。`, '提示', {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await fetch(`${API_BASE}/battle-environments/resources/${item.id}`, {
          method: 'DELETE',
          headers: this.tokenHeader()
        })
        const result = await response.json()
        if (!response.ok || result.code !== 0) {
          throw new Error(result.message || '删除失败')
        }
        ElMessage.success('文件已删除')
        await this.loadResourceList()
      } catch (error) {
        if (error === 'cancel' || error === 'close') {
          return
        }
        ElMessage.error(error.message || '删除失败')
      }
    },
    async downloadResource (item) {
      try {
        const response = await fetch(`${API_BASE}/battle-environments/resources/${item.id}/download`, {
          method: 'GET',
          headers: this.tokenHeader()
        })
        if (!response.ok) {
          let message = '文件下载失败'
          try {
            const result = await response.json()
            message = result.message || message
          } catch (_) {}
          throw new Error(message)
        }
        const blob = await response.blob()
        this.triggerBrowserDownload(blob, item.originalFilename || 'resource')
      } catch (error) {
        ElMessage.error(error.message || '文件下载失败')
      }
    },
    triggerBrowserDownload (blob, filename) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },
    formatStatus (status) {
      const map = {
        CREATING: '创建中',
        READY: '可用',
        FAILED: '失败',
        DISABLED: '已停用',
        DELETED: '已删除',
        SUCCESS: '成功'
      }
      return map[status] || status || '--'
    },
    statusClass (status) {
      return {
        creating: status === 'CREATING',
        ready: status === 'READY' || status === 'SUCCESS',
        failed: status === 'FAILED',
        disabled: status === 'DISABLED',
        deleted: status === 'DELETED'
      }
    },
    formatDateTime (value) {
      if (!value) {
        return '--'
      }
      const text = String(value).replace('T', ' ')
      return text.length > 19 ? text.slice(0, 19) : text
    },
    goTaskHall () {
      this.$router.push('/teacher/hall')
    },
    switchRole () {
      sessionStorage.removeItem('mock_logged_out_view')
      localStorage.setItem('mock_login_role', 'student')
      this.$router.push({ path: '/', query: { tab: 'open' } })
    },
    logout () {
      clearAuthState()
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

.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
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
  margin-top: 18px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1f2d3d;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
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
  font-weight: 700;
  color: #303133;
}

.form-item input,
.form-item select {
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 14px;
  color: #303133;
  outline: none;
  background: #fff;
}

.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hidden-file-input {
  display: none;
}

.upload-btn,
.primary-btn,
.mini-btn {
  border: 1px solid #1F4E8C;
  background: #1F4E8C;
  color: #fff;
  border-radius: 6px;
  cursor: pointer;
}

.upload-btn,
.primary-btn {
  height: 40px;
  padding: 0 18px;
}

.header-btn {
  min-width: 108px;
}

.mini-btn {
  height: 32px;
  padding: 0 12px;
  margin-right: 8px;
}

.danger-btn {
  background: #f56c6c;
  border-color: #f56c6c;
}

.delete-btn {
  background: #909399;
  border-color: #909399;
}

.action-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.empty-box {
  padding: 28px 0;
  text-align: center;
  color: #909399;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  border-bottom: 1px solid #ebeef5;
  padding: 12px;
  text-align: left;
  font-size: 14px;
  color: #303133;
  vertical-align: middle;
}

.table th {
  background: #f8fafc;
  color: #606266;
  font-weight: 700;
}

.path-cell {
  max-width: 260px;
  word-break: break-all;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 28px;
  padding: 0 10px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 700;
  background: #eef2f7;
  color: #606266;
}

.status-tag.creating {
  background: #ecf5ff;
  color: #409eff;
}

.status-tag.ready {
  background: #f0f9eb;
  color: #67c23a;
}

.status-tag.failed {
  background: #fef0f0;
  color: #f56c6c;
}

.status-tag.disabled,
.status-tag.deleted {
  background: #f4f4f5;
  color: #909399;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.dialog-card {
  width: min(720px, 100%);
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
  padding: 20px;
}

.upload-dialog-card {
  width: min(640px, 100%);
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 16px;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.log-box {
  max-height: 420px;
  overflow: auto;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  line-height: 1.55;
  font-size: 13px;
}

.selected-file-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafbfc;
}

.selected-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e5e7eb;
}

.selected-file-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.selected-file-name {
  word-break: break-all;
  font-size: 14px;
  color: #303133;
}

.text-delete-btn {
  border: none;
  background: transparent;
  color: #f56c6c;
  cursor: pointer;
  font-size: 13px;
}

.empty-file-tip,
.upload-text {
  color: #909399;
  font-size: 13px;
}

@media (max-width: 960px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
