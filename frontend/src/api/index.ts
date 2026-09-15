import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.response.use(
  (response) => {
    if (response.data.code === 200) {
      return response.data.data
    } else {
      throw new Error(response.data.message)
    }
  },
  (error) => {
    const message = error.response?.data?.message || '请求失败'
    throw new Error(message)
  }
)

const request = {
  get: async <T>(url: string, config?: any) => {
    return (await api.get(url, config)) as T
  },
  post: async <T>(url: string, data?: any, config?: any) => {
    return (await api.post(url, data, config)) as T
  },
  put: async <T>(url: string, data?: any, config?: any) => {
    return (await api.put(url, data, config)) as T
  },
  delete: async <T>(url: string, config?: any) => {
    return (await api.delete(url, config)) as T
  }
}

export interface Equipment {
  id: number
  code: string
  name: string
  maxDepth: number
  weight: number
  specification: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface Team {
  id: number
  name: string
  memberCount: number
  minDepth: number
  maxDepth: number
  certifiedDepth: number
  depthChangeNotify: boolean
  // 剧组状态：FILMING=拍摄中，WRAPPED=已收队（失效剧组，禁止绑定装备）
  status: string
  description: string
  createdAt: string
  updatedAt: string
  // 当前占用（ACTIVE 绑定）装备的登记重量合计，已解绑的不计入，未填重量按 0
  totalWeight: number
  // 当前未收潜记录；没有在潜记录时为 null（真正的在潜互斥状态，不是一个时间列）
  activeDive: DiveRecord | null
}

export interface Binding {
  id: number
  teamId: number
  equipmentId: number
  boundAt: string
  status: string
}

export interface BindingRecord {
  id: number
  teamId: number
  teamName: string
  teamCertifiedDepth: number
  equipmentId: number
  equipmentName: string
  equipmentMaxDepth: number
  overCertified: boolean
  boundAt: string
  status: string
}

export interface Notification {
  id: number
  teamId: number
  teamName: string
  title: string
  content: string
  notifyType: string
  removedCount: number
  equipmentNames: string
  readStatus: boolean
  readAt: string | null
  createdAt: string
}

export interface DiveRecord {
  id: number
  teamId: number
  teamName: string
  startTime: string
  plannedEndTime: string
  actualEndTime: string | null
  status: string
  createdAt: string
  open: boolean
  overdue: boolean
}

export interface SeaConditionReport {
  id: number
  reportDate: string
  waveHeight: number
  visibility: number
  // 当天能否下水：false 时当天所有小组开潜都会被后端拦截
  divable: boolean
  reporter: string
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const equipmentApi = {
  list: (page = 0, size = 20, keyword?: string) =>
    request.get<PageResponse<Equipment>>('/equipment', { params: { page, size, keyword } }),
  get: (id: number) => request.get<Equipment>(`/equipment/${id}`),
  create: (data: Omit<Equipment, 'id' | 'createdAt' | 'updatedAt'>) =>
    request.post<Equipment>('/equipment', data),
  update: (id: number, data: Omit<Equipment, 'id' | 'createdAt' | 'updatedAt'>) =>
    request.put<Equipment>(`/equipment/${id}`, data),
  delete: (id: number) => request.delete<void>(`/equipment/${id}`),
  filter: (minDepth: number, maxDepth: number, page = 0, size = 20) =>
    request.get<PageResponse<Equipment>>('/equipment/filter', { params: { minDepth, maxDepth, page, size } })
}

export type TeamPayload = Omit<Team, 'id' | 'createdAt' | 'updatedAt' | 'totalWeight' | 'activeDive'>

export const teamApi = {
  list: (page = 0, size = 20, keyword?: string) =>
    request.get<PageResponse<Team>>('/team', { params: { page, size, keyword } }),
  get: (id: number) => request.get<Team>(`/team/${id}`),
  create: (data: TeamPayload) =>
    request.post<Team>('/team', data),
  update: (id: number, data: TeamPayload) =>
    request.put<Team>(`/team/${id}`, data),
  delete: (id: number) => request.delete<void>(`/team/${id}`)
}

export const bindingApi = {
  list: (page = 0, size = 20, overCertified?: boolean) =>
    request.get<PageResponse<BindingRecord>>('/binding', { params: { page, size, overCertified } }),
  get: (id: number) => request.get<Binding>(`/binding/${id}`),
  getByTeam: (teamId: number) => request.get<Equipment[]>(`/binding/team/${teamId}`),
  create: (data: { teamId: number; equipmentId: number }) =>
    request.post<Binding>('/binding', data),
  delete: (id: number) => request.delete<void>(`/binding/${id}`),
  sync: (data: { teamId: number; newMaxDepth: number; operator?: string }) =>
    request.post<{ removedCount: number; removedEquipments: number[] }>('/binding/sync', data)
}

export const notificationApi = {
  list: (page = 0, size = 20, teamId?: number, readStatus?: boolean) =>
    request.get<PageResponse<Notification>>('/notification', {
      params: { page, size, teamId, readStatus }
    }),
  get: (id: number) => request.get<Notification>(`/notification/${id}`),
  markRead: (id: number) => request.put<Notification>(`/notification/${id}/read`),
  unreadCount: (teamId?: number) =>
    request.get<number>('/notification/unread-count', { params: { teamId } })
}

export const diveApi = {
  list: (page = 0, size = 20, teamId?: number | null, status?: string | null) =>
    request.get<PageResponse<DiveRecord>>('/dive', { params: { page, size, teamId, status } }),
  activeByTeam: (teamId: number) =>
    request.get<DiveRecord | null>(`/dive/active/${teamId}`),
  start: (data: { teamId: number; startTime: string; plannedEndTime: string }) =>
    request.post<DiveRecord>('/dive/start', data),
  end: (id: number, actualEndTime: string) =>
    request.put<DiveRecord>(`/dive/${id}/end`, { actualEndTime })
}

export const seaConditionApi = {
  list: (page = 0, size = 20) =>
    request.get<PageResponse<SeaConditionReport>>('/sea-condition', { params: { page, size } }),
  // 查某天海况单，date 为 YYYY-MM-DD；不传取今天，未交单返回 null
  byDate: (date?: string) =>
    request.get<SeaConditionReport | null>('/sea-condition/by-date', { params: { date } }),
  today: () => request.get<SeaConditionReport | null>('/sea-condition/today'),
  // 交海况单；同一天只能落一张，重复提交后端拒绝并带回当天已记的浪高、能见度
  report: (data: {
    reportDate: string
    waveHeight: number
    visibility: number
    divable: boolean
    reporter?: string
    remark?: string
  }) => request.post<SeaConditionReport>('/sea-condition', data)
}

export default api