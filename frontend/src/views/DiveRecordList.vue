<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElPagination,
  ElMessage, ElTag, ElDialog, ElForm, ElFormItem, ElDatePicker, ElAlert
} from 'element-plus'
import { diveApi, teamApi, type DiveRecord, type Team } from '@/api'

const records = ref<DiveRecord[]>([])
const teams = ref<Team[]>([])
const loading = ref(false)
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

const filterTeamId = ref<number | null>(null)
// 状态过滤：null=全部，OPEN=未收潜，CLOSED=已收潜
const filterStatus = ref<string>('')

// ---------- 开潜弹窗 ----------
const startVisible = ref(false)
const startSubmitting = ref(false)
const startForm = reactive({
  teamId: null as number | null,
  startTime: null as Date | null,
  plannedEndTime: null as Date | null
})

// ---------- 收潜弹窗 ----------
const endVisible = ref(false)
const endSubmitting = ref(false)
const endRecord = ref<DiveRecord | null>(null)
const endForm = reactive({ actualEndTime: null as Date | null })

const pad = (n: number) => String(n).padStart(2, '0')
const formatDateTime = (d: Date) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`

const teamNameById = (teamId: number) =>
  teams.value.find(t => t.id === teamId)?.name ?? `#${teamId}`

const loadTeams = async () => {
  try {
    const result = await teamApi.list(0, 100)
    teams.value = result.content
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const loadRecords = async () => {
  loading.value = true
  try {
    const result = await diveApi.list(
      currentPage.value,
      pageSize.value,
      filterTeamId.value,
      filterStatus.value || null
    )
    records.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 0
  loadRecords()
}

// 同小组还有未收潜记录时禁用开潜选项；互斥以后端为准，这里只做前置引导
const isTeamDiving = (teamId: number) =>
  teams.value.find(t => t.id === teamId)?.activeDive != null

const openStartDialog = () => {
  const now = new Date()
  const inOneHour = new Date(now.getTime() + 60 * 60 * 1000)
  startForm.teamId = null
  startForm.startTime = now
  startForm.plannedEndTime = inOneHour
  startVisible.value = true
}

const submitStart = async () => {
  if (!startForm.teamId) {
    ElMessage.error('请选择潜水小组')
    return
  }
  if (!startForm.startTime) {
    ElMessage.error('请选择开始时刻')
    return
  }
  if (!startForm.plannedEndTime) {
    ElMessage.error('请选择预计结束时刻')
    return
  }
  // 前置校验：预计结束必须晚于开始；后端会再次校验
  if (startForm.plannedEndTime.getTime() <= startForm.startTime.getTime()) {
    ElMessage.error('预计结束时刻必须晚于开始时刻')
    return
  }

  startSubmitting.value = true
  try {
    await diveApi.start({
      teamId: startForm.teamId,
      startTime: formatDateTime(startForm.startTime),
      plannedEndTime: formatDateTime(startForm.plannedEndTime)
    })
    ElMessage.success('开潜成功')
    startVisible.value = false
    currentPage.value = 0
    await Promise.all([loadRecords(), loadTeams()])
  } catch (error) {
    // 互斥拦截时后端提示中包含上一潜的开始时刻
    ElMessage.error((error as Error).message)
  } finally {
    startSubmitting.value = false
  }
}

const openEndDialog = (record: DiveRecord) => {
  endRecord.value = record
  // 默认实际结束取当前时间，但不得早于开始时刻
  const start = new Date(record.startTime.replace(' ', 'T'))
  const now = new Date()
  endForm.actualEndTime = now > start ? now : start
  endVisible.value = true
}

const submitEnd = async () => {
  if (!endRecord.value) return
  if (!endForm.actualEndTime) {
    ElMessage.error('请选择实际结束时刻')
    return
  }
  const start = new Date(endRecord.value.startTime.replace(' ', 'T'))
  // 前置校验：实际结束不能早于开始；后端会再次校验并兜底
  if (endForm.actualEndTime.getTime() < start.getTime()) {
    ElMessage.error(`实际结束时刻不能早于开始时刻（${endRecord.value.startTime}）`)
    return
  }

  endSubmitting.value = true
  try {
    await diveApi.end(endRecord.value.id, formatDateTime(endForm.actualEndTime))
    ElMessage.success('收潜成功')
    endVisible.value = false
    await Promise.all([loadRecords(), loadTeams()])
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    endSubmitting.value = false
  }
}

onMounted(() => {
  loadTeams()
  loadRecords()
})
</script>

<template>
  <div class="dive-list">
    <div class="toolbar">
      <ElSelect
        v-model="filterTeamId"
        placeholder="全部小组"
        clearable
        style="width: 200px"
        @change="handleSearch"
      >
        <ElOption v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
      </ElSelect>
      <ElSelect v-model="filterStatus" style="width: 140px" @change="handleSearch">
        <ElOption label="全部状态" value="" />
        <ElOption label="未收潜" value="OPEN" />
        <ElOption label="已收潜" value="CLOSED" />
      </ElSelect>
      <ElButton type="primary" @click="handleSearch">查询</ElButton>
      <ElButton type="success" @click="openStartDialog">开下潜记录</ElButton>
    </div>

    <ElAlert
      type="info"
      :closable="false"
      title="同一小组只要还有未收潜记录就不能再开第二条；收潜必须填写实际结束时刻，且不能早于开始时刻。"
      class="rule-alert"
    />

    <ElTable :data="records" :loading="loading" border stripe style="width: 100%">
      <ElTableColumn label="小组" width="160">
        <template #default="{ row }">{{ (row as DiveRecord).teamName || teamNameById((row as DiveRecord).teamId) }}</template>
      </ElTableColumn>
      <ElTableColumn prop="startTime" label="开始时刻" width="180" />
      <ElTableColumn label="预计结束" width="180">
        <template #default="{ row }">
          <span :class="{ 'overdue-text': (row as DiveRecord).overdue }">
            {{ (row as DiveRecord).plannedEndTime }}
          </span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="实际结束" width="180">
        <template #default="{ row }">{{ (row as DiveRecord).actualEndTime || '—' }}</template>
      </ElTableColumn>
      <ElTableColumn label="状态" width="120" align="center">
        <template #default="{ row }">
          <ElTag v-if="(row as DiveRecord).open" :type="(row as DiveRecord).overdue ? 'danger' : 'warning'">
            {{ (row as DiveRecord).overdue ? '未收潜·超时' : '未收潜' }}
          </ElTag>
          <ElTag v-else type="success">已收潜</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <ElButton
            v-if="(row as DiveRecord).open"
            type="primary"
            link
            @click="openEndDialog(row as DiveRecord)"
          >
            收潜
          </ElButton>
          <span v-else class="muted">—</span>
        </template>
      </ElTableColumn>
    </ElTable>

    <ElPagination
      :current-page="currentPage + 1"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(page) => { currentPage = page - 1; loadRecords() }"
      style="margin-top: 20px; text-align: right"
    />

    <!-- 开潜弹窗 -->
    <ElDialog v-model="startVisible" title="开下潜记录" width="460px">
      <ElForm label-width="110px">
        <ElFormItem label="潜水小组" required>
          <ElSelect v-model="startForm.teamId" placeholder="请选择小组" style="width: 100%">
            <ElOption
              v-for="team in teams"
              :key="team.id"
              :label="isTeamDiving(team.id) ? `${team.name}（未收潜中）` : team.name"
              :value="team.id"
              :disabled="isTeamDiving(team.id)"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="开始时刻" required>
          <ElDatePicker
            v-model="startForm.startTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时刻"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="预计结束" required>
          <ElDatePicker
            v-model="startForm.plannedEndTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择预计结束时刻"
            style="width: 100%"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="startVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="startSubmitting" @click="submitStart">确认开潜</ElButton>
      </template>
    </ElDialog>

    <!-- 收潜弹窗 -->
    <ElDialog v-model="endVisible" title="收潜" width="460px">
      <ElForm label-width="110px">
        <ElFormItem label="小组">
          <span>{{ endRecord?.teamName }}</span>
        </ElFormItem>
        <ElFormItem label="开始时刻">
          <span>{{ endRecord?.startTime }}</span>
        </ElFormItem>
        <ElFormItem label="实际结束" required>
          <ElDatePicker
            v-model="endForm.actualEndTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择实际结束时刻"
            style="width: 100%"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="endVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="endSubmitting" @click="submitEnd">确认收潜</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.dive-list {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.rule-alert {
  margin-bottom: 16px;
}

.overdue-text {
  color: #dc2626;
  font-weight: 600;
}

.muted {
  color: #94a3b8;
}
</style>
