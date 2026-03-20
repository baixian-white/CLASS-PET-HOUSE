<template>
  <div class="min-h-screen bg-theme flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">
      <h1 class="text-2xl font-bold text-center text-gray-700 mb-2">🎫 账号激活</h1>
      <p class="text-center text-gray-400 mb-6">
        <span v-if="auth.user?.username">当前账号：{{ auth.user.username }}</span>
        <span v-else>请输入激活码完成账号激活</span>
      </p>

      <div v-if="error" class="bg-red-50 text-red-500 text-sm p-3 rounded-lg mb-4">{{ error }}</div>
      <div v-if="success" class="bg-green-50 text-green-600 text-sm p-3 rounded-lg mb-4">{{ success }}</div>

      <div class="space-y-4">
        <div>
          <label class="block text-sm text-gray-500 mb-1">激活码</label>
          <input
            v-model="code"
            type="text"
            placeholder="请输入激活码"
            @keyup.enter="handleActivate"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent outline-none transition uppercase"
          />
        </div>

        <button
          @click="handleActivate"
          :disabled="loading"
          class="w-full py-3 bg-accent text-white rounded-xl font-medium transition active:scale-95 disabled:opacity-50"
        >
          {{ loading ? '激活中...' : '立即激活' }}
        </button>

        <button
          @click="handleLogout"
          type="button"
          class="w-full py-3 bg-gray-100 text-gray-600 rounded-xl font-medium transition active:scale-95"
        >
          切换账号
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const code = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

onMounted(async () => {
  if (!auth.token) {
    router.replace('/login')
    return
  }

  if (!auth.user) {
    try {
      await auth.fetchUser()
    } catch {
      return
    }
  }

  if (auth.isActivated) {
    router.replace('/')
  }
})

async function handleActivate() {
  if (!code.value.trim()) {
    error.value = '请输入激活码'
    return
  }

  loading.value = true
  error.value = ''
  success.value = ''

  try {
    await auth.activate(code.value.trim())
    success.value = '账号激活成功，正在进入主页...'
    setTimeout(() => {
      router.replace('/')
    }, 600)
  } catch (err) {
    error.value = err.error || '激活失败'
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  auth.logout()
  router.replace('/login')
}
</script>
