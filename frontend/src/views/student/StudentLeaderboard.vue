<template>
  <div>
    <h2 class="text-xl font-black text-slate-800 mb-4 flex items-center gap-2">
      <span class="text-2xl text-yellow-400 animate-pulse">🏆</span> 班级星光榜
    </h2>

    <!-- 维度切换 -->
    <div class="flex gap-2 mb-4">
      <button @click="rankBy = 'food'"
        :class="rankBy === 'food' ? 'bg-cyan-500 text-white shadow-md shadow-cyan-200' : 'bg-white text-slate-500 hover:bg-slate-50'"
        class="px-4 py-2 rounded-full text-sm font-black transition-all">🍖 食物排行</button>
      <button @click="rankBy = 'badges'"
        :class="rankBy === 'badges' ? 'bg-cyan-500 text-white shadow-md shadow-cyan-200' : 'bg-white text-slate-500 hover:bg-slate-50'"
        class="px-4 py-2 rounded-full text-sm font-black transition-all">🏅 徽章排行</button>
    </div>

    <!-- 排行列表 -->
    <div class="space-y-3 pb-4">
      <div v-for="(s, i) in rankedList" :key="s.id"
        class="flex items-center gap-3 rounded-2xl p-3 border-2 transition-all"
        :class="isMe(s.id)
          ? 'bg-cyan-50 border-cyan-200 shadow-md shadow-cyan-100'
          : 'bg-white/90 border-white hover:border-slate-100'">

        <!-- 名次 -->
        <div class="w-10 h-10 flex items-center justify-center shrink-0">
          <span v-if="i < 3" class="text-3xl">{{ ['🥇','🥈','🥉'][i] }}</span>
          <span v-else class="text-lg font-black text-slate-300">{{ i + 1 }}</span>
        </div>

        <!-- 宠物图 -->
        <div class="w-10 h-10 bg-slate-50 rounded-xl flex items-center justify-center overflow-hidden border border-slate-100 shrink-0">
          <img v-if="s.pet_type" :src="getPetImg(s)" class="w-9 h-9 object-contain" />
          <span v-else class="text-xl">🥚</span>
        </div>

        <!-- 名字 -->
        <div class="flex-1 min-w-0">
          <p class="font-black text-slate-700 text-sm truncate">
            {{ s.name }}
            <span v-if="isMe(s.id)" class="ml-1 text-[10px] font-bold text-cyan-500 bg-cyan-100 px-1.5 py-0.5 rounded-full">我</span>
          </p>
          <p class="text-[11px] text-slate-400 truncate">{{ s.pet_name || '未命名' }}</p>
        </div>

        <!-- 分数 -->
        <div class="px-3 py-1 rounded-full bg-slate-50 font-black text-sm text-cyan-600 border border-slate-100 shrink-0">
          {{ rankBy === 'food' ? s.food_count : (s.badges || []).length }}
        </div>
      </div>

      <div v-if="!rankedList.length" class="text-center py-12 text-slate-400">
        <p class="text-4xl mb-2">📭</p>
        <p>暂无数据</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStudentStore } from '../../stores/student'
import { PETS, getPetImageUrl } from '../../utils/pets'

const store = useStudentStore()
const rankBy = ref('food')

const rankedList = computed(() => {
  const list = [...store.leaderboard]
  if (rankBy.value === 'food') {
    list.sort((a, b) => b.food_count - a.food_count)
  } else {
    list.sort((a, b) => (b.badges || []).length - (a.badges || []).length)
  }
  return list
})

function isMe(id) {
  return store.me && id === store.me.id
}

function getPetImg(s) {
  if (!s.pet_type) return ''
  const pet = PETS.find(p => p.id === s.pet_type)
  if (!pet) return ''
  const stages = store.currentClass?.growth_stages || [0,5,10,20,30,45,60,75,90,100]
  let stage = 1
  for (let i = stages.length - 1; i >= 0; i--) {
    if (s.food_count >= stages[i]) { stage = i + 1; break }
  }
  return getPetImageUrl(pet.folder, stage)
}

onMounted(async () => {
  try { await store.fetchLeaderboard() } catch {}
})
</script>
