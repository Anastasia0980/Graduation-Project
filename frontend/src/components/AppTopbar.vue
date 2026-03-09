<template>
  <header class='topbar'>
    <div class='topbar-left'>
      <div class='platform-name clickable' @click="$emit('platform-click')">
        强化学习智能体测评平台
      </div>

      <div class='top-nav'>
        <span class='top-nav-item' :class='{ active: activeNav === "home" }' @click="$emit('platform-click')">
          首页
        </span>
        <span class='top-nav-item'>公告通知</span>
        <span class='top-nav-item'>帮助说明</span>
      </div>
    </div>

    <div class='topbar-right'>
      <template v-if='loggedIn'>
        <button class='switch-btn' @click="$emit('switch-role')">
          {{ switchButtonText }}
        </button>
        <span class='user-name clickable' @click="$emit('user-click')">{{ userName }}</span>
        <button class='text-btn' @click="$emit('logout')">退出</button>
      </template>

      <template v-else>
        <button class='text-btn' @click="$emit('login')">登录</button>
        <button class='primary-btn top-btn' @click="$emit('register')">注册</button>
      </template>
    </div>
  </header>
</template>

<script>
export default {
  name: 'AppTopbar',
  props: {
    loggedIn: {
      type: Boolean,
      default: false
    },
    userName: {
      type: String,
      default: ''
    },
    activeNav: {
      type: String,
      default: 'home'
    },
    currentRole: {
      type: String,
      default: 'student'
    }
  },
  emits: ['platform-click', 'user-click', 'logout', 'login', 'register', 'switch-role'],
  computed: {
    switchButtonText () {
      return this.currentRole === 'teacher' ? '切换到学生' : '切换到教师'
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.platform-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  color: #1f4e8c;
}

.top-nav {
  display: flex;
  gap: 24px;
}

.top-nav-item {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

.top-nav-item:hover {
  color: #1f4e8c;
}

.top-nav-item.active {
  color: #1f4e8c;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.switch-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #bfd7f4;
  background: #ecf5ff;
  color: #1f4e8c;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.switch-btn:hover {
  background: #d9ecff;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.text-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.text-btn:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.primary-btn {
  height: 36px;
  padding: 0 16px;
  border: none;
  background: #1f4e8c;
  color: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.primary-btn:hover {
  background: #173b69;
}

.top-btn {
  min-width: 72px;
}

@media (max-width: 700px) {
  .topbar {
    height: auto;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
  }

  .topbar-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .top-nav {
    flex-wrap: wrap;
    gap: 16px;
  }

  .topbar-right {
    flex-wrap: wrap;
  }
}
</style>
