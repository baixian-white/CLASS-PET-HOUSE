import { defineStore } from 'pinia'
import api from '../utils/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null
  }),
  getters: {
    isLoggedIn: s => !!s.token,
    isActivated: s => s.user?.is_activated ?? false
  },
  actions: {
    async login(username, password) {
      const data = await api.post('/auth/login', { username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      return data
    },
    async sendRegisterCode(phone) {
      return api.post('/auth/send-register-code', { phone })
    },
    async register(payload) {
      const data = await api.post('/auth/register', payload)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      return data
    },
    async fetchUser() {
      const data = await api.get('/auth/me')
      this.user = data.user
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
    }
  }
})
