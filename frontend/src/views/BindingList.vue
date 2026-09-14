<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElPagination, ElMessage, ElTag } from 'element-plus'
import { bindingApi, type BindingRecord } from '@/api'

const bindings = ref<BindingRecord[]>([])
const loading = ref(false)
const overCertifiedFilter = ref<boolean | undefined>(undefined)
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

// 超证标记由后端按当前持证深度实时计算，刷新后保持一致
const loadBindings = async () => {
  loading.value = true
  try {
    const result = await bindingApi.list(currentPage.value, pageSize.value, overCertifiedFilter.value)
    bindings.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 0
  loadBindings()
}

const handleRefresh = () => {
  loadBindings()
}

onMounted(() => {
  loadBindings()
})
</script>

<template>
  <div class="binding-list">
    <div class="toolbar">
      <ElSelect
        v-model="overCertifiedFilter"
        placeholder="是否超证"
        clearable
        style="width: 160px"
        @change="handleFilterChange"
      >
        <ElOption label="仅超证" :value="true" />
        <ElOption label="仅正常" :value="false" />
      </ElSelect>
      <ElButton type="primary" @click="handleRefresh">刷新</ElButton>
    </div>

    <ElTable :data="bindings" :loading="loading" border stripe style="width: 100%">
      <ElTableColumn prop="teamName" label="潜水小组" width="160" />
      <ElTableColumn label="持证深度(m)" width="110">
        <template #default="{ row }">
          <span>{{ row.teamCertifiedDepth }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="equipmentName" label="装备名称" width="180" />
      <ElTableColumn label="装备额定深度(m)" width="140">
        <template #default="{ row }">
          <span :class="{ 'over-depth': row.overCertified }">{{ row.equipmentMaxDepth }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="超证标记" width="110" align="center">
        <template #default="{ row }">
          <ElTag :type="row.overCertified ? 'danger' : 'success'">
            {{ row.overCertified ? '超证' : '正常' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="boundAt" label="绑定时间" width="180" />
      <ElTableColumn prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <ElTag :type="row.status === 'ACTIVE' ? 'primary' : 'info'">
            {{ row.status === 'ACTIVE' ? '生效中' : row.status }}
          </ElTag>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElPagination
      :current-page="currentPage + 1"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(page) => { currentPage = page - 1; loadBindings() }"
      @size-change="(size) => { pageSize = size; loadBindings() }"
      style="margin-top: 20px; text-align: right"
    />
  </div>
</template>

<style scoped>
.binding-list {
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

.over-depth {
  color: #F56C6C;
  font-weight: 600;
}
</style>
