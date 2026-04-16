<template>
  <aside class='sidebar'>
    <div class='sidebar-title'>功能菜单</div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "teacher-home" }'
      @click='goTeacherHome'
    >
      教师主页
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "task-hall" }'
      @click='goTaskHall'
    >
      任务大厅
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "task-manage" }'
      @click='goTaskManage'
    >
      任务管理
    </div>

    <div
      class='menu-item menu-item-parent'
      :class='{ active: isOverviewSectionActive }'
      @click='toggleOverviewMenu'
    >
      <span>任务概览</span>
      <span class='arrow'>{{ overviewOpen ? '−' : '+' }}</span>
    </div>

    <div v-if='overviewOpen' class='submenu'>
      <div
        class='submenu-item'
        :class='{ active: activeMenu === "task-overview-summary" }'
        @click.stop='goOverviewSummary'
      >
        提交数据总览
      </div>

      <div
        class='submenu-item'
        :class='{ active: activeMenu === "ranking" }'
        @click.stop='goRanking'
      >
        排行榜
      </div>

      <div
        class='submenu-item'
        :class='{ active: activeMenu === "export-score" }'
        @click.stop='goExportScore'
      >
        导出成绩
      </div>
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "class-data" }'
      @click='goClassData'
    >
      班级管理
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "environment-manage" }'
      @click='goEnvironmentManage'
    >
      环境管理
    </div>
  </aside>
</template>

<script>
export default {
  name: 'TeacherSidebar',
  props: {
    activeMenu: {
      type: String,
      default: 'teacher-home'
    }
  },
  data () {
    return {
      overviewOpen: ['task-overview-summary', 'ranking', 'export-score'].includes(this.activeMenu)
    }
  },
  computed: {
    isOverviewSectionActive () {
      return ['task-overview-summary', 'ranking', 'export-score'].includes(this.activeMenu)
    }
  },
  watch: {
    activeMenu: {
      immediate: true,
      handler (val) {
        if (['task-overview-summary', 'ranking', 'export-score'].includes(val)) {
          this.overviewOpen = true
        }
      }
    }
  },
  methods: {
    toggleOverviewMenu () {
      this.overviewOpen = !this.overviewOpen
    },
    goTeacherHome () {
      this.$router.push('/teacher/home')
    },
    goTaskHall () {
      this.$router.push('/teacher/hall')
    },
    goTaskManage () {
      this.$router.push('/teacher/tasks')
    },
    goOverviewSummary () {
      this.$router.push('/teacher/overview')
    },
    goRanking () {
      this.$router.push('/teacher/ranking')
    },
    goExportScore () {
      this.$router.push('/teacher/export')
    },
    goClassData () {
      this.$router.push('/teacher/classes')
    },
    goEnvironmentManage () {
      this.$router.push('/teacher/environment')
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
