<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElInput, ElPagination, ElMessageBox, ElMessage, ElTag } from 'element-plus'
import { teamApi, type Team } from '@/api'

const router = useRouter()
const teams = ref<Team[]>([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

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

onMounted(() => {
  loadTeams()
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
      <ElTableColumn prop="memberCount" label="成员数量" width="100" />
      <ElTableColumn label="深度范围(m)" width="160">
        <template #default="{ row }">
          <span>{{ row.minDepth }} - {{ row.maxDepth }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="持证深度(m)" width="130">
        <template #default="{ row }">
          <ElTag :type="row.certifiedDepth >= row.maxDepth ? 'success' : 'warning'">
            {{ row.certifiedDepth }}
          </ElTag>
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
</style>