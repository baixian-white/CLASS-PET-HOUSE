<template>
  <div>
    <h2 class="text-xl font-black text-slate-800 mb-1 flex items-center gap-2">
      <span class="text-2xl">🛍️</span> 班级商店
    </h2>
    <p class="text-xs text-slate-400 font-medium mb-4">如需兑换，请联系老师操作</p>

    <!-- 空状态 -->
    <div v-if="!store.shop.length" class="text-center py-16 text-slate-400">
      <p class="text-5xl mb-3">🏪</p>
      <p class="font-bold">暂无商品</p>
      <p class="text-sm mt-1">老师上架商品后会显示在这里</p>
    </div>

    <!-- 商品列表 -->
    <div class="grid grid-cols-2 gap-3 pb-4">
      <div v-for="item in store.shop" :key="item.id"
        class="bg-white/90 backdrop-blur-xl rounded-2xl p-4 border-2 border-white shadow-sm flex flex-col items-center text-center">

        <!-- 图标 -->
        <div class="w-14 h-14 bg-slate-50 rounded-2xl flex items-center justify-center text-4xl mb-3 shadow-inner">
          {{ item.icon || '🎁' }}
        </div>

        <p class="font-black text-slate-700 text-sm">{{ item.name }}</p>
        <p v-if="item.description" class="text-[11px] text-slate-400 mt-0.5 line-clamp-2">{{ item.description }}</p>

        <!-- 价格 -->
        <div class="mt-2 px-2.5 py-1 bg-orange-50 text-orange-500 font-black text-xs rounded-full border border-orange-100 flex items-center gap-1">
          <span>🏅</span> {{ item.price }}
        </div>

        <!-- 库存 -->
        <p v-if="item.stock >= 0" class="text-[10px] text-slate-400 mt-1.5 font-bold bg-slate-100 px-2 py-0.5 rounded-full">
          剩余: {{ item.stock }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useStudentStore } from '../../stores/student'

const store = useStudentStore()

onMounted(async () => {
  try { await store.fetchShop() } catch {}
})
</script>
