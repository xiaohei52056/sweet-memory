import { defineStore } from 'pinia'
import { api } from '../api'

export const usePhotosStore = defineStore('photos', {
  state: () => ({
    photos: [],
    trashed: [],
    loaded: false,
    loading: false,
  }),

  getters: {
    // 按拍摄日期升序
    sorted: (s) => [...s.photos].sort((a, b) => a.takenAt.localeCompare(b.takenAt)),
    // 首页展示照片：有标记则只展示标记的，否则回退为全部
    homePhotos(s) {
      const feat = this.sorted.filter((p) => p.featured)
      return feat.length ? feat : this.sorted
    },
    // 按年份分组（时间线用）
    byYear(s) {
      const map = new Map()
      for (const p of this.sorted) {
        const y = p.takenAt.slice(0, 4)
        if (!map.has(y)) map.set(y, [])
        map.get(y).push(p)
      }
      return [...map.entries()].map(([year, items]) => ({ year, items }))
    },
    years: (s) => s.byYear.map((g) => g.year),
    count: (s) => s.photos.length,
  },

  actions: {
    async fetchAll() {
      if (this.loading) return
      this.loading = true
      try {
        const [photos, trashed] = await Promise.all([api.listPhotos(), api.listTrashed()])
        this.photos = photos
        this.trashed = trashed
        this.loaded = true
      } finally {
        this.loading = false
      }
    },

    async addPhotos(items) {
      const created = await api.addPhotos(items)
      this.photos = this.photos.concat(created)
      return created
    },

    async updatePhoto(id, patch) {
      const updated = await api.updatePhoto(id, patch)
      this.photos = this.photos.map((p) => (p.id === id ? updated : p))
      this.trashed = this.trashed.map((p) => (p.id === id ? updated : p))
      return updated
    },

    async uploadAudio(id, file, duration) {
      const updated = await api.uploadAudio(id, file, duration)
      this.photos = this.photos.map((p) => (p.id === id ? updated : p))
      this.trashed = this.trashed.map((p) => (p.id === id ? updated : p))
      return updated
    },

    async trashPhoto(id) {
      await api.trashPhoto(id)
      const p = this.photos.find((x) => x.id === id)
      this.photos = this.photos.filter((x) => x.id !== id)
      if (p) this.trashed.push({ ...p, deleted: true })
    },

    async restorePhoto(id) {
      await api.restorePhoto(id)
      const p = this.trashed.find((x) => x.id === id)
      this.trashed = this.trashed.filter((x) => x.id !== id)
      if (p) this.photos.push({ ...p, deleted: false })
    },

    async destroyPhoto(id) {
      await api.destroyPhoto(id)
      this.trashed = this.trashed.filter((x) => x.id !== id)
    },
  },
})
