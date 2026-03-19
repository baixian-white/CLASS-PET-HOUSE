<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <h2 class="text-xl font-bold text-gray-700">⚙️ 设置</h2>

    <!-- 系统名称 & 班级名称 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm space-y-4">
      <div>
        <label class="block text-sm text-gray-500 mb-1">系统名称</label>
        <input v-model="systemName" type="text"
          class="w-full px-3 py-2 rounded-lg border border-gray-200 outline-none focus:border-accent" />
      </div>
      <div>
        <label class="block text-sm text-gray-500 mb-1">当前班级名称</label>
        <input v-model="className" type="text"
          class="w-full px-3 py-2 rounded-lg border border-gray-200 outline-none focus:border-accent" />
      </div>
    </div>

    <!-- 学生名单管理 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm">
      <h3 class="font-bold text-gray-700 mb-3">👨‍🎓 学生名单管理</h3>
      <div class="flex flex-col sm:flex-row gap-2 mb-3">
        <input v-model="newStudentName" type="text" placeholder="输入学生姓名"
          @keyup.enter="addStudent"
          class="flex-1 px-3 py-2 rounded-lg border border-gray-200 outline-none text-sm" />
        <button @click="addStudent"
          class="px-4 py-2 bg-accent text-white rounded-lg text-sm">添加</button>
        <button @click="showBatchAdd = true"
          class="px-4 py-2 bg-gray-100 text-gray-600 rounded-lg text-sm">批量添加</button>
      </div>

      <!-- 学生列表 -->
      <div class="space-y-2 max-h-60 overflow-y-auto">
        <div v-for="s in classStore.students" :key="s.id"
          class="flex items-center justify-between p-2 rounded-lg bg-gray-50">
          <span class="text-sm text-gray-700">{{ s.name }}</span>
          <div class="flex gap-1">
            <button @click="editStudent(s)" class="text-gray-400 hover:text-gray-600 text-sm">✏️</button>
            <button @click="deleteStudent(s)" class="text-gray-400 hover:text-red-500 text-sm">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 积分规则管理 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm">
      <h3 class="font-bold text-gray-700 mb-3">📋 加分项目管理</h3>
      <div class="space-y-2 max-h-60 overflow-y-auto mb-3">
        <div v-for="r in rules" :key="r.id"
          class="flex items-center justify-between p-2 rounded-lg bg-gray-50">
          <span class="text-sm">{{ r.icon }} {{ r.name }}
            <span :class="r.value > 0 ? 'text-green-500' : 'text-red-500'">
              {{ r.value > 0 ? '+' : '' }}{{ r.value }}
            </span>
          </span>
          <div class="flex gap-1">
            <button @click="deleteRule(r)" class="text-gray-400 hover:text-red-500 text-sm">🗑️</button>
          </div>
        </div>
      </div>
      <div class="mt-2 grid grid-cols-1 md:grid-cols-2 gap-3">
        <div class="p-3 rounded-xl border border-gray-100 bg-gray-50">
          <div class="flex items-center justify-between mb-2">
            <div class="text-sm font-semibold text-gray-600">自定义规则导入</div>
            <button @click="importCustomRules"
              class="px-3 py-1.5 bg-gray-100 text-gray-600 rounded-lg text-xs hover:bg-gray-200">
              导入
            </button>
          </div>
          <textarea ref="customImportTextarea" v-model="customImportText" rows="4"
            placeholder="每行一条：名称,图标,分值（图标可省略）&#10;示例：早读打卡,📖,1"
            class="w-full px-3 py-2 rounded-lg border text-sm outline-none bg-white"></textarea>
          <div class="flex items-center justify-between mt-2">
            <p class="text-xs text-gray-400">支持分隔符：逗号/中文逗号/竖线；缺省图标会自动根据分值生成。</p>
            <button @click="customImportText = ''" class="px-3 py-1.5 bg-white text-gray-500 rounded-lg text-xs border border-gray-200">清空</button>
          </div>
          <div class="mt-2">
            <div class="text-xs text-gray-500 mb-1">图标库（点击插入）</div>
            <div class="flex flex-wrap gap-1.5">
              <button v-for="ic in iconLibrary" :key="ic" @click="insertIcon(ic)"
                class="w-8 h-8 rounded-lg bg-white border border-gray-200 text-lg flex items-center justify-center hover:bg-gray-100">
                {{ ic }}
              </button>
            </div>
          </div>
        </div>
        <div class="p-3 rounded-xl border border-gray-100 bg-gray-50">
          <div class="flex items-center justify-between mb-2">
            <div class="text-sm font-semibold text-gray-600">模板规则导入</div>
            <button @click="importTemplates" :disabled="!selectedTemplates.length"
              class="px-3 py-1.5 bg-gray-100 text-gray-600 rounded-lg text-xs disabled:opacity-50 hover:bg-gray-200">
              批量导入所选
            </button>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-48 overflow-y-auto">
            <label v-for="tpl in templateOptions" :key="tpl.key"
              class="flex items-center gap-2 p-2 rounded-lg border border-gray-100 bg-white text-sm">
              <input type="checkbox" :value="tpl.key" v-model="selectedTemplateKeys" :disabled="tpl.exists"
                class="accent-blue-500" />
              <span class="flex-1 text-gray-700">{{ tpl.icon }} {{ tpl.name }}
                <span class="text-gray-500">({{ tpl.value > 0 ? '+' : '' }}{{ tpl.value }})</span>
              </span>
              <span v-if="tpl.exists" class="text-xs text-gray-400">已存在</span>
            </label>
          </div>
          <p class="text-xs text-gray-400 mt-1">勾选后点击“批量导入所选”，系统会把模板规则批量添加到当前班级。</p>
        </div>
      </div>
    </div>

    <!-- 界面主题 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm">
      <h3 class="font-bold text-gray-700 mb-3">🎨 界面主题</h3>
      <div class="flex flex-wrap gap-2">
        <button v-for="t in themes" :key="t.id" @click="currentTheme = t.id; setTheme(t.id)"
          :class="currentTheme === t.id ? 'ring-2 ring-offset-2 ring-accent' : ''"
          class="w-8 h-8 rounded-full transition" :style="{ backgroundColor: t.color }">
        </button>
      </div>
    </div>

    <!-- 成长阶段配置 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm">
      <h3 class="font-bold text-gray-700 mb-3">📊 成长阶段配置</h3>
      <p class="text-xs text-gray-400 mb-2">设置宠物每个阶段所需的食物数量（逗号分隔）</p>
      <input v-model="growthStagesText" type="text" placeholder="0,5,10,20,30,45,60,75,90,100"
        class="w-full px-3 py-2 rounded-lg border border-gray-200 outline-none text-sm focus:border-accent" />
    </div>

    <!-- 班级工具 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm space-y-3">
      <h3 class="font-bold text-gray-700 mb-3">🛠️ 班级工具</h3>
      <button @click="randomAssignPets"
        class="w-full py-2 bg-theme-light text-theme rounded-lg text-sm hover:opacity-80">🐾 一键随机分配宠物</button>
      <button @click="randomAssignGroups"
        class="w-full py-2 bg-theme-light text-theme rounded-lg text-sm hover:opacity-80">🎲 随机分组</button>
      <button @click="resetAllProgress"
        class="w-full py-2 bg-red-50 text-red-500 rounded-lg text-sm hover:bg-red-100">🔄 全班进度重置</button>
      <div v-if="classStore.classes.length > 1" class="flex gap-2 items-center">
        <select v-model="copyFromClassId" class="flex-1 px-3 py-2 rounded-lg border text-sm outline-none">
          <option value="">选择源班级</option>
          <option v-for="c in classStore.classes.filter(c => c.id !== classStore.currentClass?.id)" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <button @click="copyConfig" :disabled="!copyFromClassId"
          class="px-4 py-2 bg-accent text-white rounded-lg text-sm disabled:opacity-50">📋 复制配置</button>
      </div>
    </div>

    <!-- 🤖 AI 智能助手 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm space-y-3">
      <h3 class="font-bold text-gray-700 mb-3">🤖 AI 智能助手</h3>
      <button @click="showAiReport = true"
        class="w-full py-2.5 bg-gradient-to-r from-violet-500 to-purple-500 text-white rounded-lg text-sm font-bold hover:shadow-lg transition-all">📊 AI 生成班级周报</button>
      <p class="text-xs text-gray-400">基于全班积分数据，AI自动生成周报/月报，可直接复制发家长群</p>
    </div>

    <!-- 账号管理 -->
    <div class="bg-white rounded-2xl p-5 shadow-sm space-y-3">
      <h3 class="font-bold text-gray-700 mb-3">👤 账号管理</h3>
      <button @click="changePassword"
        class="w-full py-2 bg-gray-100 text-gray-600 rounded-lg text-sm hover:bg-gray-200">🔑 修改密码</button>
      <button @click="handleLogout"
        class="w-full py-2 bg-red-50 text-red-500 rounded-lg text-sm hover:bg-red-100">🚪 退出登录</button>
    </div>

    <!-- 保存按钮 -->
    <div class="pt-4 pb-8">
      <button @click="saveSettings"
        class="w-full sm:w-auto px-10 py-3.5 bg-accent text-white rounded-2xl font-bold shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all active:scale-95 flex items-center justify-center gap-2 text-base mx-auto">
        <span class="text-xl">💾</span> 保存全部设置
      </button>
    </div>

    <!-- 批量添加弹窗 -->
    <BatchAddModal
      v-if="showBatchAdd"
      @close="showBatchAdd = false"
      @added="showBatchAdd = false"
    />

    <!-- AI 周报弹窗 -->
    <AiReportModal
      v-if="showAiReport"
      :show="showAiReport"
      :class-id="classStore.currentClass?.id"
      @close="showAiReport = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useClassStore } from '../stores/class'
