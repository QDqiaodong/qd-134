<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElInputNumber, ElPagination, ElMessage, ElCard } from 'element-plus'
import { equipmentApi, type Equipment } from '@/api'

const equipments = ref<Equipment[]>([])
const loading = ref(false)
const minDepth = ref(0)
const maxDepth = ref(50)
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

const loadEquipments = async () => {
  loading.value = true
  try {
    const result = await equipmentApi.filter(minDepth.value, maxDepth.value, currentPage.value, pageSize.value)
    equipments.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (minDepth.value >= maxDepth.value) {
    ElMessage.error('最小深度必须小于最大深度')
    return
  }
  currentPage.value = 0
  loadEquipments()
}

onMounted(() => {
  loadEquipments()
})
</script>

<template>
  <div class="equipment-filter">
    <ElCard class="filter-card">
      <div class="filter-form">
        <div class="filter-row">
          <div class="filter-item">
            <label>最小深度(m)</label>
            <ElInputNumber 
              v-model="minDepth" 
              :min="0" 
              :max="maxDepth - 1"
              placeholder="最小深度"
              style="width: 150px"
            />
          </div>
          <span class="separator">-</span>
          <div class="filter-item">
            <label>最大深度(m)</label>
            <ElInputNumber 
              v-model="maxDepth" 
              :min="minDepth + 1" 
              :max="500"
              placeholder="最大深度"
              style="width: 150px"
            />
          </div>
          <ElButton type="primary" @click="handleSearch">筛选</ElButton>
        </div>
      </div>
    </ElCard>
    
    <div class="result-info">
      <span>筛选结果：共 {{ total }} 件装备</span>
      <span class="depth-range">深度区间: {{ minDepth }}m - {{ maxDepth }}m</span>
    </div>
    
    <ElTable :data="equipments" :loading="loading" border stripe style="width: 100%">
      <ElTableColumn prop="code" label="装备编号" width="120" />
      <ElTableColumn prop="name" label="装备名称" width="150" />
      <ElTableColumn label="最大耐压深度(m)" width="150">
        <template #default="{ row }">
          <span :class="['depth-value', { highlight: row.maxDepth >= minDepth && row.maxDepth <= maxDepth }]">
            {{ row.maxDepth }}m
          </span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="weight" label="重量(kg)" width="100" />
      <ElTableColumn prop="specification" label="规格型号" width="150" />
      <ElTableColumn prop="description" label="描述" show-overflow-tooltip />
    </ElTable>
    
    <ElPagination
      :current-page="currentPage + 1"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(page) => { currentPage = page - 1; loadEquipments() }"
      @size-change="(size) => { pageSize = size; loadEquipments() }"
      style="margin-top: 20px; text-align: right"
    />
  </div>
</template>

<style scoped>
.equipment-filter {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  padding: 16px 0;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item label {
  font-size: 14px;
  font-weight: 500;
  color: #64748B;
}

.separator {
  font-size: 20px;
  color: #CBD5E1;
  margin: 0 4px;
}

.result-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #F8FAFC;
  border-radius: 6px;
}

.result-info span {
  font-size: 14px;
  color: #64748B;
}

.depth-range {
  color: #0A2463;
  font-weight: 500;
}

.depth-value {
  font-weight: 500;
  color: #0A2463;
}

.depth-value.highlight {
  color: #3E92CC;
}
</style>