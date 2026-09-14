<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElRadioGroup, ElRadioButton,
  ElPagination, ElDialog, ElTag, ElMessage
} from 'element-plus'
import { notificationApi, teamApi, type Notification, type Team } from '@/api'

const notifications = ref<Notification[]>([])
const teams = ref<Team[]>([])
const loading = ref(false)
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

const filterTeamId = ref<number | undefined>(undefined)
const filterReadStatus = ref<'' | 'false' | 'true'>('')

const detailVisible = ref(false)
const currentNotification = ref<Notification | null>(null)

const loadNotifications = async () => {
  loading.value = true
  try {
    const teamId = filterTeamId.value || undefined
    const readStatus = filterReadStatus.value === '' ? undefined : filterReadStatus.value === 'true'
    const result = await notificationApi.list(currentPage.value, pageSize.value, teamId, readStatus)
    notifications.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const loadTeams = async () => {
  try {
    const result = await teamApi.list(0, 100)
    teams.value = result.content
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const handleFilterChange = () => {
  currentPage.value = 0
  loadNotifications()
}

const openDetail = async (row: Notification) => {
  currentNotification.value = row
  detailVisible.value = true
  if (!row.readStatus) {
    try {
      const updated = await notificationApi.markRead(row.id)
      row.readStatus = updated.readStatus
      row.readAt = updated.readAt
      if (currentNotification.value?.id === row.id) {
        currentNotification.value.readStatus = updated.readStatus
        currentNotification.value.readAt = updated.readAt
      }
    } catch (error) {
      ElMessage.error((error as Error).message)
    }
  }
}

onMounted(() => {
  loadTeams()
  loadNotifications()
})
</script>

<template>
  <div class="notification-list">
    <div class="toolbar">
      <ElSelect
        v-model="filterTeamId"
        placeholder="全部小组"
        clearable
        style="width: 200px"
        @change="handleFilterChange"
      >
        <ElOption v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
      </ElSelect>
      <ElRadioGroup v-model="filterReadStatus" @change="handleFilterChange">
        <ElRadioButton value="">全部</ElRadioButton>
        <ElRadioButton value="false">未读</ElRadioButton>
        <ElRadioButton value="true">已读</ElRadioButton>
      </ElRadioGroup>
      <ElButton type="primary" @click="loadNotifications">刷新</ElButton>
    </div>

    <ElTable
      :data="notifications"
      :loading="loading"
      border
      stripe
      style="width: 100%"
      :row-class-name="({ row }: any) => row.readStatus ? '' : 'unread-row'"
      @row-click="(row) => openDetail(row as Notification)"
    >
      <ElTableColumn prop="title" label="标题" width="180" />
      <ElTableColumn prop="teamName" label="小组" width="140" />
      <ElTableColumn label="解绑件数" width="100" align="center">
        <template #default="{ row }">
          <ElTag type="warning">{{ row.removedCount }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="equipmentNames" label="解绑装备" show-overflow-tooltip />
      <ElTableColumn label="状态" width="90" align="center">
        <template #default="{ row }">
          <ElTag :type="row.readStatus ? 'success' : 'danger'">
            {{ row.readStatus ? '已读' : '未读' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="createdAt" label="推送时间" width="180" />
      <ElTableColumn label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <ElButton type="primary" link @click.stop="openDetail(row as Notification)">
            {{ row.readStatus ? '查看' : '打开并标已读' }}
          </ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElPagination
      :current-page="currentPage + 1"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(page) => { currentPage = page - 1; loadNotifications() }"
      @size-change="(size) => { pageSize = size; loadNotifications() }"
      style="margin-top: 20px; text-align: right"
    />

    <ElDialog
      v-model="detailVisible"
      :title="currentNotification?.title"
      width="520px"
    >
      <div v-if="currentNotification" class="notification-detail">
        <div class="detail-meta">
          <ElTag :type="currentNotification.readStatus ? 'success' : 'danger'" size="small">
            {{ currentNotification.readStatus ? '已读' : '未读' }}
          </ElTag>
          <span>小组：{{ currentNotification.teamName }}</span>
          <span>推送时间：{{ currentNotification.createdAt }}</span>
        </div>
        <p class="detail-content">{{ currentNotification.content }}</p>
        <div class="detail-stats">
          <span>解绑件数：{{ currentNotification.removedCount }}</span>
          <span>解绑装备：{{ currentNotification.equipmentNames }}</span>
        </div>
      </div>
      <template #footer>
        <ElButton type="primary" @click="detailVisible = false">关闭</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.notification-list {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

:deep(.unread-row) {
  font-weight: 600;
}

.notification-detail .detail-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  color: #64748B;
  font-size: 13px;
  margin-bottom: 16px;
}

.notification-detail .detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: #1F2D3D;
  margin: 0 0 16px;
}

.notification-detail .detail-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #F5F7FA;
  border-radius: 6px;
  font-size: 13px;
  color: #64748B;
}
</style>