import { useAuthStore } from '../stores/auth'
import api from '../utils/api'
import BatchAddModal from '../components/BatchAddModal.vue'
import AiReportModal from '../components/AiReportModal.vue'
import { useTheme } from '../composables/useTheme'
import { PETS } from '../utils/pets'
import Dialog from '../utils/dialog'

const router = useRouter()
const classStore = useClassStore()
const authStore = useAuthStore()
const { setTheme } = useTheme()

const systemName = ref('')
const className = ref('')
const newStudentName = ref('')
const showBatchAdd = ref(false)
const rules = ref([])
const currentTheme = ref('pink')
const selectedTemplateKeys = ref([])
const customImportText = ref('')
const customImportTextarea = ref(null)

const iconLibrary = [
  '⭐', '🌟', '✨', '🏅', '📖', '📝', '✍️', '📚', '🎯', '✅',
  '🙋', '🤝', '👏', '💪', '📈', '🧹', '🏃', '⏰', '🚫', '⚠️',
  '💔', '😅', '😇', '😊', '🥇', '🎉', '🍀', '🎵', '🧠', '🔔'
]

const ruleTemplates = [
  { key: 'early_read', name: '早读打卡', icon: '📖', value: 1 },
  { key: 'homework_good', name: '作业优秀', icon: '⭐', value: 3 },
  { key: 'class_good', name: '课堂表现好', icon: '🙋', value: 2 },
  { key: 'help_others', name: '帮助同学', icon: '🤝', value: 2 },
  { key: 'exam_progress', name: '考试进步', icon: '📈', value: 5 },
  { key: 'duty_good', name: '值日认真', icon: '🧹', value: 1 },
  { key: 'sports_good', name: '运动达标', icon: '🏃', value: 2 },
  { key: 'late', name: '迟到', icon: '⏰', value: -1 },
  { key: 'homework_miss', name: '未交作业', icon: '📝', value: -2 },
  { key: 'class_break', name: '课堂违纪', icon: '🚫', value: -2 },
  { key: 'fight', name: '打架', icon: '👊', value: -5 },
  { key: 'dirty_words', name: '说脏话', icon: '🤐', value: -1 },
  { key: 'discipline', name: '不守纪律', icon: '⚠️', value: -1 },
  { key: 'damage', name: '损坏公物', icon: '💔', value: -3 }
]

