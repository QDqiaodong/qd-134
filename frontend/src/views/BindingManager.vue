<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElMessageBox, ElMessage, ElTag, ElCard } from 'element-plus'
import { teamApi, equipmentApi, bindingApi, type Team, type Equipment } from '@/api'

const teams = ref<Team[]>([])
const equipments = ref<Equipment[]>([])
const selectedTeamId = ref<number | null>(null)
const boundEquipments = ref<Equipment[]>([])
const loading = ref(false)
// 提交中的绑定请求（按装备维度），用于禁用按钮防连点，避免同一装备重复提交
const bindingIds = ref<Set<number>>(new Set())

const isBinding = (equipmentId: number) => bindingIds.value.has(equipmentId)

const selectedTeam = computed(() => {
  return teams.value.find(t => t.id === selectedTeamId.value)
})

// 只合计当前已绑定（占用中）的装备；已解绑的已从列表移除，不会计入；未填重量按 0
const totalWeight = computed(() => {
  return boundEquipments.value.reduce((sum, equipment) => {
    const weight = Number(equipment.weight ?? 0)
    return sum + (Number.isFinite(weight) ? weight : 0)
  }, 0)
})

const formatWeight = (weight: number) => weight.toFixed(2)

const unboundEquipments = computed(() => {
  const boundIds = new Set(boundEquipments.value.map(e => e.id))
  return equipments.value.filter(e => !boundIds.has(e.id))
})

const canBind = (equipment: Equipment) => {
  if (!selectedTeam.value) return false
  return equipment.maxDepth <= selectedTeam.value.certifiedDepth
}

