import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

function redirectTo(path) {
  if (window.location.pathname !== path) {
    window.location.href = path
  }
}

// 请求拦截器 - 自动带 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器
api.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    const data = err.response?.data
    const url = err.config?.url

    if (status === 403 && data?.status === 'not_activated') {
      redirectTo('/activate')
    }

    // 只有在非登录接口报错 401 时才强制跳回登录页
    if (status === 401 && !url?.includes('/auth/login')) {
      localStorage.removeItem('token')
      redirectTo('/login')
    }

    return Promise.reject(data || err)
  }
)

export default api
