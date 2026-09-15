import { createRouter, createWebHistory } from 'vue-router'
import EquipmentList from '@/views/EquipmentList.vue'
import EquipmentForm from '@/views/EquipmentForm.vue'
import TeamList from '@/views/TeamList.vue'
import TeamForm from '@/views/TeamForm.vue'
import BindingManager from '@/views/BindingManager.vue'
import BindingList from '@/views/BindingList.vue'
import EquipmentFilter from '@/views/EquipmentFilter.vue'
import NotificationList from '@/views/NotificationList.vue'
import DiveRecordList from '@/views/DiveRecordList.vue'

const routes = [
  { path: '/', redirect: '/equipment' },
  { path: '/equipment', component: EquipmentList },
  { path: '/equipment/add', component: EquipmentForm },
  { path: '/equipment/edit/:id', component: EquipmentForm },
  { path: '/team', component: TeamList },
  { path: '/team/add', component: TeamForm },
  { path: '/team/edit/:id', component: TeamForm },
  { path: '/binding', component: BindingManager },
  { path: '/binding-records', component: BindingList },
  { path: '/filter', component: EquipmentFilter },
  { path: '/dive', component: DiveRecordList },
  { path: '/notification', component: NotificationList }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router