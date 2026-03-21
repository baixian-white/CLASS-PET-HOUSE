<template>
  <div class="min-h-screen bg-theme flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">
      <h1 class="text-2xl font-bold text-center text-gray-700 mb-2">账号激活</h1>
      <p class="text-center text-gray-400 mb-6">请输入激活码完成账号激活</p>

      <div v-if="error" class="bg-red-50 text-red-500 text-sm p-3 rounded-lg mb-4">{{ error }}</div>
      <div v-if="success" class="bg-green-50 text-green-500 text-sm p-3 rounded-lg mb-4">{{ success }}</div>

      <div class="space-y-4">
        <input v-model="code" type="text" placeholder="激活码"
          class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent outline-none" />
        <button @click="handleActivate" :disabled="loading"
          class="w-full py-3 bg-accent bg-accent-hover text-white rounded-xl font-medium transition active:scale-95 disabled:opacity-50">
          {{ loading ? '激活中...' : '激活账号' }}
        </button>
      </div>

      <div class="mt-4 text-center text-sm text-gray-400">
        <router-link to="/login" class="hover:text-accent">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../utils/api'

const router = useRouter()
const auth = useAuthStore()

const code = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

onMounted(() => {
  if (!auth.token) router.replace('/login')
})

async function handleActivate() {
  if (!code.value) {
    error.value = '请输入激活码'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const data = await api.post('/auth/activate', { code: code.value })
    if (data?.user) auth.user = data.user
    success.value = data?.message || '激活成功'
    router.push('/')
  } catch (err) {
    error.value = err?.error || '激活失败'
  } finally {
    loading.value = false
  }
}
</script>
