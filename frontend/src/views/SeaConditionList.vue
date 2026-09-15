<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElPagination, ElMessage, ElTag,
  ElDialog, ElForm, ElFormItem, ElDatePicker, ElInputNumber, ElInput, ElRadioGroup, ElRadio, ElAlert
} from 'element-plus'
import { seaConditionApi, type SeaConditionReport } from '@/api'

const reports = ref<SeaConditionReport[]>([])
const loading = ref(false)
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)

// 今天的海况单；null 表示今天还没交单
const todayReport = ref<SeaConditionReport | null>(null)
const todayDateStr = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// ---------- 交单弹窗 ----------
const reportVisible = ref(false)
const reportSubmitting = ref(false)
const reportForm = reactive({
  reportDate: todayDateStr(),
  waveHeight: 1.0,
  visibility: 10.0,
  divable: true,
  reporter: '',
  remark: ''
})

const openReportDialog = () => {
  reportForm.reportDate = todayDateStr()
  reportForm.waveHeight = 1.0
  reportForm.visibility = 10.0
  reportForm.divable = true
  reportForm.reporter = ''
  reportForm.remark = ''
  reportVisible.value = true
}

const submitReport = async () => {
  if (!reportForm.reportDate) {
    ElMessage.error('请选择海况单日期')
    return
  }
  if (reportForm.waveHeight == null || Number.isNaN(Number(reportForm.waveHeight))) {
    ElMessage.error('请填写浪高')
    return
  }
  if (reportForm.visibility == null || Number.isNaN(Number(reportForm.visibility))) {
    ElMessage.error('请填写能见度')
    return
  }
  if (Number(reportForm.waveHeight) < 0 || Number(reportForm.visibility) < 0) {
    ElMessage.error('浪高和能见度不能为负数')
    return
  }

  reportSubmitting.value = true
  try {
    await seaConditionApi.report({
      reportDate: reportForm.reportDate,
      waveHeight: Number(reportForm.waveHeight),
      visibility: Number(reportForm.visibility),
      divable: reportForm.divable,
      reporter: reportForm.reporter || undefined,
      remark: reportForm.remark || undefined
    })
    ElMessage.success('海况单已登记')
    reportVisible.value = false
    await Promise.all([loadToday(), loadReports()])
  } catch (error) {
    // 同一天已有海况单时，后端提示中带着浪高、能见度；刷新当天卡片让后到的人看出今天已记过
    ElMessage.error((error as Error).message)
    await Promise.all([loadToday(), loadReports()])
  } finally {
    reportSubmitting.value = false
  }
}

