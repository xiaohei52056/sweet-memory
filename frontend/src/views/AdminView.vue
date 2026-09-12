<script setup>
// ============================================================
// 后台：登录 → 照片管理（悬浮+上传 / 搜索 / 分页 / 首页展示标记 / 下载）→ 回收站
// 网页端不调用麦克风；语音仅可在小程序端录制（此处可试听/移除）
// ============================================================
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import exifr from 'exifr'
import { useAuthStore } from '../stores/auth'
import { usePhotosStore } from '../stores/photos'
import { useUiStore } from '../stores/ui'

const router = useRouter()
const auth = useAuthStore()
const photosStore = usePhotosStore()
const ui = useUiStore()

// 点击照片查看详情（复用全局 Lightbox）
function viewDetail(p) {
  ui.openLightbox(filtered.value, filtered.value.findIndex((x) => x.id === p.id))
}

/* ---------- 登录 ---------- */
const loginForm = ref({ username: '', password: '' })
const loginErr = ref('')
const logging = ref(false)

async function doLogin() {
  loginErr.value = ''
  logging.value = true
  try {
    await auth.login(loginForm.value.username.trim(), loginForm.value.password)
    photosStore.fetchAll()
  } catch (e) {
    loginErr.value = e.message
  } finally {
    logging.value = false
  }
}

/* ---------- 页签 ---------- */
const tab = ref('manage')

/* ---------- 上传（左侧悬浮 + 号） ---------- */
const fileInput = ref(null)
const uploading = ref(false)

function pickFiles() {
  fileInput.value?.click()
}
function onFiles(e) {
  handleFiles([...(e.target.files || [])])
  e.target.value = ''
}

