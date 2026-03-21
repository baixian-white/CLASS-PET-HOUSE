<template>
  <div class="min-h-screen bg-theme flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">
      <h1 class="text-2xl font-bold text-center text-gray-700 mb-2">🐾 石榴果宠物屋</h1>
      <p class="text-center text-gray-400 mb-6">注册新账号</p>

      <div v-if="error" class="bg-red-50 text-red-500 text-sm p-3 rounded-lg mb-4">{{ error }}</div>

      <div class="space-y-4" autocomplete="off">
        <div>
          <label class="block text-sm text-gray-500 mb-1">用户名</label>
          <input v-model="username" type="text" placeholder="3-20个字符"
            autocomplete="off" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            name="student_register_username"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">密码</label>
          <input v-model="password" type="password" placeholder="至少6个字符"
            autocomplete="new-password" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            name="student_register_password"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">确认密码</label>
          <input v-model="confirmPassword" type="password" placeholder="再次输入密码"
            autocomplete="new-password" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            name="student_register_confirm_password"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">手机号</label>
          <input v-model="phone" type="tel" placeholder="请输入手机号"
            autocomplete="off" autocapitalize="off" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            name="student_register_phone"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">验证码</label>
          <div class="flex gap-2">
            <input v-model="verifyCode" type="text" placeholder="请输入验证码"
              autocomplete="off" autocapitalize="off" spellcheck="false"
              data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
              name="student_register_code"
              class="flex-1 px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
            <button type="button" @click="handleSendCode"
              class="px-3 py-3 rounded-xl border border-gray-200 bg-white text-xs text-gray-500 hover:bg-gray-50">
              获取验证码
            </button>
          </div>
          <p class="text-xs text-gray-400 mt-1">验证码获取流程待接入（TODO）</p>
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">邀请码</label>
          <input v-model="inviteCode" type="text" placeholder="请输入邀请码"
            autocomplete="off" autocapitalize="characters" spellcheck="false"
            data-1p-ignore="true" data-lpignore="true" data-bwignore="true"
            name="student_register_invite_code"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <button @click="handleRegister" :disabled="loading"
          class="w-full py-3 bg-accent bg-accent-hover text-white rounded-xl font-medium transition active:scale-95 disabled:opacity-50">
          {{ loading ? '注册中...' : '立即注册' }}
        </button>
      </div>

      <div class="mt-4 text-center text-sm text-gray-400">
        已有账号？<router-link to="/login" class="hover:text-accent">去登录</router-link>
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
const confirmPassword = ref('')
const phone = ref('')
const verifyCode = ref('')
const inviteCode = ref('')
const error = ref('')
const loading = ref(false)

function handleSendCode() {
  // TODO: 接入短信验证码发送
  error.value = '验证码发送功能待接入'
}

async function handleRegister() {
  if (!username.value || !password.value || !confirmPassword.value || !phone.value || !verifyCode.value || !inviteCode.value) {
    error.value = '请完整填写注册信息'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次密码不一致'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await studentStore.register({
      invite_code: inviteCode.value,
      username: username.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
      phone: phone.value,
      code: verifyCode.value
    })
    router.push('/student')
  } catch (err) {
    error.value = err?.error || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
