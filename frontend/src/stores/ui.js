import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    lightbox: {
      open: false,
      photos: [],
      index: 0,
    },
  }),

  getters: {
    currentPhoto: (s) => s.lightbox.photos[s.lightbox.index] || null,
  },

  actions: {
    openLightbox(photos, index) {
      this.lightbox = { open: true, photos, index }
    },
    closeLightbox() {
      this.lightbox.open = false
    },
    step(dir) {
      const n = this.lightbox.photos.length
      if (!n) return
      this.lightbox.index = (this.lightbox.index + dir + n) % n
    },
  },
})
