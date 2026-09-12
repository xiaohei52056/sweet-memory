<script setup>
// ============================================================
// 底部照片轮播条：从左向右无限循环
// 悬停暂停 + 放大；点击打开 Lightbox；移动端可拖拽
// ============================================================
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { usePhotosStore } from '../stores/photos'
import { useUiStore } from '../stores/ui'

const photosStore = usePhotosStore()
const ui = useUiStore()

const stripRef = ref(null)
const trackRef = ref(null)

// 轮播状态
let offset = 0
let setWidth = 0
let speed = 0
const BASE_SPEED = 42 // px/s，向右
let hovered = false
let dragging = false
let lastX = 0
let moved = 0
let raf = 0
let lastT = 0

// 首页展示照片（后台可勾选）
const homePhotos = computed(() => photosStore.homePhotos)

// 每张卡片一个稳定的微倾斜/偏移，营造散落感
const jitters = computed(() =>
  homePhotos.value.map((p, i) => ({
    rot: ((i * 7) % 5) - 2, // -2 ~ 2 deg
    dy: ((i * 13) % 4) * 3, // 0/3/6/9 px
  }))
)

function measure() {
  if (!trackRef.value) return
  // 一组 = 三分之一 track 宽度（渲染了三份）
  setWidth = trackRef.value.scrollWidth / 3
}

function tick(t) {
  raf = requestAnimationFrame(tick)
  const dt = Math.min((t - lastT) / 1000, 0.05)
  lastT = t
  if (!setWidth) measure()
  if (!setWidth) return

  const target = hovered || dragging ? 0 : BASE_SPEED
  speed += (target - speed) * 0.08
  offset += speed * dt

  if (offset > 0) offset -= setWidth
  if (offset < -setWidth) offset += setWidth

  trackRef.value.style.transform = `translate3d(${offset}px,0,0)`
}

function openAt(index) {
  ui.openLightbox(homePhotos.value, index)
}

/* ---------- 拖拽（移动端） ---------- */
function onPointerDown(e) {
  dragging = true
  moved = 0
  lastX = e.clientX ?? e.touches?.[0]?.clientX ?? 0
}
function onPointerMove(e) {
  if (!dragging) return
  const x = e.clientX ?? e.touches?.[0]?.clientX ?? 0
  const dx = x - lastX
  moved += Math.abs(dx)
  offset += dx
  lastX = x
}
function onPointerUp() {
  dragging = false
}

onMounted(() => {
  raf = requestAnimationFrame((t) => {
    lastT = t
    offset = -setWidth || 0
    tick(t)
  })
  window.addEventListener('resize', measure)
})
onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', measure)
})
</script>

<template>
  <div
    ref="stripRef"
    class="strip"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointerleave="onPointerUp"
  >
    <div ref="trackRef" class="strip-track">
      <template v-for="copy in 3" :key="copy">
        <button
          v-for="(p, i) in homePhotos"
          :key="`${copy}-${p.id}`"
          class="strip-card"
          :style="{
            transform: `rotate(${jitters[i].rot}deg) translateY(${jitters[i].dy}px)`,
          }"
          @click="moved < 6 && openAt(i)"
        >
          <img :src="p.thumb" :alt="p.note" loading="lazy" />
        </button>
      </template>
    </div>
    <div class="strip-fade left"></div>
    <div class="strip-fade right"></div>
  </div>
</template>

<style scoped>
.strip {
  position: relative;
  width: 100%;
  overflow: hidden;
  padding: 26px 0 34px;
  cursor: grab;
  touch-action: pan-y;
}
.strip:active {
  cursor: grabbing;
}

.strip-track {
  display: flex;
  gap: 18px;
  width: max-content;
  padding-left: 18px;
  will-change: transform;
}

.strip-card {
  flex: none;
  width: 148px;
  height: 186px;
  border-radius: var(--r-sm);
  overflow: hidden;
  border: 1px solid rgba(233, 237, 246, 0.14);
  background: #0a0e1c;
  box-shadow: 0 8px 26px rgba(0, 0, 0, 0.5);
  transition:
    transform 0.45s var(--ease-spring),
    box-shadow 0.45s var(--ease-out),
    border-color 0.45s;
  padding: 0;
}
.strip-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s var(--ease-out);
}
.strip-card:hover {
  transform: rotate(0deg) translateY(-10px) scale(1.08) !important;
  border-color: var(--gold-dim);
  box-shadow: 0 16px 44px rgba(0, 0, 0, 0.6), var(--glow-gold);
  z-index: 2;
}
.strip-card:hover img {
  transform: scale(1.06);
}

/* 两侧渐隐 */
.strip-fade {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 14vw;
  pointer-events: none;
  z-index: 3;
}
.strip-fade.left {
  left: 0;
  background: linear-gradient(90deg, var(--bg-deep) 8%, transparent);
}
.strip-fade.right {
  right: 0;
  background: linear-gradient(-90deg, var(--bg-deep) 8%, transparent);
}

@media (max-width: 768px) {
  .strip-card {
    width: 108px;
    height: 138px;
  }
  .strip-track {
    gap: 12px;
  }
}
</style>
