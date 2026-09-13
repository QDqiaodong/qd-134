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
  description: string
  createdAt: string
  updatedAt: string
}

export interface Binding {
  id: number
  teamId: number
  equipmentId: number
  boundAt: string
  status: string
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

export const teamApi = {
  list: (page = 0, size = 20, keyword?: string) =>
    request.get<PageResponse<Team>>('/team', { params: { page, size, keyword } }),
  get: (id: number) => request.get<Team>(`/team/${id}`),
  create: (data: Omit<Team, 'id' | 'createdAt' | 'updatedAt'>) =>
    request.post<Team>('/team', data),
  update: (id: number, data: Omit<Team, 'id' | 'createdAt' | 'updatedAt'>) =>
    request.put<Team>(`/team/${id}`, data),
  delete: (id: number) => request.delete<void>(`/team/${id}`)
}

export const bindingApi = {
  list: (page = 0, size = 20) =>
    request.get<PageResponse<Binding>>('/binding', { params: { page, size } }),
  get: (id: number) => request.get<Binding>(`/binding/${id}`),
  getByTeam: (teamId: number) => request.get<Equipment[]>(`/binding/team/${teamId}`),
  create: (data: { teamId: number; equipmentId: number }) =>
    request.post<Binding>('/binding', data),
  delete: (id: number) => request.delete<void>(`/binding/${id}`),
  sync: (data: { teamId: number; newMaxDepth: number; operator?: string }) =>
    request.post<{ removedCount: number; removedEquipments: number[] }>('/binding/sync', data)
}

export default api