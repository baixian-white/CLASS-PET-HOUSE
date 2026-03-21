import { createRouter, createWebHistory } from 'vue-router'
import { useClassStore } from '../stores/class'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/reset-password', name: 'ResetPassword', component: () => import('../views/ResetPassword.vue') },
  { path: '/admin', name: 'AdminDashboard', component: () => import('../views/AdminDashboard.vue') },

  // 学生端
  { path: '/student/login', name: 'StudentLogin', component: () => import('../views/StudentLogin.vue') },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    meta: { requiresStudentAuth: true },
    children: [
      { path: '', name: 'StudentHome', component: () => import('../views/student/StudentHome.vue') },
      { path: 'leaderboard', name: 'StudentLeaderboard', component: () => import('../views/student/StudentLeaderboard.vue') },
      { path: 'history', name: 'StudentHistory', component: () => import('../views/student/StudentHistory.vue') },
      { path: 'shop', name: 'StudentShop', component: () => import('../views/student/StudentShop.vue') },
    ]
  },

  // 老师端
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'settings', name: 'Settings', component: () => import('../views/Settings.vue') },
      { path: 'leaderboard', name: 'Leaderboard', component: () => import('../views/Leaderboard.vue') },
      { path: 'history', name: 'History', component: () => import('../views/HistoryView.vue') },
      { path: 'shop', name: 'Shop', component: () => import('../views/Shop.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  // 学生端路由守卫
  if (to.meta.requiresStudentAuth) {
    const studentToken = localStorage.getItem('studentToken')
    if (!studentToken) return '/student/login'
    return true
  }

  // 老师端路由守卫
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) return '/login'

  if (to.meta.requiresAuth && token) {
    const classStore = useClassStore()
    if (!classStore.initialized) {
      try {
        await classStore.fetchClasses()
      } catch {
        // ignore init errors here
      }
    }
  }
  return true
})

export default router
