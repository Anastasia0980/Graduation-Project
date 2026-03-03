<template>
  <div class="container">
    <h1>RL 对抗测评平台</h1>

    <div class="button-group">
      <button @click="openModal('student1')">学生1提交</button>
      <button @click="openModal('student2')">学生2提交</button>
    </div>

    <div v-if="showModal" class="modal">
      <div class="modal-content">
        <h2>上传文件 - {{ currentStudent }}</h2>

        <div class="file-item">
          <label>Config 文件：</label>
          <input type="file" @change="handleFileChange($event, 'config')" />
        </div>

        <div class="file-item">
          <label>Model 文件：</label>
          <input type="file" @change="handleFileChange($event, 'model')" />
        </div>

        <div class="modal-buttons">
          <button @click="submitFiles">提交</button>
          <button @click="closeModal">取消</button>
        </div>
      </div>
    </div>

    <div v-if="message" class="message">
      {{ message }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'App',

  data () {
    return {
      showModal: false,
      currentStudent: '',
      selectedFiles: {
        config: null,
        model: null
      },
      message: ''
    }
  },

  methods: {
    openModal (type) {
      this.currentStudent = type
      this.showModal = true
      this.selectedFiles.config = null
      this.selectedFiles.model = null
    },

    closeModal () {
      this.showModal = false
    },

    handleFileChange (event, type) {
      this.selectedFiles[type] = event.target.files[0]
    },

    async submitFiles () {
      if (!this.selectedFiles.config || !this.selectedFiles.model) {
        this.message = '请同时选择 config 和 model 文件'
        return
      }

      const form = new FormData()
      form.append('config', this.selectedFiles.config)
      form.append('model', this.selectedFiles.model)
      form.append('studentType', this.currentStudent)

      try {
        const response = await fetch('http://localhost:8081/upload', {
          method: 'POST',
          body: form
        })

        if (!response.ok) {
          throw new Error('上传失败')
        }

        const result = await response.text()
        this.closeModal()

        if (result.includes('测评完成')) {
          alert(result)
        } else {
          this.message = '上传成功: ' + result
        }
      } catch (error) {
        this.message = '上传出错: ' + error.message
      }
    }
  }
}
</script>

<style>
.container {
  text-align: center;
  margin-top: 50px;
  font-family: Arial, sans-serif;
}

.button-group {
  margin: 20px 0;
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
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
  width: 400px;
}

.file-item {
  margin: 15px 0;
}

.modal-buttons {
  margin-top: 20px;
}

.message {
  margin-top: 20px;
  font-weight: bold;
  color: green;
}
</style>