const existingRuleNameSet = computed(() => {
  return new Set((rules.value || []).map(r => (r.name || '').trim()))
})

const templateOptions = computed(() => {
  return ruleTemplates.map(t => ({
    ...t,
    exists: existingRuleNameSet.value.has(t.name)
  }))
})

const selectedTemplates = computed(() => {
  return templateOptions.value.filter(t => selectedTemplateKeys.value.includes(t.key) && !t.exists)
})
const growthStagesText = ref('')
const copyFromClassId = ref('')
const showAiReport = ref(false)

const themes = [
  { id: 'pink', color: '#f472b6' },
  { id: 'blue', color: '#60a5fa' },
  { id: 'green', color: '#4ade80' },
  { id: 'purple', color: '#a78bfa' },
  { id: 'orange', color: '#fb923c' },
  { id: 'red', color: '#f87171' },
  { id: 'teal', color: '#2dd4bf' },
  { id: 'yellow', color: '#facc15' },
]

async function loadClassData() {
  if (!classStore.currentClass) return
  systemName.value = classStore.currentClass.system_name || ''
  className.value = classStore.currentClass.name || ''
  currentTheme.value = classStore.currentClass.theme || 'pink'
  growthStagesText.value = (classStore.currentClass.growth_stages || [0,5,10,20,30,45,60,75,90,100]).join(',')
  await Promise.all([
    classStore.fetchStudents(),
    classStore.fetchScoreRules()
  ])
  rules.value = [...classStore.scoreRules]
}

