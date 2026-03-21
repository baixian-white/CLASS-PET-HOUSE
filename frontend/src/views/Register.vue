<template>
  <div class="min-h-screen bg-theme flex items-center justify-center p-4">
    <div class="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">
      <h1 class="text-2xl font-bold text-center text-gray-700 mb-2">🐾 石榴果宠物屋</h1>
      <p class="text-center text-gray-400 mb-6">注册新账号</p>

      <div v-if="error" class="bg-red-50 text-red-500 text-sm p-3 rounded-lg mb-4">{{ error }}</div>
      <div v-if="sendMessage" class="bg-emerald-50 text-emerald-600 text-sm p-3 rounded-lg mb-4">{{ sendMessage }}</div>

      <div class="space-y-4">
        <div>
          <label class="block text-sm text-gray-500 mb-1">用户名</label>
          <input v-model="username" type="text" placeholder="3-20个字符"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">密码</label>
          <input v-model="password" type="password" placeholder="至少6个字符"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">确认密码</label>
          <input v-model="confirmPassword" type="password" placeholder="再次输入密码"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">手机号</label>
          <input v-model="phone" type="tel" placeholder="请输入手机号"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">验证码</label>
          <div class="flex gap-2">
            <input v-model="verifyCode" type="text" placeholder="请输入验证码"
              class="flex-1 px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition" />
            <button type="button" @click="handleSendCode" :disabled="sendingCode || resendCountdown > 0"
              class="px-3 py-3 rounded-xl border border-gray-200 bg-white text-xs text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
              {{ sendButtonText }}
            </button>
          </div>
          <p class="text-xs text-gray-400 mt-1">请输入收到的 6 位短信验证码。</p>
        </div>
        <div>
          <label class="block text-sm text-gray-500 mb-1">系统邀请码/激活码</label>
          <input v-model="activationCode" type="text" placeholder="例如：CPH-A1B2C3D4" @keyup.enter="handleRegister"
            class="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-accent ring-accent outline-none transition uppercase" />
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
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const phone = ref('')
const verifyCode = ref('')
const activationCode = ref('')
const error = ref('')
const sendMessage = ref('')
const loading = ref(false)
const sendingCode = ref(false)
const resendCountdown = ref(0)

let resendTimer = null

const sendButtonText = computed(() => {
  if (sendingCode.value) return '发送中...'
  if (resendCountdown.value > 0) return `${resendCountdown.value}s`
  return '获取验证码'
})

function isValidPhone(value) {
  return /^1\d{10}$/.test(String(value || '').trim())
}

function startCountdown(seconds) {
  if (resendTimer) clearInterval(resendTimer)
  resendCountdown.value = Math.max(0, Number(seconds) || 60)
  resendTimer = setInterval(() => {
    if (resendCountdown.value <= 1) {
      clearInterval(resendTimer)
      resendTimer = null
      resendCountdown.value = 0
      return
    }
    resendCountdown.value -= 1
  }, 1000)
}

async function handleSendCode() {
  const normalizedPhone = phone.value.trim()
  if (!isValidPhone(normalizedPhone)) {
    error.value = '请输入正确的11位手机号'
    return
  }

  sendingCode.value = true
  error.value = ''
  sendMessage.value = ''
  try {
    const data = await auth.sendRegisterCode(normalizedPhone)
    startCountdown(data?.resend_seconds || 60)
    sendMessage.value = data?.real_send_enabled
      ? '验证码已发送，请注意查收短信'
      : '当前仍是本地调试模式，请检查后端短信配置'
  } catch (err) {
    error.value = err?.error || '验证码发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  if (!username.value || !password.value || !confirmPassword.value || !phone.value || !verifyCode.value || !activationCode.value) {
    error.value = '请填写完整的注册信息'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次密码不一致'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.register({
      username: username.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
      phone: phone.value,
      verifyCode: verifyCode.value,
      activationCode: activationCode.value
    })
    router.push('/')
  } catch (err) {
    error.value = err.error || '注册失败'
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (resendTimer) clearInterval(resendTimer)
})
</script>
