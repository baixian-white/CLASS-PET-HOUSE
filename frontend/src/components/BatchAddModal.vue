<template>
  <div class="fixed inset-0 bg-black/30 z-50 flex items-end sm:items-center justify-center"
    @click.self="$emit('close')">
    <div class="bg-white rounded-t-2xl sm:rounded-2xl shadow-xl w-full max-w-sm p-5">
      <h3 class="text-center font-bold text-gray-700 mb-3">批量添加学生</h3>
      <p class="text-xs text-gray-400 mb-1">一行一个姓名，自动过滤空行、超长和重复</p>
      <p class="text-xs text-gray-400 mb-2">当前班级 {{ currentStudentCount }}/{{ LIMITS.MAX_STUDENTS_PER_CLASS }} 人，还可添加 {{ remainingStudentSlots }} 人</p>
      <textarea v-model="text" rows="8" placeholder="张三&#10;李四&#10;王五"
        class="w-full px-3 py-2 rounded-lg border border-gray-200 outline-none
        focus:border-accent text-sm resize-none"></textarea>
      <div class="flex gap-2 mt-3">
        <button @click="$emit('close')"
          class="flex-1 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm">取消</button>
        <button @click="handleAdd" :disabled="loading || remainingStudentSlots <= 0"
          class="flex-1 py-2 bg-accent text-white rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed">
          {{ loading ? '添加中...' : '确认添加' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useClassStore } from '../stores/class'
import { useEscClose } from '../composables/useEscClose'
import api from '../utils/api'
import Dialog from '../utils/dialog'
import { LIMITS } from '../constants/limits'

const emit = defineEmits(['close', 'added'])
useEscClose(emit)
const classStore = useClassStore()
const text = ref('')
const loading = ref(false)
const currentStudentCount = computed(() => classStore.students?.length || 0)
const remainingStudentSlots = computed(() => Math.max(0, LIMITS.MAX_STUDENTS_PER_CLASS - currentStudentCount.value))

async function handleAdd() {
  if (!classStore.currentClass) return
  if (remainingStudentSlots.value <= 0) {
    Dialog.alert(`一个班级最多${LIMITS.MAX_STUDENTS_PER_CLASS}位学生`)
    return
  }

  const names = text.value.split('\n').map(s => s.trim()).filter(Boolean)
  if (!names.length) return
  if (names.length > 200) {
    Dialog.alert('单次最多添加200名学生')
    return
  }

  const existingNames = new Set(
    (classStore.students || [])
      .map(student => student.name?.trim())
      .filter(Boolean)
  )
  const creatableNames = []
  for (const rawName of names) {
    const name = rawName.trim()
    if (!name || name.length > 50 || existingNames.has(name)) continue
    creatableNames.push(name)
    existingNames.add(name)
  }
  if (!creatableNames.length) {
    Dialog.alert('没有可添加的新学生')
    return
  }
  if (creatableNames.length > remainingStudentSlots.value) {
    Dialog.alert(`当前班级最多还能添加${remainingStudentSlots.value}位学生`)
    return
  }

  loading.value = true
  try {
    await api.post('/students', {
      class_id: classStore.currentClass.id,
      names: creatableNames
    })
    await classStore.fetchStudents()
    emit('added')
  } catch (err) {
    Dialog.alert(err.error || '添加失败')
  } finally {
    loading.value = false
  }
}
</script>
