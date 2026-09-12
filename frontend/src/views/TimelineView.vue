<script setup>
// ============================================================
// 时间线：横向电影胶卷
// 按年分组；拖拽/滚轮/触摸横移 + 惯性；顶部年份刻度跳转
// ============================================================
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePhotosStore } from '../stores/photos'
import { useUiStore } from '../stores/ui'

const router = useRouter()
const photosStore = usePhotosStore()
const ui = useUiStore()

const railRef = ref(null)
const yearRefs = ref({})
const activeYear = ref('')

/* ---------- 横向滚动 + 惯性 ---------- */
let vx = 0
let raf = 0
let dragging = false
let lastX = 0
let moved = 0
let downIndex = -1

function clampScroll() {
  const el = railRef.value
  if (!el) return
  el.scrollLeft = Math.max(0, Math.min(el.scrollLeft, el.scrollWidth - el.clientWidth))
}

function inertia() {
  cancelAnimationFrame(raf)
  const step = () => {
    if (dragging || Math.abs(vx) < 0.4) return
    railRef.value.scrollLeft += vx
    vx *= 0.94
    clampScroll()
    raf = requestAnimationFrame(step)
  }
  raf = requestAnimationFrame(step)
}

function onWheel(e) {
  e.preventDefault()
  cancelAnimationFrame(raf)
  railRef.value.scrollLeft += (e.deltaY + e.deltaX) * 1.1
}

function onPointerDown(e) {
  dragging = true
  moved = 0
  vx = 0
  lastX = e.clientX
  downIndex = e.target.closest('.tl-card')?.dataset.index ?? -1
  cancelAnimationFrame(raf)
}
function onPointerMove(e) {
  if (!dragging) return
  const dx = e.clientX - lastX
  moved += Math.abs(dx)
  railRef.value.scrollLeft -= dx
  vx = -dx * 0.9
  lastX = e.clientX
}
function onPointerUp(e) {
  if (!dragging) return
  dragging = false
  const card = e.target.closest('.tl-card')
  if (moved < 6 && card) {
    const gi = +card.dataset.group
    const ii = +card.dataset.index
    const photo = photosStore.byYear[gi].items[ii]
    ui.openLightbox(photosStore.byYear[gi].items, ii)
  } else {
    inertia()
  }
}

/* ---------- 年份导航 ---------- */
function jumpToYear(year) {
  const el = yearRefs.value[year]
  if (!el || !railRef.value) return
  railRef.value.scrollTo({
    left: el.offsetLeft - 60,
    behavior: 'smooth',
  })
}

function syncActiveYear() {
  const el = railRef.value
  if (!el) return
  const x = el.scrollLeft + el.clientWidth * 0.35
  let cur = photosStore.years[0]
  for (const g of photosStore.byYear) {
    const el2 = yearRefs.value[g.year]
    if (el2 && el2.offsetLeft <= x) cur = g.year
  }
  activeYear.value = cur
}

/* ---------- 滚动纵深联动 ---------- */
function emitDepth() {
  const el = railRef.value
  if (!el) return
  const max = el.scrollWidth - el.clientWidth
  window.dispatchEvent(
    new CustomEvent('starfield:depth', { detail: max > 0 ? el.scrollLeft / max : 0 })
  )
}

onMounted(async () => {
  await photosStore.fetchAll()
  await nextTick()
  activeYear.value = photosStore.years[0] || ''
  railRef.value?.addEventListener('wheel', onWheel, { passive: false })
  railRef.value?.addEventListener('scroll', () => {
    syncActiveYear()
    emitDepth()
  })
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  railRef.value?.removeEventListener('wheel', onWheel)
  window.dispatchEvent(new CustomEvent('starfield:depth', { detail: 0 }))
})
</script>

