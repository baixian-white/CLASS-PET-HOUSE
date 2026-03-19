import { defineStore } from 'pinia'
import api from '../utils/api'

export const useClassStore = defineStore('class', {
  state: () => ({
    classes: [],
    currentClass: null,
    students: [],
    groups: [],
    scoreRules: [],
    loading: false,
    initialized: false
  }),
  actions: {
    async fetchClasses() {
      this.classes = await api.get('/classes')
      const savedId = localStorage.getItem('currentClassId')
      if (savedId) {
        const found = this.classes.find(c => String(c.id) === String(savedId))
        if (found) this.currentClass = found
      }
      if (!this.currentClass || !this.classes.find(c => c.id === this.currentClass.id)) {
        this.currentClass = this.classes.length ? this.classes[0] : null
      }
      if (this.currentClass) {
        localStorage.setItem('currentClassId', this.currentClass.id)
      } else {
        localStorage.removeItem('currentClassId')
      }
      this.initialized = true
    },
    async switchClass(cls) {
      this.currentClass = cls
      if (cls?.id) localStorage.setItem('currentClassId', cls.id)
      await this.fetchStudents()
      await this.fetchGroups()
      await this.fetchScoreRules()
    },
    async fetchStudents() {
      if (!this.currentClass) return
      this.students = await api.get(`/students/class/${this.currentClass.id}`)
    },
    async fetchGroups() {
      if (!this.currentClass) return
      this.groups = await api.get(`/groups/class/${this.currentClass.id}`)
    },
    async fetchScoreRules() {
      if (!this.currentClass) return
      this.scoreRules = await api.get(`/score-rules/class/${this.currentClass.id}`)
    }
  }
})
