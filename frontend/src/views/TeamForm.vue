<script setup lang="ts">import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElButton, ElMessage, ElSwitch } from 'element-plus';
import { teamApi } from '@/api';
const router = useRouter();
const route = useRoute();
const isEdit = computed(() => !!route.params.id);
const form = ref({
 name: '',
 memberCount: 0,
 minDepth: 0,
 maxDepth: 30,
 certifiedDepth: 30,
 depthChangeNotify: true,
 description: ''
});
const depthError = ref('');
const rules = {
 name: [{ required: true, message: '小组名称不能为空', trigger: 'blur' }],
 maxDepth: [{ required: true, message: '最大允许深度不能为空', trigger: 'blur' },
 { type: 'number', min: 1, message: '最大允许深度必须大于0', trigger: 'blur' },
 { type: 'number', max: 500, message: '最大允许深度不能超过500米', trigger: 'blur' }],
 certifiedDepth: [{ required: true, message: '持证最大深度不能为空', trigger: 'blur' },
 { type: 'number', min: 1, message: '持证最大深度必须大于0', trigger: 'blur' }]
};
const validateDepth = () => {
 if (form.value.minDepth >= form.value.maxDepth) {
 depthError.value = '最小深度必须小于最大深度';
 return false;
 }
 if (form.value.maxDepth > form.value.certifiedDepth) {
 depthError.value = '最大允许深度不能超过持证深度，证深不符禁止保存';
 return false;
 }
 depthError.value = '';
 return true;
};
watch([() => form.value.minDepth, () => form.value.maxDepth, () => form.value.certifiedDepth], () => {
 validateDepth();
});
const loadTeam = async () => {
 if (!isEdit.value)
 return;
 try {
 const data = await teamApi.get(Number(route.params.id));
 form.value = {
 name: data.name,
 memberCount: data.memberCount || 0,
 minDepth: data.minDepth || 0,
 maxDepth: data.maxDepth,
 certifiedDepth: data.certifiedDepth,
 depthChangeNotify: data.depthChangeNotify ?? true,
 description: data.description || ''
 };
 } catch (error) {
 ElMessage.error((error as Error).message);
 router.push('/team');
 }
};
const handleSubmit = async () => {
 if (!validateDepth()) {
 ElMessage.error(depthError.value);
 return;
 }
 try {
 if (isEdit.value) {
 await teamApi.update(Number(route.params.id), form.value);
 ElMessage.success('更新成功');
 }
 else {
 await teamApi.create(form.value);
 ElMessage.success('创建成功');
 }
 router.push('/team');
 } catch (error) {
 ElMessage.error((error as Error).message);
 }
};
const handleCancel = () => {
 router.push('/team');
};
onMounted(() => {
 loadTeam();
});
</script>

<template>
  <div class="team-form">
    <h2>{{ isEdit ? '编辑小组' : '新增小组' }}</h2>
    
    <ElForm :model="form" :rules="rules" label-width="140px" class="form-content">
      <ElFormItem label="小组名称" prop="name">
        <ElInput v-model="form.name" placeholder="请输入小组名称" />
      </ElFormItem>
      
      <ElFormItem label="成员数量" prop="memberCount">
        <ElInputNumber 
          v-model="form.memberCount" 
          :min="0" 
          placeholder="请输入成员数量"
          style="width: 100%"
        />
      </ElFormItem>
      
      <div class="depth-row">
        <ElFormItem label="最小允许深度(m)" prop="minDepth">
          <ElInputNumber 
            v-model="form.minDepth" 
            :min="0" 
            :max="form.maxDepth - 1"
            placeholder="最小深度"
            style="width: 100%"
          />
        </ElFormItem>
        
        <ElFormItem label="最大允许深度(m)" prop="maxDepth">
          <ElInputNumber
            v-model="form.maxDepth"
            :min="form.minDepth + 1"
            :max="form.certifiedDepth"
            placeholder="最大深度"
            style="width: 100%"
          />
        </ElFormItem>
      </div>

      <ElFormItem label="持证最大深度(m)" prop="certifiedDepth">
        <ElInputNumber
          v-model="form.certifiedDepth"
          :min="1"
          :max="500"
          placeholder="持证深度"
          style="width: 100%"
        />
        <span v-if="depthError" class="error-text">{{ depthError }}</span>
      </ElFormItem>
      
      <ElFormItem label="深度变动通知" prop="depthChangeNotify">
        <ElSwitch
          v-model="form.depthChangeNotify"
          active-text="订阅"
          inactive-text="退订"
        />
        <span class="notify-hint">订阅后，深度下调导致装备解绑时会收到站内通知</span>
      </ElFormItem>

      <ElFormItem label="描述" prop="description">
        <ElInput v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
      </ElFormItem>
      
      <ElFormItem>
        <ElButton type="primary" @click="handleSubmit">{{ isEdit ? '保存' : '创建' }}</ElButton>
        <ElButton @click="handleCancel">取消</ElButton>
      </ElFormItem>
    </ElForm>
  </div>
</template>

<style scoped>
.team-form {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.team-form h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0A2463;
  margin-bottom: 24px;
}

.form-content {
  max-width: 600px;
}

.depth-row {
  display: flex;
  gap: 20px;
}

.depth-row .el-form-item {
  flex: 1;
}

.error-text {
  display: block;
  color: #F56C6C;
  font-size: 12px;
  margin-top: 4px;
}

.notify-hint {
  margin-left: 12px;
  color: #94A3B8;
  font-size: 12px;
}
</style>