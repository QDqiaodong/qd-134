<script setup lang="ts">import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElButton, ElMessage } from 'element-plus';
import { equipmentApi } from '@/api';
const router = useRouter();
const route = useRoute();
const isEdit = computed(() => !!route.params.id);
const form = ref({
 code: '',
 name: '',
 maxDepth: 30,
 weight: 0,
 specification: '',
 description: ''
});
const rules = {
 code: [{ required: true, message: '装备编号不能为空', trigger: 'blur' }],
 name: [{ required: true, message: '装备名称不能为空', trigger: 'blur' }],
 maxDepth: [{ required: true, message: '最大耐压深度不能为空', trigger: 'blur' },
 { type: 'number', min: 1, message: '最大耐压深度必须大于0', trigger: 'blur' }],
 weight: [{ type: 'number', min: 0, message: '重量不能为负数', trigger: 'blur' }]
};
const loadEquipment = async () => {
 if (!isEdit.value)
 return;
 try {
 const data = await equipmentApi.get(Number(route.params.id));
 form.value = {
 code: data.code,
 name: data.name,
 maxDepth: data.maxDepth,
 weight: data.weight || 0,
 specification: data.specification || '',
 description: data.description || ''
 };
 } catch (error) {
 ElMessage.error((error as Error).message);
 router.push('/equipment');
 }
};
const handleSubmit = async () => {
 try {
 if (isEdit.value) {
 await equipmentApi.update(Number(route.params.id), form.value);
 ElMessage.success('更新成功');
 }
 else {
 await equipmentApi.create(form.value);
 ElMessage.success('创建成功');
 }
 router.push('/equipment');
 } catch (error) {
 ElMessage.error((error as Error).message);
 }
};
const handleCancel = () => {
 router.push('/equipment');
};
onMounted(() => {
 loadEquipment();
});
</script>

<template>
  <div class="equipment-form">
    <h2>{{ isEdit ? '编辑装备' : '新增装备' }}</h2>
    
    <ElForm :model="form" :rules="rules" label-width="120px" class="form-content">
      <ElFormItem label="装备编号" prop="code">
        <ElInput v-model="form.code" placeholder="请输入装备编号" />
      </ElFormItem>
      
      <ElFormItem label="装备名称" prop="name">
        <ElInput v-model="form.name" placeholder="请输入装备名称" />
      </ElFormItem>
      
      <ElFormItem label="最大耐压深度(m)" prop="maxDepth">
        <ElInputNumber 
          v-model="form.maxDepth" 
          :min="1" 
          :max="500" 
          placeholder="请输入最大耐压深度"
          style="width: 100%"
        />
      </ElFormItem>
      
      <ElFormItem label="重量(kg)" prop="weight">
        <ElInputNumber 
          v-model="form.weight" 
          :min="0" 
          :precision="2" 
          placeholder="请输入重量"
          style="width: 100%"
        />
      </ElFormItem>
      
      <ElFormItem label="规格型号" prop="specification">
        <ElInput v-model="form.specification" placeholder="请输入规格型号" />
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
.equipment-form {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.equipment-form h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0A2463;
  margin-bottom: 24px;
}

.form-content {
  max-width: 600px;
}
</style>