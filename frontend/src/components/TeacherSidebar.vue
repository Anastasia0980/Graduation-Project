<template>
  <aside class='sidebar'>
    <div class='sidebar-title'>功能菜单</div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "teacher-home" }'
      @click="$emit('teacher-home-click')"
    >
      教师主页
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "task-hall" }'
      @click="$emit('task-hall-click')"
    >
      任务大厅
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "submission-history" }'
      @click="$emit('history-click')"
    >
      提交历史
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "publish-task" }'
      @click="$emit('publish-click')"
    >
      发布任务
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "task-manage" }'
      @click="$emit('manage-click')"
    >
      任务管理
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "class-data" }'
      @click="$emit('class-data-click')"
    >
      班级管理
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "export-score" }'
      @click="$emit('export-click')"
    >
      导出成绩
    </div>

    <div
      class='menu-item ranking-menu-item'
      :class='{ active: activeMenu === "ranking" }'
      @click='goRanking'
    >
      排行榜
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
  emits: [
    'teacher-home-click',
    'task-hall-click',
    'history-click',
    'publish-click',
    'manage-click',
    'class-data-click',
    'export-click'
  ],
  methods: {
    goRanking () {
      this.$router.push('/teacher/ranking')
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

.ranking-menu-item {
  margin-top: 0;
}

@media (max-width: 900px) {
  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #dcdfe6;
  }

  .ranking-menu-item {
    margin-top: 0;
  }
}
</style>