// 读取照片 EXIF 拍摄时间；exifr 解析失败或超时时返回 null（由调用方兜底为当天）
// 优先级：DateTimeOriginal（真实拍摄）> CreateDate > ModifyDate（部分照片仅有 tag 306）
function readTakenAt(file) {
  return Promise.race([
    exifr
      .parse(file, { DateTimeOriginal: true, CreateDate: true, ModifyDate: true })
      .then((d) => {
        const date = d?.DateTimeOriginal || d?.CreateDate || d?.ModifyDate
        if (!(date instanceof Date) || isNaN(date)) return null
        const p = (n) => String(n).padStart(2, '0')
        return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`
      })
      .catch(() => null),
    new Promise((r) => setTimeout(() => r(null), 3000)),
  ])
}

async function handleFiles(files) {
  const imgs = files.filter((f) => f.type.startsWith('image/'))
  if (!imgs.length) return
  uploading.value = true
  try {
    const items = []
    for (const f of imgs) {
      const takenAt = (await readTakenAt(f)) || new Date().toISOString().slice(0, 10)
      items.push({ file: f, takenAt, note: '' })
    }
    await photosStore.addPhotos(items)
  } catch (e) {
    alert(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

/* ---------- 搜索（留言模糊 / 年份 / 月份） ---------- */
const q = ref('')
const fYear = ref('')
const fMonth = ref('')
const MONTHS = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12']

const filtered = computed(() => {
  let list = photosStore.sorted.slice().reverse() // 最新在前
  // 被选为首页展示的照片排在最前
  list.sort((a, b) => (b.featured ? 1 : 0) - (a.featured ? 1 : 0))
  const kw = q.value.trim().toLowerCase()
  if (kw) list = list.filter((p) => (p.note || '').toLowerCase().includes(kw))
  if (fYear.value) list = list.filter((p) => p.takenAt.startsWith(fYear.value))
  if (fMonth.value) list = list.filter((p) => p.takenAt.slice(5, 7) === fMonth.value)
  return list
})

/* ---------- 分页（一页 40 张） ---------- */
const PAGE_SIZE = 40
const page = ref(1)
const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))
const paged = computed(() =>
  filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE)
)
watch([q, fYear, fMonth], () => (page.value = 1))
watch(pageCount, (n) => {
  if (page.value > n) page.value = n
})

/* ---------- 首页展示标记 ---------- */
const featuredCount = computed(() => photosStore.photos.filter((p) => p.featured).length)
function toggleFeatured(p) {
  photosStore.updatePhoto(p.id, { featured: !p.featured })
}

/* ---------- 下载 ---------- */
async function download(p) {
  if (!confirm(`下载这张照片？\n${p.takenAt}${p.note ? ' · ' + p.note : ''}`)) return
  try {
    const res = await fetch(p.url)
    if (!res.ok) throw new Error()
    const blob = await res.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${p.takenAt}_${p.id}.jpg`
    a.click()
    URL.revokeObjectURL(a.href)
  } catch {
    // 跨域不可 fetch 时退化为新窗口打开
    window.open(p.url, '_blank')
  }
}

/* ---------- 编辑 ---------- */
const editing = ref(null)
const noteLen = computed(() => (editing.value?.note || '').length)

function openEdit(p) {
  editing.value = { id: p.id, takenAt: p.takenAt, note: p.note || '' }
}
async function saveEdit() {
  if (!editing.value) return
  await photosStore.updatePhoto(editing.value.id, {
    takenAt: editing.value.takenAt,
    note: editing.value.note,
  })
  editing.value = null
}

/* ---------- 删除 / 回收站 ---------- */
async function trash(p) {
  await photosStore.trashPhoto(p.id)
}
async function restore(p) {
  await photosStore.restorePhoto(p.id)
}
async function destroy(p) {
  if (!confirm(`彻底删除这张回忆？此操作不可恢复。`)) return
  await photosStore.destroyPhoto(p.id)
}

/* ---------- 试听 ---------- */
const previewAudio = ref(null)
function preview(url) {
  previewAudio.value = url
}

onMounted(() => {
  if (auth.isAdmin) photosStore.fetchAll()
})
</script>

<template>
  <main class="admin">
    <!-- ============ 登录 ============ -->
    <section v-if="!auth.isAdmin" class="login-wrap">
      <form class="login glass" @submit.prevent="doLogin">
        <h1 class="serif">星河回忆</h1>
        <p class="login-sub">管理员入口</p>

        <label class="field">
          <span>账号</span>
          <input v-model="loginForm.username" autocomplete="username" placeholder="username" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="loginForm.password" type="password" autocomplete="current-password" placeholder="••••••••" />
        </label>

        <p v-if="loginErr" class="login-err">{{ loginErr }}</p>

        <button class="btn-gold login-btn" :disabled="logging">
          {{ logging ? '验证中…' : '进入' }}
        </button>

        <router-link to="/" class="login-back">返回星河</router-link>
      </form>
    </section>

    <!-- ============ 管理 ============ -->
    <section v-else class="panel">
      <header class="admin-header">
        <div class="admin-brand">
          <span class="serif">星河回忆</span>
          <em>后台</em>
        </div>

        <nav class="admin-tabs">
          <button :class="{ active: tab === 'manage' }" @click="tab = 'manage'">
            照片管理 <b>{{ photosStore.count }}</b>
          </button>
          <button :class="{ active: tab === 'trash' }" @click="tab = 'trash'">
            回收站 <b>{{ photosStore.trashed.length }}</b>
          </button>
        </nav>

        <div class="admin-user">
          <span>{{ auth.user?.nickname }}</span>
          <button class="btn-ghost" @click="router.push('/')">首页</button>
          <button class="btn-ghost" @click="auth.logout()">退出</button>
        </div>
      </header>

      <!-- 照片管理 -->
      <div v-if="tab === 'manage'" class="admin-body">
        <!-- 搜索栏 -->
        <div class="toolbar">
          <label class="search">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <circle cx="11" cy="11" r="7" />
              <path d="M20 20l-3.5-3.5" />
            </svg>
            <input v-model="q" placeholder="按留言模糊搜索…" />
          </label>

          <select v-model="fYear" class="select">
            <option value="">全部年份</option>
            <option v-for="y in photosStore.years.slice().reverse()" :key="y" :value="y">{{ y }} 年</option>
          </select>

          <select v-model="fMonth" class="select">
            <option value="">全部月份</option>
            <option v-for="m in MONTHS" :key="m" :value="m">{{ +m }} 月</option>
          </select>

          <span class="toolbar-meta">
            {{ filtered.length }} 张
            <em v-if="featuredCount">· 首页展示 {{ featuredCount }} 张</em>
          </span>
        </div>

        <!-- 网格 -->
        <div class="grid">
          <article v-for="p in paged" :key="p.id" class="cell" :class="{ featured: p.featured }" @click="viewDetail(p)">
            <img :src="p.thumb" :alt="p.note" loading="lazy" />
            <span v-if="p.featured" class="cell-flag">首页</span>
            <div class="cell-info">
              <span class="cell-date">{{ p.takenAt }}</span>
              <span class="cell-note">{{ p.note || '—' }}</span>
            </div>
            <div class="cell-actions" @click.stop>
              <button
                class="btn-icon"
                :class="{ on: p.featured }"
                :title="p.featured ? '取消首页展示' : '设为首页展示'"
                @click="toggleFeatured(p)"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" :fill="p.featured ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.6">
                  <path d="M12 2l1.8 6.2L20 10l-6.2 1.8L12 18l-1.8-6.2L4 10l6.2-1.8z" />
                </svg>
              </button>
              <button class="btn-icon" title="下载照片" @click="download(p)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M12 4v12m0 0l-5-5m5 5l5-5M4 20h16" />
                </svg>
              </button>
              <button v-if="p.audioUrl" class="btn-icon" title="试听语音" @click="preview(p.audioUrl)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
              </button>
              <button class="btn-icon" title="编辑" @click="openEdit(p)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M17 3l4 4L8 20H4v-4z" />
                </svg>
              </button>
              <button class="btn-icon danger" title="移入回收站" @click="trash(p)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M4 7h16M9 7V4h6v3m-8 0l1 13h8l1-13" />
                </svg>
              </button>
            </div>
          </article>
        </div>

        <p v-if="!paged.length" class="empty">没有匹配的回忆</p>

        <!-- 分页 -->
        <nav v-if="pageCount > 1" class="pager">
          <button class="btn-ghost" :disabled="page <= 1" @click="page--">上一页</button>
          <span class="pager-num">{{ page }} / {{ pageCount }}</span>
          <button class="btn-ghost" :disabled="page >= pageCount" @click="page++">下一页</button>
        </nav>
      </div>

      <!-- 回收站 -->
      <div v-else class="admin-body">
        <p v-if="!photosStore.trashed.length" class="empty">回收站是空的</p>
        <div class="grid">
          <article v-for="p in photosStore.trashed" :key="p.id" class="cell trashed">
            <img :src="p.thumb" :alt="p.note" loading="lazy" />
            <div class="cell-info">
              <span class="cell-date">{{ p.takenAt }}</span>
              <span class="cell-note">{{ p.note || '—' }}</span>
            </div>
            <div class="cell-actions">
              <button class="btn-icon" title="恢复" @click="restore(p)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M3 12a9 9 0 1 0 3-6.7M3 4v5h5" />
                </svg>
              </button>
              <button class="btn-icon danger" title="彻底删除" @click="destroy(p)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            </div>
          </article>
        </div>
      </div>

      <!-- 左侧悬浮上传按钮 -->
      <button
        v-if="tab === 'manage'"
        class="fab"
        :title="uploading ? '正在存入星河…' : '添加照片'"
        @click="pickFiles"
      >
        <input ref="fileInput" type="file" accept="image/*" multiple hidden @click.stop @change="onFiles" />
        <svg v-if="!uploading" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14" />
        </svg>
        <span v-else class="fab-spin"></span>
      </button>
    </section>

    <!-- ============ 编辑弹窗 ============ -->
    <Transition name="dlg">
      <div v-if="editing" class="dialog-mask" @click.self="editing = null">
        <div class="dialog glass">
          <h3 class="serif">编辑回忆</h3>

          <label class="field">
            <span>拍摄日期</span>
            <input v-model="editing.takenAt" type="date" />
          </label>

          <label class="field">
            <span>留言 <em class="cnt">{{ noteLen }}/20</em></span>
            <textarea v-model="editing.note" maxlength="20" rows="2" placeholder="写一句想说的话（20字以内）"></textarea>
          </label>

          <p class="dlg-tip">语音留言请在小程序端录制（网页端不开放麦克风）</p>

          <div class="dlg-actions">
            <button class="btn-ghost" @click="editing = null">取消</button>
            <button class="btn-gold" @click="saveEdit">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <audio v-if="previewAudio" :src="previewAudio" autoplay @ended="previewAudio = null"></audio>
  </main>
</template>

<style scoped>
.admin {
  position: relative;
  z-index: 1;
  height: 100%;
  overflow-y: auto;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(26, 42, 94, 0.35), transparent 50%),
    var(--bg-deep);
}

/* ---------- 登录 ---------- */
.login-wrap {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.login {
  width: min(92vw, 380px);
  border-radius: var(--r-lg);
  padding: 44px 38px 34px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  box-shadow: var(--shadow-card);
}
.login h1 {
  font-size: 30px;
  letter-spacing: 0.2em;
  text-align: center;
  color: var(--gold-bright);
  text-shadow: 0 0 30px rgba(232, 195, 126, 0.35);
}
.login-sub {
  text-align: center;
  font-size: 13px;
  letter-spacing: 0.4em;
  color: var(--ink-faint);
  margin-top: -10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field > span {
  font-size: 13px;
  letter-spacing: 0.2em;
  color: var(--ink-dim);
  display: flex;
  justify-content: space-between;
}
.field input,
.field textarea {
  background: rgba(233, 237, 246, 0.06);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  padding: 11px 14px;
  font-size: 15px;
  outline: none;
  color: var(--ink);
  transition: border-color 0.3s, box-shadow 0.3s;
  resize: none;
}
.field input:focus,
.field textarea:focus {
  border-color: var(--gold-dim);
  box-shadow: 0 0 0 3px var(--gold-faint);
}
.field input::placeholder,
.field textarea::placeholder {
  color: var(--ink-faint);
}

.login-err {
  color: var(--danger);
  font-size: 14px;
  text-align: center;
}
.login-btn {
  justify-content: center;
}
.login-back {
  text-align: center;
  font-size: 13px;
  color: var(--ink-faint);
  letter-spacing: 0.2em;
  text-decoration: none;
}
.login-back:hover {
  color: var(--gold);
}

/* ---------- 管理面板 ---------- */
.panel {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}
.admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 28px;
  background: var(--glass-strong);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--line);
}
.admin-brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.admin-brand span {
  font-size: 20px;
  letter-spacing: 0.2em;
  color: var(--gold-bright);
}
.admin-brand em {
  font-style: normal;
  font-size: 12px;
  letter-spacing: 0.3em;
  color: var(--ink-faint);
  border: 1px solid var(--line);
  padding: 2px 8px;
  border-radius: 999px;
}

.admin-tabs {
  display: flex;
  gap: 4px;
  background: rgba(233, 237, 246, 0.05);
  border-radius: 999px;
  padding: 4px;
}
.admin-tabs button {
  padding: 7px 20px;
  border-radius: 999px;
  font-size: 14px;
  color: var(--ink-dim);
  transition: all 0.3s;
}
.admin-tabs button b {
  font-weight: 500;
  opacity: 0.6;
  margin-left: 4px;
}
.admin-tabs button.active {
  background: linear-gradient(135deg, var(--gold-bright), var(--gold));
  color: #1a1408;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--ink-dim);
}

.admin-body {
  padding: 26px 28px 90px;
}

/* ---------- 搜索栏 ---------- */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
  flex-wrap: wrap;
}
.search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 200px;
  max-width: 360px;
  padding: 9px 14px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(233, 237, 246, 0.05);
  color: var(--ink-faint);
  transition: border-color 0.3s, box-shadow 0.3s;
}
.search:focus-within {
  border-color: var(--gold-dim);
  box-shadow: 0 0 0 3px var(--gold-faint);
  color: var(--gold-dim);
}
.search input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  color: var(--ink);
  font-size: 14px;
}
.search input::placeholder {
  color: var(--ink-faint);
}

