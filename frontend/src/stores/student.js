import { defineStore } from 'pinia'
import axios from 'axios'

// 学生端独立 axios 实例，使用 studentToken
const studentApi = axios.create({
  baseURL: '/api/student-portal',
  timeout: 10000
})

studentApi.interceptors.request.use(config => {
  const token = localStorage.getItem('studentToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

studentApi.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    const url = err.config?.url
    if (status === 401 && !url?.includes('/login') && !url?.includes('/register') && !url?.includes('/invite/check')) {
      localStorage.removeItem('studentToken')
      window.location.href = '/login'
    }
    return Promise.reject(err.response?.data || err)
  }
)

export { studentApi }

export const useStudentStore = defineStore('student', {
  state: () => ({
    token: localStorage.getItem('studentToken') || null,
    me: null,          // { id, name, pet_type, pet_name, food_count, badges, ... }
    currentClass: null, // { id, name, growth_stages }
    history: [],
    historyTotal: 0,
    leaderboard: [],
    shop: []
  }),
  getters: {
    isLoggedIn: s => !!s.token,
    myRankFood: s => {
      if (!s.me) return null
      const sorted = [...s.leaderboard].sort((a, b) => b.food_count - a.food_count)
      return sorted.findIndex(x => x.id === s.me.id) + 1
    }
  },
  actions: {
    async checkInviteCode(invite_code) {
      return studentApi.post('/invite/check', { invite_code })
    },
    async sendRegisterCode(phone) {
      return studentApi.post('/send-register-code', { phone })
    },
    async register(payload) {
      const data = await studentApi.post('/register', payload)
      this.token = data.token
      this.me = data.student
      localStorage.setItem('studentToken', data.token)
      return data
    },
    async login(username, password) {
      const data = await studentApi.post('/login', { username, password })
      this.token = data.token
      this.me = data.student
      localStorage.setItem('studentToken', data.token)
      return data
    },
    logout() {
      this.token = null
      this.me = null
      this.currentClass = null
      this.history = []
      this.leaderboard = []
      this.shop = []
      localStorage.removeItem('studentToken')
    },
    async fetchMe() {
      const data = await studentApi.get('/me')
      this.me = data.student
      this.currentClass = data.class
    },
    async fetchHistory(offset = 0) {
      const data = await studentApi.get(`/history?limit=50&offset=${offset}`)
      this.history = data.rows
      this.historyTotal = data.count
    },
    async fetchLeaderboard() {
      this.leaderboard = await studentApi.get('/leaderboard')
    },
    async fetchShop() {
      this.shop = await studentApi.get('/shop')
    }
  }
})
