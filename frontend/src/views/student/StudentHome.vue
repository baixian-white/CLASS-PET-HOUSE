<template>
  <div class="flex flex-col items-center w-full gap-5 pb-6">
    
    <!-- 中间宠物区与控制栏 -->
    <div class="w-full flex flex-col gap-3">
      <div class="relative w-full aspect-square sm:aspect-[4/3] rounded-[2rem] shadow-[0_10px_30px_-10px_rgba(0,0,0,0.15)] overflow-hidden border-4 border-white transition-all select-none group"
           @mousemove="onDragMove"
           @mouseup="onDragEnd"
           @mouseleave="onDragEnd"
           @touchmove="onDragMove"
           @touchend="onDragEnd">
        <!-- 单独容器的背景 -->
        <img :src="roomBgUrl" class="absolute inset-0 w-full h-full object-cover z-0 pointer-events-none" draggable="false" />

        <!-- 宠物图片实体 (可拖拽) -->
        <div class="absolute z-20 cursor-move flex flex-col items-center justify-end"
             :style="{ 
               transform: `translate(calc(-50% + ${petX}px), calc(-50% + ${petY}px)) scale(${petScale})`,
               transformOrigin: 'bottom center',
               left: '50%', top: '65%',
               touchAction: 'none'
             }"
             @mousedown.prevent="onDragStart"
             @touchstart.prevent="onDragStart">
             
          <img v-if="me?.pet_type && petImageUrl" :src="petImageUrl" class="relative z-10 w-20 h-20 sm:w-28 sm:h-28 object-contain animate-[float_3s_ease-in-out_infinite] mb-3 sm:mb-4 pointer-events-none" draggable="false" />
          <span v-else class="relative z-10 text-[4rem] leading-none animate-[float_3s_ease-in-out_infinite] mb-5 pointer-events-none">🥚</span>
          
          <!-- 垫子(跟随宠物) -->
          <div class="absolute bottom-3 sm:bottom-4 w-14 h-2 sm:w-20 sm:h-3 bg-black/20 rounded-[100%] blur-[2px] z-0 pointer-events-none"></div>
        </div>
      </div>

      <!-- 宠物互动控制栏 (与下方信息卡片等宽，方形圆角) -->
      <div class="flex items-center justify-between w-full bg-white/80 backdrop-blur-md px-4 py-3 sm:px-5 sm:py-3.5 rounded-2xl shadow-sm border border-white/80 transition-all">
        
        <!-- 左侧: 切换背景主题 (扁频色块) -->
        <button @click.stop.prevent="nextBackground" class="flex items-center justify-center gap-2 bg-cyan-400 hover:bg-cyan-500 text-white px-4 py-2 sm:px-5 sm:py-2.5 rounded-xl transition-all active:scale-95 group">
          <span class="text-lg sm:text-xl leading-none group-hover:scale-110 transition-transform origin-bottom">🖼️</span>
          <span class="text-[11px] sm:text-xs font-black tracking-wide">切换主题</span>
        </button>

        <!-- 右侧: 缩放控制面板 -->
        <div class="flex items-center gap-2 sm:gap-4 pl-2">
          <span class="text-[10px] sm:text-xs font-black text-slate-400 select-none hidden sm:inline">比例</span>
          <input type="range" min="0.3" max="1.5" step="0.05" v-model.number="petScale" class="w-20 sm:w-28 accent-cyan-400 cursor-pointer drop-shadow-sm" />
          
          <!-- 还原按钮 (扁平色块) -->
          <button @click="resetTransform" class="flex items-center justify-center bg-slate-100 hover:bg-slate-200 text-slate-500 px-3 py-1.5 sm:px-3.5 sm:py-2 rounded-xl shadow-sm border border-slate-200 transition-all active:scale-95 shrink-0" title="重置大小与位置">
            <span class="text-[10px] sm:text-[11px] font-black">还原</span>
          </button>
        </div>
        
      </div>
    </div>

    <!-- 信息区 (移动到宠物下方) -->
    <div class="w-full space-y-3">
      <!-- 宠物文字说明 -->
      <div class="text-center bg-white/80 backdrop-blur-md rounded-2xl p-4 shadow-sm w-full">
        <div v-if="me?.pet_type">
          <p class="text-xl font-black text-slate-800">{{ me.pet_name || '未命名' }}</p>
          <div class="flex items-center justify-center gap-2 mt-1">
            <p class="text-xs font-bold text-slate-500">{{ petTypeName }}</p>
            <div class="inline-flex items-center gap-1 bg-cyan-50/80 px-2 py-0.5 rounded-full border border-cyan-100/50">
              <div class="w-1.5 h-1.5 rounded-full" :class="levelDotColor"></div>
              <span class="text-[10px] font-black text-cyan-600">Lv.{{ petStage }}</span>
            </div>
          </div>
        </div>
        <div v-else>
          <p class="text-lg font-black text-slate-800">{{ me?.name || '同学' }}</p>
          <p v-if="groupName" class="text-xs text-slate-500 mt-0.5">👥 {{ groupName }}</p>
          <p class="text-xs text-slate-500 mt-1">还没有宠物，请联系老师分配 🐾</p>
        </div>
      </div>

      <!-- 数据统计 -->
      <div class="grid gap-2" :class="groupName ? 'grid-cols-3' : 'grid-cols-2'">
        <div class="bg-white/80 backdrop-blur-md rounded-xl p-3 shadow-sm text-center">
          <p class="text-2xl font-black text-amber-500">{{ me?.food_count ?? 0 }}</p>
          <p class="text-[10px] font-bold text-slate-500 mt-0.5">🍖 积分食物</p>
        </div>
        <div class="bg-white/80 backdrop-blur-md rounded-xl p-3 shadow-sm text-center">
          <p class="text-2xl font-black text-orange-400">{{ badgeCount }}</p>
          <p class="text-[10px] font-bold text-slate-500 mt-0.5">🏅 徽章数量</p>
        </div>
        <div v-if="groupName" class="bg-white/80 backdrop-blur-md rounded-xl p-3 shadow-sm text-center overflow-hidden">
          <p class="text-lg font-black text-purple-400 truncate">{{ groupName }}</p>
          <p class="text-[10px] font-bold text-slate-500 mt-0.5">👥 我的小组</p>
        </div>
      </div>

      <!-- 成长进度 -->
      <div v-if="me?.pet_type" class="bg-white/80 backdrop-blur-md rounded-xl p-3 shadow-sm">
        <div class="flex justify-between items-center mb-1.5">
          <h3 class="font-black text-slate-700 text-xs">成长进度</h3>
          <span v-if="!isMaxLevel" class="text-[10px] font-bold text-blue-500">
            还差 {{ maxFood - (me?.food_count ?? 0) }} 🍖 升级
          </span>
          <span v-else class="text-[10px] font-black text-yellow-500">✨ 已满级！</span>
        </div>
        <div class="h-2.5 bg-slate-200/50 rounded-full overflow-hidden">
          <div class="h-full bg-gradient-to-r from-cyan-400 to-sky-400 rounded-full transition-all duration-1000"
            :style="{ width: `${progressPercent}%` }"></div>
        </div>
        <div class="flex justify-between text-[9px] font-bold text-slate-500 mt-1">
          <span>Lv.{{ petStage }}</span>
          <span>{{ me?.food_count ?? 0 }} / {{ maxFood }}</span>
        </div>
      </div>

      <!-- 我的排名 -->
      <div v-if="myRank" class="bg-cyan-50/80 backdrop-blur-md rounded-xl p-3 flex items-center justify-between shadow-sm">
        <div class="flex items-center gap-3">
          <span class="text-2xl leading-none">{{ myRank <= 3 ? ['🥇','🥈','🥉'][myRank-1] : '🏅' }}</span>
          <div>
            <p class="font-black text-slate-700 text-sm">班级排名第 <span class="text-cyan-500 text-lg">{{ myRank }}</span> 名</p>
            <p class="text-[10px] text-slate-500 font-medium mt-0.5">按积分食物排行</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useStudentStore } from '../../stores/student'