onMounted(async () => {
  await classStore.fetchClasses()
  await loadClassData()
})

watch(() => classStore.currentClass?.id, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await loadClassData()
  }
})

async function addStudent() {
  if (!newStudentName.value.trim()) return
  try {
    await api.post('/students', {
      class_id: classStore.currentClass.id,
      name: newStudentName.value.trim()
    })
    newStudentName.value = ''
    await classStore.fetchStudents()
  } catch (err) { Dialog.alert(err.error || '添加失败') }
}

async function editStudent(s) {
  const name = await Dialog.prompt('修改学生姓名', s.name)
  if (!name || name === s.name) return
  try {
    await api.put(`/students/${s.id}`, { name })
    await classStore.fetchStudents()
  } catch (err) { Dialog.alert(err.error || '修改失败') }
}

async function deleteStudent(s) {
  if (!(await Dialog.confirm(`确定删除"${s.name}"？`))) return
  try {
    await api.delete(`/students/${s.id}`)
    await classStore.fetchStudents()
  } catch (err) { Dialog.alert(err.error || '删除失败') }
}

async function deleteRule(r) {
  if (!(await Dialog.confirm(`确定删除"${r.name}"？`))) return
  try {
    await api.delete(`/score-rules/${r.id}`)
    rules.value = rules.value.filter(x => x.id !== r.id)
    classStore.scoreRules = classStore.scoreRules.filter(x => x.id !== r.id)
  } catch (err) { Dialog.alert(err.error || '删除失败') }
}

async function importTemplates() {
  if (!selectedTemplates.value.length) return
  const currentCount = rules.value.length
  if (currentCount + selectedTemplates.value.length > 50) {
    Dialog.alert('最多创建50条规则，请减少选择的模板数量')
    return
  }
  try {
    const created = []
    for (const tpl of selectedTemplates.value) {
      const rule = await api.post('/score-rules', {
        class_id: classStore.currentClass.id,
        name: tpl.name,
        icon: tpl.icon,
        value: tpl.value
      })
      created.push(rule)
    }
    if (created.length) {
      rules.value.push(...created)
      classStore.scoreRules.push(...created)
    }
    selectedTemplateKeys.value = []
    Dialog.alert(`已导入 ${created.length} 条规则`)
  } catch (err) {
    Dialog.alert(err.error || '批量导入失败')
  }
}

function insertIcon(icon) {
  const el = customImportTextarea.value
  const text = customImportText.value || ''
  if (el && typeof el.selectionStart === 'number') {
    const start = el.selectionStart
    const end = el.selectionEnd
    customImportText.value = text.slice(0, start) + icon + text.slice(end)
    nextTick(() => {
      el.focus()
      const pos = start + icon.length
      el.setSelectionRange(pos, pos)
    })
  } else {
    customImportText.value = text + icon
  }
}

function parseCustomRules(text) {
  const lines = (text || '').split(/\r?\n/).map(l => l.trim()).filter(Boolean)
  const parsed = []
  for (const line of lines) {
    const parts = line.split(/[,，|]/).map(p => p.trim()).filter(Boolean)
    if (parts.length < 2) continue
    const name = parts[0]
    let icon = parts.length >= 3 ? parts[1] : ''
    const valueStr = parts.length >= 3 ? parts[2] : parts[1]
    const value = parseInt(valueStr, 10)
    if (!name || isNaN(value) || value === 0) continue
    if (!icon) icon = value > 0 ? '⭐' : '⚠️'
    parsed.push({ name, icon, value })
  }
  return parsed
}