<template>
  <main class="tl">
    <!-- 顶部栏 -->
    <header class="tl-header">
      <button class="btn-ghost" @click="router.push('/')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M9 5l-7 7 7 7" transform="translate(2 0)" />
        </svg>
        返回星河
      </button>

      <nav class="tl-years serif">
        <button
          v-for="y in photosStore.years"
          :key="y"
          class="tl-year"
          :class="{ active: activeYear === y }"
          @click="jumpToYear(y)"
        >
          {{ y }}
        </button>
      </nav>

      <span class="tl-count">{{ photosStore.count }} 段回忆</span>
    </header>

    <!-- 横向胶卷 -->
    <div
      ref="railRef"
      class="tl-rail"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointerleave="onPointerUp"
    >
      <!-- 胶卷上下齿孔 -->
      <div class="film-edge top"></div>
      <div class="film-edge bottom"></div>

      <div class="tl-flow">
        <section
          v-for="(group, gi) in photosStore.byYear"
          :key="group.year"
          :ref="(el) => (yearRefs[group.year] = el)"
          class="tl-group"
        >
          <h2 class="tl-year-title serif">{{ group.year }}</h2>
          <div class="tl-cards">
            <button
              v-for="(p, ii) in group.items"
              :key="p.id"
              class="tl-card"
              :class="{ tilt: ii % 2 === 1 }"
              :data-group="gi"
              :data-index="ii"
            >
              <img :src="p.thumb" :alt="p.note" loading="lazy" />
              <span class="tl-card-date serif">{{ p.takenAt.slice(5).replace('-', '.') }}</span>
              <span v-if="p.audioUrl" class="tl-card-voice">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 14a3 3 0 0 0 3-3V6a3 3 0 1 0-6 0v5a3 3 0 0 0 3 3zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.9V21h2v-3.1A7 7 0 0 0 19 11z" />
                </svg>
              </span>
            </button>
          </div>
        </section>
      </div>
    </div>

    <p class="tl-hint">拖拽 · 回溯时光</p>
  </main>
</template>

<style scoped>
.tl {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tl-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 30px;
}

.tl-years {
  display: flex;
  gap: 6px;
  padding: 6px;
  border-radius: 999px;
  background: var(--glass);
  backdrop-filter: blur(14px);
  border: 1px solid var(--line);
  overflow-x: auto;
  max-width: 60vw;
}
.tl-year {
  padding: 6px 18px;
  border-radius: 999px;
  font-size: 15px;
  letter-spacing: 0.12em;
  color: var(--ink-dim);
  transition: all 0.35s var(--ease-out);
  flex: none;
}
.tl-year:hover {
  color: var(--ink);
}
.tl-year.active {
  color: #1a1408;
  background: linear-gradient(135deg, var(--gold-bright), var(--gold));
  box-shadow: var(--glow-gold);
}

.tl-count {
  font-size: 13px;
  letter-spacing: 0.25em;
  color: var(--ink-faint);
  flex: none;
}

/* ---------- 胶卷 ---------- */
.tl-rail {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  cursor: grab;
  position: relative;
  scrollbar-width: none;
}
.tl-rail::-webkit-scrollbar {
  display: none;
}
.tl-rail:active {
  cursor: grabbing;
}

.film-edge {
  position: absolute;
  left: 0;
  right: 0;
  height: 18px;
  pointer-events: none;
  z-index: 2;
  background-image: radial-gradient(circle at 12px 9px, rgba(233, 237, 246, 0.16) 3px, transparent 3.5px);
  background-size: 34px 18px;
  background-repeat: repeat-x;
}
.film-edge.top { top: 8px; }
.film-edge.bottom { bottom: 8px; }

.tl-flow {
  display: flex;
  align-items: center;
  gap: 70px;
  height: 100%;
  padding: 40px 8vw 40px 60px;
  width: max-content;
}

.tl-group {
  flex: none;
}
.tl-year-title {
  font-size: clamp(40px, 6vw, 72px);
  font-weight: 300;
  letter-spacing: 0.18em;
  color: transparent;
  -webkit-text-stroke: 1px var(--gold-dim);
  margin-bottom: 26px;
  user-select: none;
}

.tl-cards {
  display: flex;
  gap: 26px;
}
.tl-card {
  position: relative;
  flex: none;
  width: 210px;
  height: 262px;
  border-radius: var(--r-sm);
  overflow: hidden;
  border: 1px solid rgba(233, 237, 246, 0.14);
  background: #0a0e1c;
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.55);
  transform: rotate(-1.6deg);
  transition: transform 0.45s var(--ease-spring), box-shadow 0.45s, border-color 0.45s;
  padding: 0;
}
.tl-card.tilt {
  transform: rotate(1.8deg) translateY(14px);
}
.tl-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.tl-card:hover {
  transform: rotate(0) translateY(-8px) scale(1.05);
  border-color: var(--gold-dim);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.65), var(--glow-gold);
  z-index: 3;
}

.tl-card-date {
  position: absolute;
  left: 10px;
  bottom: 8px;
  font-size: 13px;
  letter-spacing: 0.16em;
  color: var(--gold-bright);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.8);
}
.tl-card-voice {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(4, 6, 15, 0.6);
  backdrop-filter: blur(4px);
  color: var(--gold);
}

.tl-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  letter-spacing: 0.5em;
  color: var(--ink-faint);
  pointer-events: none;
}

@media (max-width: 768px) {
  .tl-header { padding: 16px; flex-wrap: wrap; }
  .tl-years { max-width: 100vw; order: 3; width: 100%; }
  .tl-count { display: none; }
  .tl-card { width: 150px; height: 190px; }
  .tl-flow { gap: 40px; padding-left: 24px; }
}
</style>
