import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    lightbox: {
      open: false,
      photos: [],
      index: 0,
      editable: false,
    },
    // 管理页编辑弹窗请求：Lightbox 背面按钮写入，AdminView 消费
    editRequest: null,
  }),

  getters: {
    currentPhoto: (s) => s.lightbox.photos[s.lightbox.index] || null,
  },

  actions: {
    openLightbox(photos, index, opts = {}) {
      this.lightbox = { open: true, photos, index, editable: !!opts.editable }
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
