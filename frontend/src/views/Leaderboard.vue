<template>
  <div class="max-w-5xl mx-auto pb-8">
    <div class="px-2 mb-6">
      <div class="rounded-[2rem] border border-white/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.95),rgba(243,248,255,0.95))] shadow-[0_24px_60px_-24px_rgba(14,165,233,0.45)] px-5 py-5 sm:px-7 sm:py-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <h2 class="text-xl sm:text-2xl font-black text-slate-800 flex items-center gap-2 drop-shadow-sm">
              <span class="text-2xl sm:text-3xl text-yellow-400 drop-shadow-md animate-breathe">🏆</span>
              星光榜
            </h2>
            <p class="mt-2 text-xs sm:text-sm text-slate-500 leading-relaxed">
              当前班级支持四个维度：当前宠物食物、总食物、当前勋章、总勋章。
            </p>
          </div>
          <div class="hidden sm:flex items-center gap-2 rounded-2xl bg-white/80 border border-slate-100 px-3 py-2 text-xs font-bold text-slate-500 shrink-0">
            <span>👥 {{ currentRows.length }} 人上榜</span>
          </div>
        </div>
      </div>
    </div>

    <div class="flex flex-wrap gap-2 sm:gap-3 mb-5 px-2">
      <button
        v-for="tab in rankingTabs"
        :key="tab.key"
        @click="activeTab = tab.key"
        :class="activeTab === tab.key
          ? 'bg-accent text-white shadow-md shadow-accent/40 scale-105'
          : 'bg-white text-slate-500 hover:bg-slate-50 hover:text-slate-700'"
        class="px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-bold transition-all duration-300"
      >
        {{ tab.icon }} {{ tab.label }}
      </button>
    </div>

    <div class="px-2 mb-5">
      <div class="rounded-[1.6rem] border border-white/70 bg-white/90 backdrop-blur-md px-4 py-4 sm:px-5 shadow-sm">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="text-sm sm:text-base font-black text-slate-800">
              {{ currentTab.icon }} {{ currentTab.title }}
            </p>
            <p class="mt-1 text-xs sm:text-sm text-slate-500">
              {{ currentTab.description }}
            </p>
          </div>
          <div class="rounded-2xl bg-slate-50 border border-slate-100 px-3 py-2 text-xs font-bold text-slate-500">
            单位：{{ currentTab.unit }}
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="space-y-3 sm:space-y-4 px-1">
      <div
        v-for="index in 6"
        :key="index"
        class="h-24 sm:h-28 rounded-2xl sm:rounded-[1.8rem] border-2 border-white bg-white/75 animate-pulse"
      ></div>
    </div>

    <div v-else-if="errorMessage" class="px-2">
      <div class="rounded-[1.8rem] border border-rose-100 bg-rose-50/90 px-5 py-5 text-sm text-rose-500 shadow-sm">
        {{ errorMessage }}
      </div>
    </div>

    <div v-else-if="!currentRows.length" class="px-2">
      <div class="rounded-[1.8rem] border border-slate-100 bg-white/90 px-5 py-8 text-center text-slate-400 shadow-sm">
        当前班级还没有可展示的排行数据
      </div>
    </div>

    <div v-else class="space-y-3 sm:space-y-4 px-1">
      <div
        v-for="(student, index) in currentRows"
        :key="`${activeTab}-${student.id}`"
        class="group rounded-2xl sm:rounded-[1.8rem] border-2 border-white bg-white/90 backdrop-blur-md p-3 sm:p-4 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-[var(--theme-ring)]/40 hover:shadow-[0_15px_35px_-10px_var(--theme-ring)] animate-stagger-fade-in"
        :style="{ animationDelay: `${index * 0.05}s` }"
      >
        <div class="flex items-start gap-3 sm:gap-4">
          <div class="relative w-12 h-12 flex items-center justify-center shrink-0">
            <div
              v-if="index < 3"
              class="absolute inset-0 rounded-full blur-md opacity-60 bg-gradient-to-tr"
              :class="medalGlowClass(index)"
            ></div>
            <span
              class="text-3xl relative z-10 font-kuaile italic leading-none"
              :class="medalTextClass(index)"
            >
              {{ index < 3 ? ['🥇', '🥈', '🥉'][index] : index + 1 }}
            </span>
          </div>

          <div class="w-12 h-12 sm:w-14 sm:h-14 shrink-0 bg-slate-50/80 rounded-xl sm:rounded-[1.2rem] flex items-center justify-center overflow-hidden border border-slate-100/50 group-hover:scale-110 transition-transform duration-300">
            <img
              v-if="student.pet_type"
              :src="getPetImage(student)"
              class="w-9 h-9 sm:w-12 sm:h-12 object-contain animate-float-idle"
            />
            <span v-else class="text-xl sm:text-2xl">🥚</span>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <p class="text-sm sm:text-base font-black text-slate-700 truncate">
                {{ student.name }}
              </p>
              <span
                v-if="student.Group?.name"
                class="inline-flex items-center rounded-full bg-cyan-50 px-2 py-0.5 text-[10px] sm:text-xs font-bold text-cyan-600"
              >
                {{ student.Group.name }}
              </span>
            </div>
            <p class="text-[10px] sm:text-xs font-bold text-slate-400 truncate mt-1">
              {{ student.pet_name || '未命名宠物' }}
            </p>
            <p class="text-[11px] sm:text-xs text-slate-500 mt-2 leading-relaxed">
              {{ secondaryText(student) }}
            </p>
          </div>

          <div class="text-right shrink-0">
            <div class="px-3 py-1.5 sm:px-4 sm:py-2 rounded-full bg-slate-50 border border-slate-100 text-accent font-black text-sm sm:text-base">
              {{ student.rank_value }}
            </div>
            <p class="mt-1 text-[10px] sm:text-xs font-bold text-slate-400">
              {{ currentTab.unit }}
            </p>
          </div>
        </div>

        <div
          v-if="activeTab === 'total_food' && student.food_summary?.length"
          class="mt-4 rounded-[1.3rem] border border-amber-100 bg-amber-50/70 px-3 py-3 sm:px-4"
        >
          <div class="flex items-center justify-between gap-3 mb-2">
            <p class="text-xs sm:text-sm font-black text-amber-700">分数组成</p>
            <p class="text-[10px] sm:text-xs font-bold text-amber-500">
              共 {{ student.food_summary.length }} 项
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <div
              v-for="(summary, summaryIndex) in visibleFoodSummary(student)"
              :key="`${student.id}-${summary.rule_id}-${summaryIndex}`"
              class="rounded-2xl bg-white/85 border border-amber-100 px-3 py-2 text-[11px] sm:text-xs text-slate-600"
            >
              <span class="font-black text-slate-700">{{ summary.rule_name }}</span>
              <span class="mx-1 text-slate-300">·</span>
              <span>+{{ summary.total_food }}</span>
              <span class="mx-1 text-slate-300">·</span>
              <span>{{ summary.award_count }}次</span>
            </div>
            <div
              v-if="student.food_summary.length > FOOD_SUMMARY_PREVIEW_LIMIT"
              class="rounded-2xl bg-amber-100/80 px-3 py-2 text-[11px] sm:text-xs font-bold text-amber-700"
            >
              还有 {{ student.food_summary.length - FOOD_SUMMARY_PREVIEW_LIMIT }} 项
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useClassStore } from '../stores/class.js'
import api from '../utils/api.js'
import { PETS, getPetImageUrl } from '../utils/pets.js'

