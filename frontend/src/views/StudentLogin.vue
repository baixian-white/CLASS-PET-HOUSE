<template>
  <div class="min-h-screen flex items-center justify-center p-4" style="background: linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 40%, #fce4ec 100%)">
    <div class="bg-white/90 backdrop-blur-xl rounded-3xl shadow-2xl p-8 w-full max-w-sm border border-white">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-400 to-teal-500 shadow-lg shadow-cyan-200 mb-4">
          <img src="/logo.png" alt="logo" class="w-14 h-14" />
        </div>
        <h1 class="text-2xl font-black text-slate-800">石榴果宠物屋</h1>
        <p class="text-sm text-slate-400 mt-1 font-medium">学生登录</p>
      </div>

      <!-- Error -->
      <div v-if="error" class="bg-red-50 border border-red-100 text-red-500 text-sm p-3 rounded-2xl mb-4 font-bold">
        {{ error }}
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4" autocomplete="off">
        <div>
          <label class="block text-xs font-bold text-slate-500 mb-1.5 ml-1">学生用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名"
            autocomplete="off" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true"
            class="w-full px-4 py-3 rounded-2xl border-2 border-slate-100 bg-slate-50 text-sm font-bold text-slate-700 outline-none focus:border-cyan-400 focus:bg-white transition-all placeholder:text-slate-300" />
        </div>
        <div>
          <label class="block text-xs font-bold text-slate-500 mb-1.5 ml-1">密码</label>
          <input v-model="password" type="password" placeholder="请输入密码"
            autocomplete="new-password"
            data-1p-ignore="true" data-lpignore="true"
            class="w-full px-4 py-3 rounded-2xl border-2 border-slate-100 bg-slate-50 text-sm font-bold text-slate-700 outline-none focus:border-cyan-400 focus:bg-white transition-all placeholder:text-slate-300" />
        </div>
        <button type="submit" :disabled="loading"
          class="w-full py-3 rounded-2xl bg-gradient-to-r from-cyan-400 to-teal-500 text-white font-black text-base shadow-lg shadow-cyan-200 hover:shadow-cyan-300 hover:-translate-y-0.5 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
          {{ loading ? '登录中...' : '🐾 进入宠物屋' }}
        </button>
      </form>

      <div class="mt-6 text-center">
        <router-link to="/login" class="text-xs text-slate-400 hover:text-cyan-500 transition-colors font-medium">
          老师登录 →
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStudentStore } from '../stores/student'

const router = useRouter()
const studentStore = useStudentStore()
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
    await studentStore.login(username.value, password.value)
    router.push('/student')
  } catch (err) {
    error.value = err.error || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>
