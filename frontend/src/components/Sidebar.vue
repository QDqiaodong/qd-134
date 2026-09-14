<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const isCollapsed = ref(false)

const menuItems = [
  { path: '/equipment', label: '装备管理', icon: '⚙️' },
  { path: '/team', label: '小组管理', icon: '👥' },
  { path: '/binding', label: '深度绑定', icon: '🔗' },
  { path: '/filter', label: '装备筛选', icon: '🔍' },
  { path: '/notification', label: '站内通知', icon: '🔔' }
]

const isActive = (path: string) => route.path.startsWith(path)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<template>
  <aside :class="['sidebar', { collapsed: isCollapsed }]">
    <div class="logo-section">
      <div class="logo-icon">🌊</div>
      <span v-if="!isCollapsed" class="logo-text">潜水基地</span>
    </div>
    
    <nav class="menu-section">
      <ul class="menu-list">
        <li v-for="item in menuItems" :key="item.path">
          <button
            :class="['menu-item', { active: isActive(item.path) }]"
            @click="router.push(item.path)"
          >
            <span class="menu-icon">{{ item.icon }}</span>
            <span v-if="!isCollapsed" class="menu-label">{{ item.label }}</span>
          </button>
        </li>
      </ul>
    </nav>
    
    <button class="collapse-btn" @click="toggleCollapse">
      {{ isCollapsed ? '▶' : '◀' }}
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 200px;
  background: linear-gradient(180deg, #0A2463 0%, #1E3A8A 100%);
  color: white;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 60px;
}

.logo-section {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
}

.menu-section {
  flex: 1;
  padding: 16px 0;
}

.menu-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu-item {
  width: 100%;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.menu-item.active {
  background: rgba(62, 146, 204, 0.3);
  color: white;
  border-left: 3px solid #3E92CC;
}

.menu-icon {
  font-size: 18px;
}

.menu-label {
  font-size: 14px;
}

.collapse-btn {
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>