const FOOD_SUMMARY_PREVIEW_LIMIT = 4

const classStore = useClassStore()

const rankingTabs = [
  {
    key: 'current_food',
    label: '当前食物榜',
    title: '当前宠物食物排行榜',
    icon: '🍖',
    unit: '食物分',
    description: '统计当前宠物还在累积中的食物分，清零后会从新的时间点重新开始计算。'
  },
  {
    key: 'total_food',
    label: '总食物榜',
    title: '总食物排行榜',
    icon: '🥣',
    unit: '累计食物分',
    description: '统计学生历史上累计获得的正向食物分，并展示这些分数分别来自哪些规则。'
  },
  {
    key: 'current_badges',
    label: '当前勋章榜',
    title: '勋章排行榜',
    icon: '🏅',
    unit: '当前勋章',
    description: '统计学生当前还持有、还可以继续兑换的勋章数量。'
  },
  {
    key: 'total_badges',
    label: '总勋章榜',
    title: '总勋章排行榜',
    icon: '🎖️',
    unit: '累计勋章',
    description: '统计学生历史上一共获得过多少勋章，不会因为兑换而减少。'
  }
]

const activeTab = ref('current_food')
const loading = ref(false)
const errorMessage = ref('')
const leaderboards = ref(createEmptyLeaderboards())

