import { createRouter, createWebHistory } from 'vue-router'
import { useClassStore } from '../stores/class'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/reset-password', name: 'ResetPassword', component: () => import('../views/ResetPassword.vue') },
  { path: '/admin', name: 'AdminDashboard', component: () => import('../views/AdminDashboard.vue') },
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
