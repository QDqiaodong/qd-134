<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElInput, ElPagination, ElMessageBox, ElMessage, ElTag } from 'element-plus'
import { teamApi, seaConditionApi, type Team, type SeaConditionReport } from '@/api'

const router = useRouter()
const teams = ref<Team[]>([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

// 今天的海况单：每天一张，null 表示岸上还没交单（仅在名单旁提示，开潜拦截以后端为准）
const todaySea = ref<SeaConditionReport | null>(null)

const loadTodaySea = async () => {
  try {
    todaySea.value = await seaConditionApi.today()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const loadTeams = async () => {
  loading.value = true
  try {
    const result = await teamApi.list(currentPage.value, pageSize.value, keyword.value)
    teams.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadTeams()
}

const handleEdit = (id: number) => {
  router.push(`/team/edit/${id}`)
}

const handleDelete = async (id: number, name: string) => {
  try {
    await ElMessageBox.confirm(`确定要删除小组「${name}」吗？`, '确认删除', {
      type: 'warning'
    })
    await teamApi.delete(id)
    ElMessage.success('删除成功')
    loadTeams()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error((error as Error).message)
    }
  }
}

const handleAdd = () => {
  router.push('/team/add')
}

// 未填重量的装备按 0 计入合计；接口未返回时也按 0 展示
const formatWeight = (weight: number | null | undefined) => {
  const value = Number(weight ?? 0)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

onMounted(() => {
  loadTeams()
  loadTodaySea()
})
</script>

<template>
  <div class="team-list">
    <div class="toolbar">
      <ElInput
        v-model="keyword"
        placeholder="搜索小组名称"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <ElButton type="primary" @click="handleSearch">搜索</ElButton>
      <ElButton type="success" @click="handleAdd">新增小组</ElButton>
    </div>
    
    <ElTable :data="teams" :loading="loading" border stripe style="width: 100%">
      <ElTableColumn prop="name" label="小组名称" width="150" />
      <ElTableColumn label="今日海况" width="200" align="center">
        <template #default>
          <template v-if="todaySea">
            <ElTag :type="todaySea.divable ? 'success' : 'danger'">
              海况·{{ todaySea.divable ? '可下水' : '不能下水' }}
            </ElTag>
            <div class="sea-metric">
              浪高 {{ Number(todaySea.waveHeight ?? 0).toFixed(2) }}m ·
              能见度 {{ Number(todaySea.visibility ?? 0).toFixed(2) }}m
            </div>
          </template>
          <ElTag v-else type="info">海况·未交单</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="占用装备总重(kg)" width="160" align="right">
        <template #default="{ row }">
          <span class="total-weight">{{ formatWeight(row.totalWeight) }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="memberCount" label="成员数量" width="100" />
      <ElTableColumn label="深度范围(m)" width="160">
        <template #default="{ row }">
          <span>{{ row.minDepth }} - {{ row.maxDepth }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="持证深度(m)" width="130">
        <template #default="{ row }">
          <ElTag :type="row.maxDepth <= row.certifiedDepth ? 'success' : 'danger'">
            {{ row.certifiedDepth }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="深度通知" width="100" align="center">
        <template #default="{ row }">
          <ElTag :type="row.depthChangeNotify ? 'success' : 'info'">
            {{ row.depthChangeNotify ? '已订阅' : '未订阅' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="剧组状态" width="100" align="center">
        <template #default="{ row }">
          <ElTag :type="row.status === 'WRAPPED' ? 'info' : 'success'">
            {{ row.status === 'WRAPPED' ? '已收队' : '拍摄中' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="下潜状态" width="200" align="center">
        <template #default="{ row }">
          <ElTag v-if="row.activeDive" :type="row.activeDive.overdue ? 'danger' : 'warning'">
            {{ row.activeDive.overdue ? '未收潜·超时' : '未收潜' }}
          </ElTag>
          <ElTag v-else type="success">岸上</ElTag>
          <div v-if="row.activeDive" class="dive-time">
            开始 {{ row.activeDive.startTime }}
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="description" label="描述" show-overflow-tooltip />
      <ElTableColumn prop="createdAt" label="创建时间" width="180" />
      <ElTableColumn label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <ElButton type="primary" link @click="handleEdit(row.id)">编辑</ElButton>
          <ElButton type="danger" link @click="handleDelete(row.id, row.name)">删除</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
    
    <ElPagination
      :current-page="currentPage + 1"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(page) => { currentPage = page - 1; loadTeams() }"
      @size-change="(size) => { pageSize = size; loadTeams() }"
      style="margin-top: 20px; text-align: right"
    />
  </div>
</template>

<style scoped>
.team-list {
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

.total-weight {
  font-weight: 600;
  color: #0A2463;
}

.dive-time {
  margin-top: 4px;
  font-size: 12px;
  color: #64748B;
  line-height: 1.3;
}

.sea-metric {
  margin-top: 4px;
  font-size: 12px;
  color: #64748B;
  line-height: 1.3;
}
</style>