const loadToday = async () => {
  try {
    todayReport.value = await seaConditionApi.today()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const loadReports = async () => {
  loading.value = true
  try {
    const result = await seaConditionApi.list(currentPage.value, pageSize.value)
    reports.value = result.content
    total.value = result.totalElements
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

const refreshAll = () => Promise.all([loadToday(), loadReports()])

// 列表里的“今天”高亮
const isToday = (date: string) => date === todayDateStr()

const metricText = (v: number | null | undefined) => {
  const value = Number(v ?? 0)
  return Number.isFinite(value) ? value.toFixed(2) : '0.00'
}

const todayCardType = computed(() => todayReport.value
  ? (todayReport.value.divable ? 'success' : 'error')
  : 'warning')

onMounted(() => {
  loadToday()
  loadReports()
})
</script>

<template>
  <div class="sea-condition-list">
    <div class="toolbar">
      <ElButton type="primary" @click="openReportDialog">交今日海况单</ElButton>
      <ElButton @click="refreshAll">刷新</ElButton>
    </div>

    <!-- 今天的海况单：不能下水时当天开潜一律被后端挡住 -->
    <ElAlert
      :type="todayCardType"
      :closable="false"
      show-icon
      class="today-alert"
    >
      <template #title>
        <div v-if="todayReport" class="today-content">
          <span class="today-title">
            {{ todayReport.reportDate }} 海况单已记（填报人：{{ todayReport.reporter || '岸上人员' }}）
          </span>
          <span class="today-metric">浪高 {{ metricText(todayReport.waveHeight) }} 米</span>
          <span class="today-metric">能见度 {{ metricText(todayReport.visibility) }} 米</span>
          <ElTag :type="todayReport.divable ? 'success' : 'danger'" size="small">
            {{ todayReport.divable ? '今天可以下水' : '今天不能下水·开潜将被拦截' }}
          </ElTag>
        </div>
        <div v-else>
          今天（{{ todayDateStr() }}）还没有海况单，请岸上人员先交单。
        </div>
      </template>
    </ElAlert>

    <ElAlert
      type="info"
      :closable="false"
      title="每天只能落下一张海况单：同一天两人同时交单，后到的提交会被拒绝并看到当天已记的浪高、能见度；海况单记为不能下水的那天，任何小组开潜都会被挡住。"
      class="rule-alert"
    />

    <ElTable :data="reports" :loading="loading" border stripe style="width: 100%">
      <ElTableColumn label="日期" width="140">
        <template #default="{ row }">
          <span>{{ (row as SeaConditionReport).reportDate }}</span>
          <ElTag v-if="isToday((row as SeaConditionReport).reportDate)" type="primary" size="small" class="today-tag">
            今天
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="浪高(m)" width="120" align="right">
        <template #default="{ row }">{{ metricText((row as SeaConditionReport).waveHeight) }}</template>
      </ElTableColumn>
      <ElTableColumn label="能见度(m)" width="120" align="right">
        <template #default="{ row }">{{ metricText((row as SeaConditionReport).visibility) }}</template>
      </ElTableColumn>
      <ElTableColumn label="能否下水" width="180" align="center">
        <template #default="{ row }">
          <ElTag :type="(row as SeaConditionReport).divable ? 'success' : 'danger'">
            {{ (row as SeaConditionReport).divable ? '可以下水' : '不能下水' }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="reporter" label="填报人" width="140" />
      <ElTableColumn prop="remark" label="备注" show-overflow-tooltip />
      <ElTableColumn prop="createdAt" label="登记时间" width="180" />
    </ElTable>

    <ElPagination
      :current-page="currentPage + 1"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(page) => { currentPage = page - 1; loadReports() }"
      style="margin-top: 20px; text-align: right"
    />

    <!-- 交海况单弹窗 -->
    <ElDialog v-model="reportVisible" title="交海况单" width="480px">
      <ElForm label-width="110px">
        <ElFormItem label="日期" required>
          <ElDatePicker
            v-model="reportForm.reportDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="浪高(米)" required>
          <ElInputNumber
            v-model="reportForm.waveHeight"
            :min="0"
            :precision="2"
            :step="0.1"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="能见度(米)" required>
          <ElInputNumber
            v-model="reportForm.visibility"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="能否下水" required>
          <ElRadioGroup v-model="reportForm.divable">
            <ElRadio :value="true">可以下水</ElRadio>
            <ElRadio :value="false">不能下水（当天禁止开潜）</ElRadio>
          </ElRadioGroup>
        </ElFormItem>
        <ElFormItem label="填报人">
          <ElInput v-model="reportForm.reporter" placeholder="默认：岸上人员" maxlength="100" />
        </ElFormItem>
        <ElFormItem label="备注">
          <ElInput v-model="reportForm.remark" type="textarea" :rows="2" maxlength="500" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="reportVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="reportSubmitting" @click="submitReport">确认交单</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.sea-condition-list {
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

.today-alert {
  margin-bottom: 12px;
}

.today-content {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.today-title {
  font-weight: 600;
}

.today-metric {
  font-weight: 600;
}

.rule-alert {
  margin-bottom: 16px;
}

.today-tag {
  margin-left: 6px;
}
</style>
