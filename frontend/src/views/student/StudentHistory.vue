<template>
  <div>
    <h2 class="text-xl font-black text-slate-800 mb-4 flex items-center gap-2">
      <span class="text-2xl">📜</span> 我的积分记录
    </h2>

    <!-- 加载中 -->
    <div v-if="loading" class="text-center py-12 text-slate-400">
      <p class="text-2xl animate-spin inline-block">⏳</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!store.history.length" class="text-center py-16 text-slate-400">
      <p class="text-5xl mb-3">📝</p>
      <p class="font-bold">暂无积分记录</p>
      <p class="text-sm mt-1">老师给你加分后就会显示在这里</p>
    </div>

    <!-- 记录列表 -->
    <div v-else class="space-y-2.5 pb-4">
      <div v-for="r in store.history" :key="r.id"
        class="flex items-center gap-3 bg-white/90 rounded-2xl px-4 py-3 border-2 border-white shadow-sm">

        <!-- 图标 -->
        <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
          :class="r.value > 0 ? 'bg-green-50' : r.value < 0 ? 'bg-red-50' : 'bg-yellow-50'">
          <span class="text-lg">
            {{ r.type === 'graduate' ? '🎓' : r.type === 'exchange' ? '🛍️' : r.value > 0 ? '⬆️' : '⬇️' }}
          </span>
        </div>

        <!-- 规则名 -->
        <div class="flex-1 min-w-0">
          <p class="font-black text-slate-700 text-sm truncate">{{ r.rule_name || '操作记录' }}</p>
          <p class="text-[11px] text-slate-400 mt-0.5">{{ formatDate(r.created_at) }}</p>
        </div>

        <!-- 分值 -->
        <div class="font-black text-base shrink-0"
          :class="r.value > 0 ? 'text-green-500' : r.value < 0 ? 'text-red-400' : 'text-slate-400'">
          {{ r.value > 0 ? '+' : '' }}{{ r.value !== 0 ? r.value : '—' }}
        </div>
      </div>

      <!-- 已是最早 -->
      <p v-if="store.history.length >= 50" class="text-center text-xs text-slate-400 py-3">
        最多显示近50条记录
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useStudentStore } from '../../stores/student'

const store = useStudentStore()
const loading = ref(false)

function formatDate(str) {
  if (!str) return ''
  try {
    const d = new Date(str)
    return d.toLocaleString('zh-CN', {
      month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit'
    }).replace(/\//g, '-')
  } catch { return '' }
}

onMounted(async () => {
  loading.value = true
  try { await store.fetchHistory() } catch {} finally { loading.value = false }
})
</script>
