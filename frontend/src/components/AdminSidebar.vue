<template>
  <aside class='sidebar'>
    <div class='sidebar-title'>功能菜单</div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "announcement" }'
      @click="$emit('change-menu', 'announcement')"
    >
      公告管理
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "help" }'
      @click="$emit('change-menu', 'help')"
    >
      说明管理
    </div>

    <div
      class='menu-item'
      :class='{ active: activeMenu === "user" }'
      @click="$emit('change-menu', 'user')"
    >
      用户管理
    </div>
  </aside>
</template>

<script>
export default {
  name: 'AdminSidebar',
  props: {
    activeMenu: {
      type: String,
      default: 'announcement'
    }
  },
  emits: ['change-menu']
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
  color: #163a66;
  background: #e7f0fb;
  border-right: 3px solid #163a66;
  font-weight: 700;
}
</style>