let lastRequestId = 0

const currentTab = computed(() => rankingTabs.find(item => item.key === activeTab.value) || rankingTabs[0])

const currentRows = computed(() => leaderboards.value[activeTab.value] || [])

watch(
  () => classStore.currentClass?.id,
  async classId => {
    await fetchLeaderboards(classId)
  },
  { immediate: true }
)

async function fetchLeaderboards(classId) {
  if (!classId) {
    lastRequestId += 1
    leaderboards.value = createEmptyLeaderboards()
    errorMessage.value = ''
    loading.value = false
    return
  }

  const requestId = ++lastRequestId
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await api.get(`/leaderboards/class/${classId}`)
    if (requestId !== lastRequestId) return
    leaderboards.value = normalizeLeaderboards(data)
  } catch (error) {
    if (requestId !== lastRequestId) return
    leaderboards.value = createEmptyLeaderboards()
    errorMessage.value = error?.message || '排行榜加载失败，请稍后重试'
  } finally {
    if (requestId === lastRequestId) {
      loading.value = false
    }
  }
}

function createEmptyLeaderboards() {
  return {
    current_food: [],
    total_food: [],
    current_badges: [],
    total_badges: []
  }
}

function normalizeLeaderboards(data) {
  return {
    current_food: Array.isArray(data?.current_food_ranking) ? data.current_food_ranking : [],
    total_food: Array.isArray(data?.total_food_ranking) ? data.total_food_ranking : [],
    current_badges: Array.isArray(data?.current_badges_ranking) ? data.current_badges_ranking : [],
    total_badges: Array.isArray(data?.total_badges_ranking) ? data.total_badges_ranking : []
  }
}

function secondaryText(student) {
  if (activeTab.value === 'current_food') {
    return `累计获得 ${student.total_food_earned || 0} 食物分`
  }
  if (activeTab.value === 'total_food') {
    return `当前食物分 ${student.food_count || 0}，用于宠物当前成长进度`
  }
  if (activeTab.value === 'current_badges') {
    return `历史累计获得 ${student.total_badges_earned || 0} 枚勋章`
  }
  return `当前还持有 ${student.current_badges_count || 0} 枚勋章`
}

function visibleFoodSummary(student) {
  return (student.food_summary || []).slice(0, FOOD_SUMMARY_PREVIEW_LIMIT)
}

function medalGlowClass(index) {
  if (index === 0) return 'from-yellow-200 to-yellow-500'
  if (index === 1) return 'from-slate-200 to-slate-400'
  return 'from-orange-200 to-amber-600'
}

function medalTextClass(index) {
  if (index === 0) return 'text-yellow-400 drop-shadow-md scale-110'
  if (index === 1) return 'text-slate-400 drop-shadow-md scale-105'
  if (index === 2) return 'text-amber-500 drop-shadow-md scale-105'
  return 'text-slate-300 text-xl font-bold font-sans not-italic'
}

function getPetImage(student) {
  if (!student?.pet_type) return ''
  const pet = PETS.find(item => item.id === student.pet_type)
  if (!pet) return ''

  const stages = classStore.currentClass?.growth_stages || [0, 5, 10, 20, 30, 45, 60, 75, 90, 100]
  const currentFood = student.food_count || 0
  let stage = 1

  for (let i = stages.length - 1; i >= 0; i--) {
    if (currentFood >= stages[i]) {
      stage = i + 1
      break
    }
  }

  return getPetImageUrl(pet.folder, stage)
}
</script>
