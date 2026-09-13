<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElButton, ElInput, ElPagination, ElMessageBox, ElMessage } from 'element-plus'
import { equipmentApi, type Equipment } from '@/api'

const router = useRouter()
const equipments = ref<Equipment[]>([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

const loadEquipments = async () => {
  loading.value = true
  try {
    const result = await equipmentApi.list(currentPage.value, pageSize.value, keyword.value)
    equipments.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadEquipments()
}

const handleEdit = (id: number) => {
  router.push(`/equipment/edit/${id}`)
}

const handleDelete = async (id: number, name: string) => {
  try {
    await ElMessageBox.confirm(`确定要删除装备「${name}」吗？`, '确认删除', {
      type: 'warning'
    })
    await equipmentApi.delete(id)
    ElMessage.success('删除成功')
    loadEquipments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error((error as Error).message)
    }
  }
}

const handleAdd = () => {
  router.push('/equipment/add')
}

onMounted(() => {
  loadEquipments()
})
</script>

<template>
  <div class="equipment-list">
    <div class="toolbar">
      <ElInput
        v-model="keyword"
        placeholder="搜索装备名称或编号"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <ElButton type="primary" @click="handleSearch">搜索</ElButton>
      <ElButton type="success" @click="handleAdd">新增装备</ElButton>
    </div>
    
    <ElTable :data="equipments" :loading="loading" border stripe style="width: 100%">
      <ElTableColumn prop="code" label="装备编号" width="120" />
      <ElTableColumn prop="name" label="装备名称" width="150" />
      <ElTableColumn prop="maxDepth" label="最大耐压深度(m)" width="150" />
      <ElTableColumn prop="weight" label="重量(kg)" width="100" />
      <ElTableColumn prop="specification" label="规格型号" width="150" />
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
      @current-change="(page) => { currentPage = page - 1; loadEquipments() }"
      @size-change="(size) => { pageSize = size; loadEquipments() }"
      style="margin-top: 20px; text-align: right"
    />
  </div>
</template>

<style scoped>
.equipment-list {
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