async function importCustomRules() {
  const items = parseCustomRules(customImportText.value)
  if (!items.length) {
    Dialog.alert('没有可导入的规则，请检查格式')
    return
  }
  const existing = new Set((rules.value || []).map(r => (r.name || '').trim()))
  const unique = []
  const seen = new Set()
  for (const item of items) {
    const key = (item.name || '').trim()
    if (!key || existing.has(key) || seen.has(key)) continue
    seen.add(key)
    unique.push(item)
  }
  if (!unique.length) {
    Dialog.alert('没有可导入的规则（可能都已存在）')
    return
  }
  if (rules.value.length + unique.length > 50) {
    Dialog.alert('最多创建50条规则，请减少导入数量')
    return
  }
  try {
    const created = []
    for (const item of unique) {
      const rule = await api.post('/score-rules', {
        class_id: classStore.currentClass.id,
        name: item.name,
        icon: item.icon,
        value: item.value
      })
      created.push(rule)
    }
    if (created.length) {
      rules.value.push(...created)
      classStore.scoreRules.push(...created)
    }
    customImportText.value = ''
    Dialog.alert(`已导入 ${created.length} 条规则`)
  } catch (err) {
    Dialog.alert(err.error || '导入失败')
  }
}

async function changePassword() {
  const oldPwd = await Dialog.prompt('请输入旧密码')
  if (!oldPwd) return
  const newPwd = await Dialog.prompt('请输入新密码（至少6位）')
  if (!newPwd) return
  try {
    await api.put('/auth/change-password', { oldPassword: oldPwd, newPassword: newPwd })
    Dialog.alert('密码修改成功')
  } catch (err) { Dialog.alert(err.error || '修改失败') }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

async function saveSettings() {
  try {
    const updateData = {
      system_name: systemName.value,
      name: className.value,
      theme: currentTheme.value
    }
    // 解析成长阶段
    if (growthStagesText.value) {
      const stages = growthStagesText.value.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
      if (stages.length >= 2) updateData.growth_stages = stages
    }
    await api.put(`/classes/${classStore.currentClass.id}`, updateData)
    await classStore.fetchClasses()
    Dialog.alert('设置已保存')
  } catch (err) { Dialog.alert(err.error || '保存失败') }
}

async function randomAssignPets() {
  if (!(await Dialog.confirm('将为所有未分配宠物的学生随机分配，确定？'))) return
  try {
    await api.post('/students/random-pets', {
      class_id: classStore.currentClass.id,
      pets: PETS
    })
    await classStore.fetchStudents()
    Dialog.alert('随机分配完成')
  } catch (err) { Dialog.alert(err.error || '分配失败') }
}

async function randomAssignGroups() {
  if (!(await Dialog.confirm('将打乱所有学生并随机分配到现有分组，确定？'))) return
  try {
    await api.post('/groups/random-assign', {
      class_id: classStore.currentClass.id
    })
    await classStore.fetchGroups()
    await classStore.fetchStudents()
    Dialog.alert('随机分组完成')
  } catch (err) { Dialog.alert(err.error || '分组失败') }
}

async function resetAllProgress() {
  if (!(await Dialog.confirm('⚠️ 将重置全班所有学生的食物和宠物，此操作不可撤回！确定？'))) return
  try {
    await api.post('/students/reset-all', {
      class_id: classStore.currentClass.id
    })
    await classStore.fetchStudents()
    Dialog.alert('全班进度已重置')
  } catch (err) { Dialog.alert(err.error || '重置失败') }
}

async function copyConfig() {
  if (!copyFromClassId.value) return
  if (!(await Dialog.confirm('将从源班级复制积分规则、商品和成长阶段到当前班级，确定？'))) return
  try {
    await api.post('/classes/copy-config', {
      from_class_id: copyFromClassId.value,
      to_class_id: classStore.currentClass.id
    })
    await classStore.fetchClasses()
    await classStore.fetchScoreRules()
    rules.value = [...classStore.scoreRules]
    Dialog.alert('配置复制成功')
  } catch (err) { Dialog.alert(err.error || '复制失败') }
}
</script>
