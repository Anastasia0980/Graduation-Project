<template>
  <div class="pagination-wrapper" v-if="total > 0">
    <div class="pagination-left">
      <span class="page-total">共 {{ total }} 条</span>
      <span class="page-size-label">每页</span>
      <select class="page-size-select" :value="pageSize" @change="handlePageSizeChange">
        <option v-for="item in pageSizeOptions" :key="item" :value="item">
          {{ item }}
        </option>
      </select>
      <span class="page-size-label">条</span>
    </div>

    <div class="pagination-right">
      <button class="page-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
        上一页
      </button>

      <button
        v-for="page in visiblePages"
        :key="page"
        class="page-number"
        :class="{ active: page === currentPage, ellipsis: page === '...'}"
        :disabled="page === '...'"
        @click="page !== '...' && changePage(page)"
      >
        {{ page }}
      </button>

      <button class="page-btn" :disabled="currentPage >= totalPages" @click="changePage(currentPage + 1)">
        下一页
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CommonPagination',
  props: {
    total: {
      type: Number,
      required: true
    },
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 5
    },
    pageSizeOptions: {
      type: Array,
      default: () => [5, 10, 20]
    }
  },
  emits: ['update:currentPage', 'update:pageSize', 'change'],
  computed: {
    totalPages () {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    visiblePages () {
      const total = this.totalPages
      const current = this.currentPage

      if (total <= 7) {
        return Array.from({ length: total }, (_, i) => i + 1)
      }

      if (current <= 4) {
        return [1, 2, 3, 4, 5, '...', total]
      }

      if (current >= total - 3) {
        return [1, '...', total - 4, total - 3, total - 2, total - 1, total]
      }

      return [1, '...', current - 1, current, current + 1, '...', total]
    }
  },
  methods: {
    changePage (page) {
      if (page < 1 || page > this.totalPages || page === this.currentPage) return
      this.$emit('update:currentPage', page)
      this.$emit('change', { currentPage: page, pageSize: this.pageSize })
    },
    handlePageSizeChange (event) {
      const size = Number(event.target.value)
      this.$emit('update:pageSize', size)
      this.$emit('update:currentPage', 1)
      this.$emit('change', { currentPage: 1, pageSize: size })
    }
  }
}
</script>

<style scoped>
.pagination-wrapper {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.pagination-left,
.pagination-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-total,
.page-size-label {
  font-size: 14px;
  color: #606266;
}

.page-size-select {
  height: 34px;
  min-width: 70px;
  padding: 0 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #303133;
  font-size: 14px;
}

.page-btn,
.page-number {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.page-btn:hover,
.page-number:hover {
  color: #1f4e8c;
  border-color: #1f4e8c;
}

.page-number.active {
  background: #1f4e8c;
  color: #ffffff;
  border-color: #1f4e8c;
}

.page-btn:disabled,
.page-number:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.page-number.ellipsis {
  cursor: default;
}

@media (max-width: 768px) {
  .pagination-wrapper {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-left,
  .pagination-right {
    flex-wrap: wrap;
  }
}
</style>
