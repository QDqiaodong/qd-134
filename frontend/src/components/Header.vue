<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/equipment': '装备管理',
    '/team': '小组管理',
    '/binding': '深度绑定',
    '/filter': '装备筛选'
  }
  for (const path of Object.keys(titles)) {
    if (route.path.startsWith(path)) {
      return titles[path]
    }
  }
  return '潜水基地系统'
})
</script>

<template>
  <header class="header">
    <div class="header-content">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <div class="header-right">
        <span class="user-info">管理员</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  background: white;
  border-bottom: 1px solid #E2E8F0;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #0A2463;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  font-size: 14px;
  color: #64748B;
}
</style>