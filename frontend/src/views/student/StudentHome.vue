<template>
  <div class="space-y-5">
    <!-- 宠物卡片 -->
    <div class="bg-white/90 backdrop-blur-xl rounded-3xl p-6 shadow-sm border-2 border-white text-center">
      <!-- 宠物图片 -->
      <div class="flex justify-center mb-4">
        <div class="w-44 h-44 flex items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100/50 rounded-3xl border border-slate-100 shadow-inner overflow-hidden">
          <img v-if="me?.pet_type && petImageUrl" :src="petImageUrl" class="w-40 h-40 object-contain animate-[float_3s_ease-in-out_infinite]" />
          <span v-else class="text-7xl">🥚</span>
        </div>
      </div>

      <!-- 宠物名 & 学生名 -->
      <div v-if="me?.pet_type">
        <p class="text-2xl font-black text-slate-800">{{ me.pet_name || '未命名' }}</p>
        <p class="text-sm text-slate-400 font-medium mt-0.5">
          {{ petTypeName }} · {{ me.name }}
          <span v-if="groupName"> · 👥 {{ groupName }}</span>
        </p>
      </div>
      <div v-else>
        <p class="text-xl font-black text-slate-800">{{ me?.name || '同学' }}</p>
        <p v-if="groupName" class="text-sm text-slate-400 mt-0.5">👥 {{ groupName }}</p>
        <p class="text-sm text-slate-400 mt-1">还没有宠物，请联系老师分配 🐾</p>
      </div>

      <!-- 等级标 -->
      <div v-if="me?.pet_type" class="mt-3 inline-flex items-center gap-1.5 bg-cyan-50 px-3 py-1.5 rounded-full border border-cyan-100">
        <div class="w-2 h-2 rounded-full" :class="levelDotColor"></div>
        <span class="text-sm font-black text-cyan-600">Lv.{{ petStage }}</span>
      </div>
    </div>

    <!-- 数据统计 -->
    <div class="grid gap-3" :class="groupName ? 'grid-cols-3' : 'grid-cols-2'">
      <div class="bg-white/90 backdrop-blur-xl rounded-2xl p-4 shadow-sm border-2 border-white text-center">
        <p class="text-3xl font-black text-amber-500">{{ me?.food_count ?? 0 }}</p>
        <p class="text-xs font-bold text-slate-400 mt-1">🍖 积分食物</p>
      </div>
      <div class="bg-white/90 backdrop-blur-xl rounded-2xl p-4 shadow-sm border-2 border-white text-center">
        <p class="text-3xl font-black text-orange-400">{{ badgeCount }}</p>
        <p class="text-xs font-bold text-slate-400 mt-1">🏅 徽章数量</p>
      </div>
      <div v-if="groupName" class="bg-white/90 backdrop-blur-xl rounded-2xl p-4 shadow-sm border-2 border-white text-center">
        <p class="text-xl font-black text-purple-400 truncate">{{ groupName }}</p>
        <p class="text-xs font-bold text-slate-400 mt-1">👥 我的小组</p>
      </div>
    </div>

    <!-- 成长进度 -->
    <div v-if="me?.pet_type" class="bg-white/90 backdrop-blur-xl rounded-2xl p-5 shadow-sm border-2 border-white">
      <div class="flex justify-between items-center mb-3">
        <h3 class="font-black text-slate-700 text-sm">成长进度</h3>
        <span v-if="!isMaxLevel" class="text-xs font-bold text-blue-500">
          还差 {{ maxFood - (me?.food_count ?? 0) }} 🍖 升级
        </span>
        <span v-else class="text-xs font-black text-yellow-500">✨ 已满级！</span>
      </div>
      <div class="h-4 bg-slate-100 rounded-full overflow-hidden">
        <div class="h-full bg-gradient-to-r from-cyan-400 to-sky-400 rounded-full transition-all duration-1000"
          :style="{ width: `${progressPercent}%` }"></div>
      </div>
      <div class="flex justify-between text-[10px] font-bold text-slate-400 mt-1">
        <span>Lv.{{ petStage }}</span>
        <span>{{ me?.food_count ?? 0 }} / {{ maxFood }}</span>
      </div>
    </div>

    <!-- 我的排名 -->
    <div v-if="myRank" class="bg-gradient-to-br from-cyan-50 to-teal-50 rounded-2xl p-4 border border-cyan-100 flex items-center gap-4">
      <span class="text-4xl">{{ myRank <= 3 ? ['🥇','🥈','🥉'][myRank-1] : '🏅' }}</span>
      <div>
        <p class="font-black text-slate-700">班级排名第 <span class="text-cyan-500 text-xl">{{ myRank }}</span> 名</p>
        <p class="text-xs text-slate-400 font-medium">按积分食物排行</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useStudentStore } from '../../stores/student'
import { PETS, getPetImageUrl } from '../../utils/pets'

const store = useStudentStore()
const me = computed(() => store.me)

const DEFAULT_STAGES = [0, 5, 10, 20, 30, 45, 60, 75, 90, 100]

const stages = computed(() => store.currentClass?.growth_stages || DEFAULT_STAGES)
const maxFood = computed(() => stages.value[stages.value.length - 1])

const petStage = computed(() => {
  if (!me.value?.pet_type) return 1
  let stage = 1
  for (let i = stages.value.length - 1; i >= 0; i--) {
    if ((me.value.food_count ?? 0) >= stages.value[i]) { stage = i + 1; break }
  }
  return stage
})

const progressPercent = computed(() => {
  return Math.min(100, ((me.value?.food_count ?? 0) / maxFood.value) * 100)
})

const isMaxLevel = computed(() => (me.value?.food_count ?? 0) >= maxFood.value)

const badgeCount = computed(() => (me.value?.badges || []).length)
const groupName = computed(() => me.value?.Group?.name || '')

const levelDotColor = computed(() => {
  const colors = ['bg-emerald-400','bg-blue-400','bg-purple-400','bg-orange-400','bg-red-400','bg-pink-400','bg-teal-400','bg-indigo-400','bg-fuchsia-400','bg-rose-400']
  return colors[(petStage.value - 1) % colors.length]
})

const petImageUrl = computed(() => {
  if (!me.value?.pet_type) return ''
  const pet = PETS.find(p => p.id === me.value.pet_type)
  if (!pet) return ''
  return getPetImageUrl(pet.folder, petStage.value)
})

const petTypeName = computed(() => {
  const pet = PETS.find(p => p.id === me.value?.pet_type)
  return pet?.name || ''
})

const myRank = computed(() => {
  if (!me.value || !store.leaderboard.length) return null
  const sorted = [...store.leaderboard].sort((a, b) => b.food_count - a.food_count)
  const idx = sorted.findIndex(x => x.id === me.value.id)
  return idx >= 0 ? idx + 1 : null
})

onMounted(async () => {
  try {
    await Promise.all([store.fetchMe(), store.fetchLeaderboard()])
  } catch {}
})
</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
</style>
