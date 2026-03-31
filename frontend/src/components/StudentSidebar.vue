<template>
  <aside class='sidebar'>
    <div class='sidebar-title'>功能菜单</div>

    <template v-if='loggedIn'>
      <div
        class='menu-item menu-item-parent'
        :class='{ active: activeMenu === "task-open" || activeMenu === "task-ended" }'
        @click='handleTaskMenuClick'
      >
        <span>任务广场</span>
        <span class='arrow'>{{ taskMenuOpen ? '−' : '+' }}</span>
      </div>

      <div v-if='taskMenuOpen' class='submenu'>
        <div
          class='submenu-item'
          :class='{ active: activeMenu === "task-open" }'
          @click.stop='handleOpenTaskClick'
        >
          开放中
        </div>
        <div
          class='submenu-item'
          :class='{ active: activeMenu === "task-ended" }'
          @click.stop='handleEndedTaskClick'
        >
          已结束
        </div>
      </div>

      <div
        class='menu-item'
        :class='{ active: activeMenu === "class" }'
        @click='handleClassClick'
      >
        我的班级
      </div>

      <div
        class='menu-item'
        :class='{ active: activeMenu === "ranking" }'
        @click='goRanking'
      >
        排行榜
      </div>

      <div
        class='menu-item'
        :class='{ active: activeMenu === "history" }'
        @click='handleHistoryClick'
      >
        提交历史
      </div>

      <div
        class='menu-item'
        :class='{ active: activeMenu === "profile" }'
        @click='handleProfileClick'
      >
        个人主页
      </div>

      <div
        class='menu-item'
        :class='{ active: activeMenu === "resource" }'
        @click='goResource'
      >
        环境下载
      </div>
    </template>

    <template v-else>
      <div class='menu-item active'>主页</div>
    </template>
  </aside>
</template>

<script>
export default {
  name: 'StudentSidebar',
  props: {
    loggedIn: {
      type: Boolean,
      default: false
    },
    activeMenu: {
      type: String,
      default: ''
    },
    taskMenuOpen: {
      type: Boolean,
      default: false
    }
  },
  emits: [
    'profile-click',
    'class-click',
    'toggle-task-menu',
    'open-task-click',
    'ended-task-click',
    'history-click'
  ],
  methods: {
    handleTaskMenuClick () {
      this.$emit('toggle-task-menu')
    },
    handleOpenTaskClick () {
      this.$emit('open-task-click')
    },
    handleEndedTaskClick () {
      this.$emit('ended-task-click')
    },
    handleClassClick () {
      this.$emit('class-click')
    },
    handleHistoryClick () {
      this.$emit('history-click')
    },
    handleProfileClick () {
      this.$emit('profile-click')
    },
    goRanking () {
      this.$router.push('/student/ranking')
    },
    goResource () {
      this.$router.push('/student/resources')
    }
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.sidebar {
  width: 220px;
  background: #ffffff;
  border-right: 1px solid #dcdfe6;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
}

.sidebar-title {
  padding: 0 20px 14px;
  font-size: 14px;
  font-weight: 700;
  color: #909399;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
}

.menu-item {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 14px;
  color: #303133;
  cursor: pointer;
}

.menu-item:hover {
  background: #f5f7fa;
}

.menu-item.active {
  color: #1f4e8c;
  background: #ecf5ff;
  border-right: 3px solid #1f4e8c;
  font-weight: 600;
}

.menu-item-parent {
  justify-content: space-between;
}

.arrow {
  font-size: 16px;
  color: #909399;
}

.submenu {
  background: #fafafa;
  padding: 4px 0;
}

.submenu-item {
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 36px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
}

.submenu-item:hover {
  background: #f0f2f5;
}

.submenu-item.active {
  color: #1f4e8c;
  font-weight: 600;
  background: #ecf5ff;
}

@media (max-width: 900px) {
  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #dcdfe6;
  }
}
</style>
