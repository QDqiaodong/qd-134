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
  description: string
  createdAt: string
  updatedAt: string
  // 当前占用（ACTIVE 绑定）装备的登记重量合计，已解绑的不计入，未填重量按 0
  totalWeight: number
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

export type TeamPayload = Omit<Team, 'id' | 'createdAt' | 'updatedAt' | 'totalWeight'>

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

export default api