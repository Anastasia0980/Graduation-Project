<template>
  <div class='container'>
    <h1>RL 对抗测评平台（自动匹配对战）</h1>

    <div class='top-buttons'>
      <button @click='openModal'>提交参赛（上传 model + config）</button>
      <button @click='loadTasks'>查询任务详情</button>
    </div>

    <div v-if='showModal' class='modal'>
      <div class='modal-content'>
        <h2>提交参赛</h2>

        <div class='file-item'>
          <label>studentId：</label>
          <input v-model='studentId' placeholder='例如 10001' />
        </div>

        <div class='file-item'>
          <label>environment：</label>
          <input v-model='environment' placeholder='tictactoe_v3' />
        </div>

        <div class='file-item'>
          <label>games：</label>
          <input v-model.number='games' type='number' min='1' />
        </div>

        <div class='file-item'>
          <label>Config：</label>
          <input type='file' accept='.json' @change='handleFileChange($event, "config")' />
        </div>

        <div class='file-item'>
          <label>Model：</label>
          <input type='file' accept='.pt,.pth' @change='handleFileChange($event, "model")' />
        </div>

        <div class='modal-buttons'>
          <button @click='submit' :disabled='loading'>
            {{ loading ? '提交中...' : '提交' }}
          </button>
          <button @click='closeModal'>取消</button>
        </div>
      </div>
    </div>

    <div v-if='message' class='message'>{{ message }}</div>

    <div v-if='evaluationId' class='message'>
      已匹配并启动对战：evaluationId = {{ evaluationId }}
    </div>

    <div v-if='taskList.length > 0' class='task-section'>
      <h2>任务详情</h2>
      <table class='task-table'>
        <thead>
          <tr>
            <th>任务编号</th>
            <th>当前状态</th>
            <th>学生1</th>
            <th>学生2</th>
            <th>胜负关系</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for='task in taskList' :key='task.evaluationId'>
            <td>{{ task.evaluationId }}</td>
            <td>{{ task.status }}</td>
            <td>{{ task.student1Id }}</td>
            <td>{{ task.student2Id }}</td>
            <td>{{ task.winnerText }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
const API_BASE = (process.env.VUE_APP_API_BASE && process.env.VUE_APP_API_BASE.trim()) ||
  (typeof window !== 'undefined'
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : 'http://localhost:8080')

export default {
  name: 'BattlePage',
  data () {
    return {
      showModal: false,
      loading: false,
      message: '',
      evaluationId: null,

      studentId: '',
      environment: 'tictactoe_v3',
      games: 50,

      selectedFiles: { config: null, model: null },

      taskList: []
    }
  },
  methods: {
    openModal () {
      this.showModal = true
      this.message = ''
      this.evaluationId = null
      this.selectedFiles = { config: null, model: null }
    },
    closeModal () {
      this.showModal = false
    },
    handleFileChange (e, type) {
      this.selectedFiles[type] = e.target.files[0]
    },
    async submit () {
      if (!this.studentId) {
        this.message = '请填写 studentId'
        return
      }
      if (!this.selectedFiles.config || !this.selectedFiles.model) {
        this.message = '请同时选择 config 和 model 文件'
        return
      }

      const form = new FormData()
      form.append('model', this.selectedFiles.model)
      form.append('config', this.selectedFiles.config)
      form.append('studentId', this.studentId)
      form.append('environment', this.environment)
      form.append('games', String(this.games))

      this.loading = true
      this.message = ''
      this.evaluationId = null

      try {
        const resp = await fetch(`${API_BASE}/battle/submit`, {
          method: 'POST',
          body: form
        })
        const res = await resp.json()

        if (res.code !== 0) {
          throw new Error(res.message || '提交失败')
        }

        const payload = res.data || {}
        this.message = payload.message || '提交成功'

        if (payload.evaluationId) {
          this.evaluationId = payload.evaluationId
          this.closeModal()
          await this.loadTasks()
        }
      } catch (e) {
        this.message = `提交失败：${e.message}`
      } finally {
        this.loading = false
      }
    },
    async loadTasks () {
      try {
        const resp = await fetch(`${API_BASE}/battle/tasks`)
        const res = await resp.json()

        if (res.code !== 0) {
          throw new Error(res.message || '查询失败')
        }

        this.taskList = res.data || []
      } catch (e) {
        this.message = `查询任务详情失败：${e.message}`
      }
    }
  }
}
</script>

<style scoped>
.container {
  text-align: center;
  margin-top: 50px;
  font-family: Arial, sans-serif;
  padding-bottom: 40px;
}

.top-buttons {
  margin-bottom: 20px;
}

button {
  padding: 10px 20px;
  margin: 10px;
  font-size: 16px;
  cursor: pointer;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
  width: 420px;
  text-align: left;
}

.file-item {
  margin: 12px 0;
  display: flex;
  gap: 10px;
  align-items: center;
}

.file-item label {
  width: 110px;
}

.modal-buttons {
  margin-top: 20px;
  text-align: right;
}

.message {
  margin-top: 20px;
  font-weight: bold;
}

.task-section {
  width: 80%;
  margin: 30px auto;
  text-align: center;
}

.task-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
}

.task-table th,
.task-table td {
  border: 1px solid #ddd;
  padding: 10px;
}

.task-table th {
  background-color: #f2f2f2;
}
</style>
