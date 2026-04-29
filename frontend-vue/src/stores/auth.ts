import { defineStore } from 'pinia'

import { http } from '../services/http'

export type User = {
  id: number
  username: string
  role: string
  phone?: string
  address?: string
  gender?: string
  birthDate?: string
}

type LoginResponse = {
  access_token: string
  token_type: string
  user: User
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('access_token') || '',
    user: (() => {
      const raw = localStorage.getItem('pa_user') || ''
      if (!raw) return null as User | null
      try {
        return JSON.parse(raw) as User
      } catch {
        return null as User | null
      }
    })(),
    loading: false,
  }),
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('access_token', token)
    },
    setUser(user: User | null) {
      this.user = user
      if (!user) {
        localStorage.removeItem('pa_user')
        return
      }
      localStorage.setItem('pa_user', JSON.stringify(user))
    },
    clearToken() {
      this.token = ''
      this.user = null
      localStorage.removeItem('access_token')
      localStorage.removeItem('pa_user')
    },
    async login(username: string, password: string) {
      this.loading = true
      try {
        const res = await http.post<LoginResponse>('/auth/login', { username, password })
        this.setToken(res.data.access_token)
        this.setUser(res.data.user)
      } finally {
        this.loading = false
      }
    },
    async fetchMe() {
      const res = await http.get('/auth/me')
      this.setUser(res.data as User)
      return res.data as User
    },
    logout() {
      this.clearToken()
    },
  },
})