import { PETS, getPetImageUrl } from '../../utils/pets'

// ----- 背景皮肤切换逻辑 -----
const BACKGROUNDS = [
  '背景图1.png', '背景图2.png', '背景图3.png', '背景图4.png', '背景图5.png',
  '背景图6.png', '背景图7.png', '背景图8.png', '背景图9.png', '背景图10.png',
  '背景图11.png', '背景图12.png', '背景图13.png', '背景图14.png', '背景图15.png',
  '背景图16.png', '背景图17.png'
]

const bgIndex = ref(0)
const roomBgUrl = computed(() => {
  const baseUrl = import.meta.env.VITE_BACKGROUND_BASE_URL || '/pet-backgrounds'
  return `${baseUrl}/${BACKGROUNDS[bgIndex.value]}`
})

const nextBackground = () => {
  bgIndex.value = (bgIndex.value + 1) % BACKGROUNDS.length
  saveTransform() // 保存包含背景的信息
}

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

// ----- 宠物自由拖拽和缩放逻辑 -----
const petScale = ref(1)
const petX = ref(0)
const petY = ref(0)
const isDragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let initialPosX = 0
let initialPosY = 0

const STORAGE_KEY = 'class-pet-house:pet-transform'

const loadTransform = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      if (typeof parsed.scale === 'number') petScale.value = Math.min(Math.max(parsed.scale, 0.4), 1.5)
      if (typeof parsed.x === 'number') petX.value = parsed.x
      if (typeof parsed.y === 'number') petY.value = parsed.y
      if (typeof parsed.bg === 'number') bgIndex.value = parsed.bg
    }
  } catch (e) {
    console.error('Failed to load pet transform', e)
  }
}

const saveTransform = () => {
  const data = JSON.stringify({ 
    scale: petScale.value, 
    x: petX.value, 
    y: petY.value,
    bg: bgIndex.value
  })
  localStorage.setItem(STORAGE_KEY, data)
}

watch(petScale, () => {
  saveTransform()
})

const resetTransform = () => {
  petScale.value = 1
  petX.value = 0
  petY.value = 0
  saveTransform()
}

const onDragStart = (e) => {
  isDragging.value = true
  const clientX = e.touches ? e.touches[0].clientX : e.clientX
  const clientY = e.touches ? e.touches[0].clientY : e.clientY
  dragStartX = clientX
  dragStartY = clientY
  initialPosX = petX.value
  initialPosY = petY.value
}

const onDragMove = (e) => {
  if (!isDragging.value) return
  const clientX = e.touches ? e.touches[0].clientX : e.clientX
  const clientY = e.touches ? e.touches[0].clientY : e.clientY
  const dx = clientX - dragStartX
  const dy = clientY - dragStartY
  petX.value = initialPosX + dx
  petY.value = initialPosY + dy
}

const onDragEnd = () => {
  if (isDragging.value) {
    isDragging.value = false
    saveTransform()
  }
}

onMounted(async () => {
  loadTransform()
  try {
    await Promise.all([store.fetchMe(), store.fetchLeaderboard()])
  } catch {}
})
</script>

<style scoped>
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
</style>

