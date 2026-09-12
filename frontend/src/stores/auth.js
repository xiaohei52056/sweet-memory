import { defineStore } from 'pinia'
import { api, getToken, setToken } from '../api'

const KEY = 'galaxy-memory-admin'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: sessionStorage.getItem(KEY) && getToken()
      ? JSON.parse(sessionStorage.getItem(KEY))
      : null,
  }),

  getters: {
    isAdmin: (s) => !!s.user && !!getToken(),
  },

  actions: {
    async login(username, password) {
      const user = await api.login(username, password)
      this.user = user
      sessionStorage.setItem(KEY, JSON.stringify(user))
    },
    logout() {
      this.user = null
      setToken('')
      sessionStorage.removeItem(KEY)
    },
  },
})
