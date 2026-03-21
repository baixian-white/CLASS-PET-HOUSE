<template>
  <div class="min-h-screen pb-[calc(5rem+env(safe-area-inset-bottom))]"
    style="background: linear-gradient(160deg, #e8f5e9 0%, #e0f7fa 50%, #fce4ec 100%)">

    <!-- 顶部导航栏 -->
    <div class="fixed top-0 left-0 right-0 z-50 px-4 pt-[calc(0.75rem+env(safe-area-inset-top))] pb-2">
      <div class="max-w-lg mx-auto bg-white/90 backdrop-blur-xl rounded-2xl shadow-lg px-4 py-3 flex items-center gap-3 border border-white">
        <!-- 宠物头像 -->
        <div class="w-10 h-10 rounded-xl bg-slate-50 flex items-center justify-center overflow-hidden border border-slate-100 shrink-0">
          <img v-if="store.me?.pet_type" :src="petImageUrl" class="w-9 h-9 object-contain" />
          <span v-else class="text-xl">🥚</span>
        </div>
        <!-- 学生信息 -->
        <div class="flex-1 min-w-0">
          <p class="font-black text-slate-800 text-sm truncate">{{ store.me?.name || '同学' }}</p>
          <p class="text-[11px] text-slate-400 font-medium truncate">
            {{ store.me?.pet_name || '还没有宠物' }} · {{ store.currentClass?.name || '' }}
          </p>
        </div>
        <!-- 积分 -->
        <div class="flex items-center gap-1 bg-amber-50 px-2.5 py-1 rounded-full border border-amber-100 shrink-0">
          <span class="text-sm">🍖</span>
          <span class="text-sm font-black text-amber-600">{{ store.me?.food_count ?? 0 }}</span>
        </div>
        <!-- 退出 -->
        <button @click="handleLogout"
          class="w-8 h-8 flex items-center justify-center bg-slate-100 hover:bg-red-50 hover:text-red-500 text-slate-400 rounded-xl transition-colors shrink-0 text-sm">
          🚪
        </button>
      </div>
    </div>

    <!-- 占位 -->
    <div class="h-[calc(4.5rem+env(safe-area-inset-top))]"></div>

    <!-- 页面内容 -->
    <main class="max-w-lg mx-auto px-4 py-4">
      <router-view />
    </main>

    <!-- 底部 TabBar -->
    <div class="fixed bottom-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-xl border-t border-slate-100 shadow-[0_-5px_20px_rgba(0,0,0,0.05)] pb-[env(safe-area-inset-bottom)]">
      <nav class="max-w-lg mx-auto flex items-center justify-around px-2 py-2">
        <router-link v-for="tab in tabs" :key="tab.path" :to="tab.path"
          class="flex flex-col items-center gap-0.5 w-16 h-14 justify-center rounded-2xl transition-all"
          :class="isActive(tab.path) ? 'bg-cyan-50/80 text-cyan-500' : 'text-slate-400 hover:bg-slate-50'">
          <span class="text-xl" :class="isActive(tab.path) ? 'scale-110 drop-shadow-sm' : 'opacity-70'">
            {{ tab.icon }}
          </span>
          <span class="text-[10px] font-bold leading-none"
            :class="isActive(tab.path) ? 'text-cyan-500' : ''">{{ tab.label }}</span>
        </router-link>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStudentStore } from '../stores/student'
import { PETS, getPetImageUrl } from '../utils/pets'

const store = useStudentStore()
const route = useRoute()
const router = useRouter()

const tabs = [
  { path: '/student', icon: '🏠', label: '我的宠物' },
  { path: '/student/leaderboard', icon: '🏆', label: '排行榜' },
  { path: '/student/history', icon: '📜', label: '积分记录' },
  { path: '/student/shop', icon: '🛍️', label: '商店' },
]

function isActive(path) {
  if (path === '/student') return route.path === '/student'
  return route.path.startsWith(path)
}

const petImageUrl = computed(() => {
  if (!store.me?.pet_type) return ''
  const pet = PETS.find(p => p.id === store.me.pet_type)
  if (!pet) return ''
  const stages = store.currentClass?.growth_stages || [0,5,10,20,30,45,60,75,90,100]
  const maxFood = stages[stages.length - 1]
  let stage = 1
  for (let i = stages.length - 1; i >= 0; i--) {
    if (store.me.food_count >= stages[i]) { stage = i + 1; break }
  }
  return getPetImageUrl(pet.folder, stage)
})

function handleLogout() {
  store.logout()
  router.push('/student/login')
}

onMounted(async () => {
  try {
    await store.fetchMe()
  } catch {}
})
</script>
