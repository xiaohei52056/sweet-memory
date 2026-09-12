// ============================================================
// API 抽象层：真实后端（Spring Boot）
// - 访客：可读取照片列表（首页/时间线/lightbox）
// - 管理员：登录获取 JWT，执行上传/编辑/回收站等操作
// - token 存于 sessionStorage，401 时自动清除登录态
// ============================================================

import axios from 'axios'

const BASE = import.meta.env.VITE_API_BASE || '/api'

export const http = axios.create({
  baseURL: BASE,
  timeout: 30000,
})

const TOKEN_KEY = 'galaxy-memory-token'

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}
export function setToken(t) {
  t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY)
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401 && getToken()) {
      setToken('')
      window.dispatchEvent(new CustomEvent('auth:expired'))
    }
    const msg = err.response?.data?.message || err.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

// 后端 Photo → 前端契约（id 转字符串，字段对齐）
function toPhoto(row) {
  return {
    id: String(row.id),
    url: row.url,
    thumb: row.thumb || row.url,
    takenAt: row.takenAt,
    note: row.note || '',
    audioUrl: row.audioUrl || null,
    audioDuration: row.audioDuration || 0,
    featured: !!row.featured,
    deleted: !!row.deleted,
  }
}

export const api = {
  /* ---------- 认证 ---------- */
  async login(username, password) {
    const data = await http.post('/login', { username, password })
    setToken(data.token)
    return { username: data.username, nickname: data.nickname }
  },

  /* ---------- 照片 ---------- */
  async listPhotos() {
    const rows = await http.get('/photos')
    return rows.map(toPhoto)
  },

  async listTrashed() {
    if (!getToken()) return []
    const rows = await http.get('/photos', { params: { trashed: true } })
    return rows.map(toPhoto)
  },

  /**
   * 批量上传：items = [{ file:File, takenAt, note, featured }]
   * 走 multipart：files[] 为图片，metas 为同名 JSON 数组
   */
  async addPhotos(items) {
    const form = new FormData()
    const metas = items.map((it) => ({
      takenAt: it.takenAt,
      note: it.note || '',
      featured: !!it.featured,
    }))
    items.forEach((it, i) => form.append('files', it.file, it.file.name || `photo-${i}.jpg`))
    form.append('metas', JSON.stringify(metas))
    const rows = await http.post('/photos', form)
    return rows.map(toPhoto)
  },

  async updatePhoto(id, patch) {
    const row = await http.patch(`/photos/${id}`, patch)
    return toPhoto(row)
  },

  async trashPhoto(id) {
    await http.post(`/photos/${id}/trash`)
  },

  async restorePhoto(id) {
    await http.post(`/photos/${id}/restore`)
  },

  async destroyPhoto(id) {
    await http.delete(`/photos/${id}/final`)
  },

  /** 上传语音（二期小程序使用） */
  async uploadAudio(id, file, duration) {
    const form = new FormData()
    form.append('file', file, file.name || 'voice.m4a')
    if (duration != null) form.append('duration', String(duration))
    const row = await http.post(`/photos/${id}/audio`, form)
    return toPhoto(row)
  },
}