.select {
  padding: 9px 12px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(233, 237, 246, 0.05);
  color: var(--ink-dim);
  font-size: 14px;
  outline: none;
  cursor: pointer;
}
.select option {
  background: #0a0e1c;
  color: var(--ink);
}

.toolbar-meta {
  margin-left: auto;
  font-size: 13px;
  color: var(--ink-faint);
  letter-spacing: 0.08em;
}
.toolbar-meta em {
  font-style: normal;
  color: var(--gold-dim);
}

/* ---------- 网格：固定一行五张 ---------- */
.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 18px;
}
.cell {
  position: relative;
  border-radius: var(--r-sm);
  overflow: hidden;
  border: 1px solid var(--line);
  background: #0a0e1c;
  aspect-ratio: 3 / 3.4;
  cursor: pointer;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.cell.featured {
  border-color: var(--gold-dim);
  box-shadow: 0 0 0 1px var(--gold-faint), 0 8px 26px rgba(0, 0, 0, 0.4);
}
.cell-flag {
  position: absolute;
  top: 8px;
  left: 8px;
  font-size: 11px;
  letter-spacing: 0.2em;
  color: #1a1408;
  background: linear-gradient(135deg, var(--gold-bright), var(--gold));
  padding: 2px 8px 2px 10px;
  border-radius: 999px;
  z-index: 2;
}
.cell img {
  width: 100%;
  height: 72%;
  object-fit: cover;
}
.cell.trashed img {
  filter: grayscale(0.7) brightness(0.7);
}
.cell-info {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cell-date {
  font-size: 13px;
  color: var(--gold-dim);
  letter-spacing: 0.08em;
}
.cell-note {
  font-size: 13px;
  color: var(--ink-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cell-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.25s;
  background: rgba(4, 6, 15, 0.55);
  backdrop-filter: blur(6px);
  border-radius: 999px;
  padding: 3px;
  z-index: 2;
}
.cell:hover .cell-actions {
  opacity: 1;
}
.cell-actions .btn-icon {
  width: 30px;
  height: 30px;
}
.cell-actions .btn-icon.on {
  color: var(--gold);
}
.cell-actions .danger:hover {
  color: var(--danger);
  background: rgba(224, 122, 122, 0.14);
}

.empty {
  text-align: center;
  color: var(--ink-faint);
  letter-spacing: 0.3em;
  padding: 60px 0;
}

/* ---------- 分页 ---------- */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 30px;
}
.pager-num {
  font-size: 14px;
  letter-spacing: 0.15em;
  color: var(--ink-dim);
}
.pager .btn-ghost:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ---------- 右下悬浮上传按钮 ---------- */
.fab {
  position: fixed;
  right: 30px;
  bottom: 34px;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--gold-bright), var(--gold));
  color: #1a1408;
  box-shadow: 0 10px 30px rgba(232, 195, 126, 0.35), 0 4px 14px rgba(0, 0, 0, 0.4);
  transition: transform 0.3s var(--ease-spring), box-shadow 0.3s;
  z-index: 20;
}
.fab:hover {
  transform: scale(1.1);
  box-shadow: 0 14px 40px rgba(232, 195, 126, 0.5), 0 6px 18px rgba(0, 0, 0, 0.45);
}
.fab-spin {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2.5px solid rgba(26, 20, 8, 0.25);
  border-top-color: #1a1408;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---------- 弹窗 ---------- */
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  background: rgba(3, 5, 12, 0.7);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.dialog {
  width: min(92vw, 400px);
  border-radius: var(--r-lg);
  padding: 30px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.dialog h3 {
  font-size: 20px;
  letter-spacing: 0.2em;
  color: var(--gold-bright);
}
.cnt {
  font-style: normal;
  color: var(--gold-dim);
}
.dlg-tip {
  font-size: 13px;
  color: var(--ink-faint);
}
.dlg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dlg-enter-active { transition: opacity 0.3s; }
.dlg-leave-active { transition: opacity 0.2s; }
.dlg-enter-from, .dlg-leave-to { opacity: 0; }
.dlg-enter-active .dialog { animation: dlgIn 0.35s var(--ease-spring); }
@keyframes dlgIn {
  from { transform: scale(0.92) translateY(14px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}

@media (max-width: 768px) {
  .admin-header { flex-wrap: wrap; padding: 12px 14px; }
  .admin-tabs { order: 3; width: 100%; justify-content: center; }
  .admin-body { padding: 16px 14px 90px; }
  .grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .fab { right: 16px; bottom: 20px; }
  .fab:hover { transform: scale(1.08); }
  .toolbar-meta { margin-left: 0; width: 100%; }
}
</style>
