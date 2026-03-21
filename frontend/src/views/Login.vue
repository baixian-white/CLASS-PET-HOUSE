<template>
  <div class="min-h-screen bg-theme flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">

      <!-- Logo -->
      <h1 class="text-2xl font-bold text-center text-gray-700 mb-6 flex items-center justify-center gap-2">
        <img src="/logo.png" alt="logo" class="w-14 h-14 sm:w-16 sm:h-16" />
        石榴果宠物屋
      </h1>

      <!-- 角色切换 Tab -->
      <div class="flex bg-gray-100 rounded-xl p-1 mb-6">
        <button @click="role = 'student'"
          :class="role === 'student' ? 'bg-white shadow text-cyan-600 font-black' : 'text-gray-400 hover:text-gray-600'"
          class="flex-1 py-2 rounded-lg text-sm font-bold transition-all flex items-center justify-center gap-1.5">
          🐾 学生登录
        </button>
        <button @click="role = 'teacher'"
          :class="role === 'teacher' ? 'bg-white shadow text-accent font-black' : 'text-gray-400 hover:text-gray-600'"
          class="flex-1 py-2 rounded-lg text-sm font-bold transition-all flex items-center justify-center gap-1.5">
          📚 教师登录
        </button>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="bg-red-50 text-red-500 text-sm p-3 rounded-lg mb-4">{{ error }}</div>

      <!-- 登录表单 -->
      <form class="space-y-4" autocomplete="off" @submit.prevent="handleLogin">
        <div>
          <label class="block text-sm text-gray-500 mb-1">用户名</label>
          <input v-model="username" type="text" :placeholder="role === 'student' ? '请输入学生用户名' : '请输入教师用户名'"
            autocomplete="off" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">密码</label>
          <input v-model="password" type="password" placeholder="请输入密码"
            autocomplete="new-password" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <button type="submit" :disabled="loading"
          class="w-full py-3 text-white rounded-xl font-medium transition active:scale-95 disabled:opacity-50"
          :class="role === 'student' ? 'bg-cyan-500 hover:bg-cyan-600' : 'bg-accent bg-accent-hover'">
          {{ loading ? '登录中...' : (role === 'student' ? '🐾 进入宠物屋' : '📚 进入教师端') }}
        </button>
      </form>

      <!-- 教师端附加链接 -->
      <div v-if="role === 'teacher'" class="mt-4 text-center text-sm text-gray-400 space-x-4">
        <router-link to="/register" class="hover:text-accent">注册账号</router-link>
        <router-link to="/reset-password" class="hover:text-accent">忘记密码</router-link>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useStudentStore } from '../stores/student'

const router = useRouter()
const auth = useAuthStore()
const studentStore = useStudentStore()

const role = ref('student')   // 默认显示学生登录
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = '请填写用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    if (role.value === 'student') {
      await studentStore.login(username.value, password.value)
      router.push('/student')
    } else {
      const data = await auth.login(username.value, password.value)
      if (data.status === 'not_activated') {
        router.push('/activate')
      } else {
        router.push('/')
      }
    }
  } catch (err) {
    error.value = err.error || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>