const loadTeams = async () => {
  try {
    const result = await teamApi.list(0, 100)
    teams.value = result.content
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const loadEquipments = async () => {
  try {
    const result = await equipmentApi.list(0, 100)
    equipments.value = result.content
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const loadBoundEquipments = async () => {
  if (!selectedTeamId.value) {
    boundEquipments.value = []
    return
  }
  loading.value = true
  try {
    const result = await bindingApi.getByTeam(selectedTeamId.value)
    boundEquipments.value = result
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const handleTeamChange = () => {
  loadBoundEquipments()
}

const handleBind = async (equipmentId: number) => {
  if (!selectedTeamId.value) return
  // 连点/重入直接忽略：同一装备的提交未返回前不再发第二个请求
  if (isBinding(equipmentId)) return

  const equipment = equipments.value.find(e => e.id === equipmentId)
  if (!equipment) return

  bindingIds.value.add(equipmentId)
  try {
    await bindingApi.create({
      teamId: selectedTeamId.value,
      equipmentId
    })
    ElMessage.success(`成功绑定装备「${equipment.name}」`)
    await loadBoundEquipments()
    loadTeams()
  } catch (error) {
    // 已提交过/重复提交时后端返回明确提示，刷新占用清单保持与服务端一致
    ElMessage.error((error as Error).message)
    loadBoundEquipments()
  } finally {
    bindingIds.value.delete(equipmentId)
  }
}

const handleUnbind = async (equipmentId: number) => {
  const equipment = boundEquipments.value.find(e => e.id === equipmentId)
  if (!equipment) return
  
  try {
    await ElMessageBox.confirm(`确定要解绑装备「${equipment.name}」吗？`, '确认解绑', {
      type: 'warning'
    })
    
    const bindings = await bindingApi.list(0, 100)
    const binding = bindings.content.find(b => b.teamId === selectedTeamId.value && b.equipmentId === equipmentId)
    
    if (binding) {
      await bindingApi.delete(binding.id)
    }
    
    ElMessage.success('解绑成功')
    loadBoundEquipments()
    loadTeams()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error((error as Error).message)
    }
  }
}

const handleSyncDepth = async () => {
  if (!selectedTeam.value) return
  
  try {
    const result = await ElMessageBox.prompt(
      `当前小组最大深度: ${selectedTeam.value.maxDepth}m\n请输入新的最大深度:`,
      '同步深度权限',
      {
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '请输入有效的正整数'
      }
    )
    
    const newMaxDepth = parseInt(result.value)

    if (newMaxDepth > selectedTeam.value.certifiedDepth) {
      ElMessage.error(`目标深度(${newMaxDepth}m)超过小组持证深度(${selectedTeam.value.certifiedDepth}m)，证深不符禁止同步`)
      return
    }

    const syncResult = await bindingApi.sync({
      teamId: selectedTeam.value.id,
      newMaxDepth,
      operator: '管理员'
    })
    
    ElMessage.success(`同步完成，移除${syncResult.removedCount}件超深度装备`)
    loadTeams()
    loadBoundEquipments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error((error as Error).message)
    }
  }
}

onMounted(() => {
  loadTeams()
  loadEquipments()
})
</script>

<template>
  <div class="binding-manager">
    <div class="section-header">
      <div class="team-select">
        <ElSelect 
          v-model="selectedTeamId" 
          placeholder="请选择潜水小组" 
          style="width: 240px"
          @change="handleTeamChange"
        >
          <ElOption v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
        </ElSelect>
        <ElButton 
          v-if="selectedTeam" 
          type="warning" 
          @click="handleSyncDepth"
        >
          同步深度权限
        </ElButton>
      </div>
    </div>
    
    <div v-if="selectedTeam" class="team-info-card">
      <ElCard>
        <div class="team-info">
          <h3>{{ selectedTeam.name }}</h3>
          <div class="info-row">
            <span>成员数量: {{ selectedTeam.memberCount }}</span>
            <span>深度范围: {{ selectedTeam.minDepth }}m - {{ selectedTeam.maxDepth }}m</span>
            <span>持证深度: <ElTag :type="selectedTeam.maxDepth <= selectedTeam.certifiedDepth ? 'success' : 'danger'">{{ selectedTeam.certifiedDepth }}m</ElTag></span>
            <span>占用装备总重: <strong>{{ formatWeight(totalWeight) }} kg</strong>（{{ boundEquipments.length }} 件）</span>
          </div>
        </div>
      </ElCard>
    </div>
    
    <div class="binding-content">
      <div class="panel">
        <h3>可绑定装备</h3>
        <ElTable :data="unboundEquipments" border stripe style="width: 100%">
          <ElTableColumn prop="code" label="编号" width="100" />
          <ElTableColumn prop="name" label="装备名称" width="150" />
          <ElTableColumn label="最大耐压深度(m)" width="150">
            <template #default="{ row }">
              <ElTag :type="canBind(row as Equipment) ? 'success' : 'danger'">
                {{ (row as Equipment).maxDepth }}m
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="weight" label="重量(kg)" width="100" />
          <ElTableColumn label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <ElButton
                type="success"
                link
                :loading="isBinding((row as Equipment).id)"
                :disabled="!canBind(row as Equipment) || isBinding((row as Equipment).id)"
                @click="handleBind((row as Equipment).id)"
              >
                {{ isBinding((row as Equipment).id) ? '绑定中' : (canBind(row as Equipment) ? '绑定' : '超证') }}
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
      
      <div class="panel">
        <h3>已绑定装备</h3>
        <ElTable :data="boundEquipments" :loading="loading" border stripe style="width: 100%">
          <ElTableColumn prop="code" label="编号" width="100" />
          <ElTableColumn prop="name" label="装备名称" width="150" />
          <ElTableColumn label="最大耐压深度(m)" width="150">
            <template #default="{ row }">
              <span>{{ row.maxDepth }}m</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="weight" label="重量(kg)" width="100" />
          <ElTableColumn label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <ElButton type="danger" link @click="handleUnbind(row.id)">解绑</ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>
  </div>
</template>

<style scoped>
.binding-manager {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.section-header {
  margin-bottom: 20px;
}

.team-select {
  display: flex;
  gap: 12px;
  align-items: center;
}

.team-info-card {
  margin-bottom: 20px;
}

.team-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0A2463;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.info-row span {
  font-size: 14px;
  color: #64748B;
}

.binding-content {
  display: flex;
  gap: 24px;
}

.panel {
  flex: 1;
}

.panel h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0A2463;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .binding-content {
    flex-direction: column;
  }
}
</style>