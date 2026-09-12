<script setup>
// ============================================================
// 首页：星空 + 「星河回忆」标题 + 诗句文案 + 底部照片轮播条
// 入场特效：标题逐字晕开 → 金线展开 → 诗句上浮
// 滚轮 → 星场纵深（dispatch starfield:depth）
// ============================================================
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import PhotoStrip from '../components/PhotoStrip.vue'
import { usePhotosStore } from '../stores/photos'

const router = useRouter()
const photosStore = usePhotosStore()

const titleRef = ref(null)
const TITLE = '星河回忆'
const POEM = ['把每一次相遇，折叠成星', '当夜空亮起', '记忆便有了形状']

/* ---------- 滚轮纵深 ---------- */
let depth = 0
function onWheel(e) {
  depth += e.deltaY * 0.00045
  depth = Math.min(1, Math.max(0, depth))
  window.dispatchEvent(new CustomEvent('starfield:depth', { detail: depth }))
}

onMounted(async () => {
  photosStore.fetchAll()
  window.addEventListener('wheel', onWheel, { passive: true })

  // 入场时间线：标题逐字晕开 → 金线展开 → 诗句三行
  const tl = gsap.timeline({ delay: 0.35 })
  const chars = titleRef.value?.querySelectorAll('.t-char')
  if (chars?.length) {
    // 墨晕入场：blur 收拢 + 轻微缩放，像星光在夜空中渐渐亮起
    tl.fromTo(
      chars,
      { opacity: 0, scale: 1.18, filter: 'blur(16px)' },
      { opacity: 1, scale: 1, filter: 'blur(0px)', duration: 1.4, stagger: 0.16, ease: 'power3.out' }
    )
    tl.fromTo(
      '.hero-divider',
      { opacity: 0, scaleX: 0 },
      { opacity: 1, scaleX: 1, duration: 1.1, ease: 'power2.inOut' },
      '-=0.8'
    )
    tl.fromTo(
      '.hero-diamond',
      { opacity: 0, rotate: 120, scale: 0.4 },
      { opacity: 1, rotate: 45, scale: 1, duration: 0.9, ease: 'back.out(1.6)' },
      '-=1'
    )
    tl.fromTo(
      '.poem-line',
      { opacity: 0, y: 18, filter: 'blur(8px)' },
      { opacity: 1, y: 0, filter: 'blur(0px)', duration: 1.1, stagger: 0.3, ease: 'power3.out' },
      '-=0.5'
    )
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('wheel', onWheel)
  window.dispatchEvent(new CustomEvent('starfield:depth', { detail: 0 }))
})
</script>

<template>
  <main class="home">
    <!-- 右上：时间线 / 管理入口 -->
    <nav class="home-nav">
      <button class="btn-gold nav-timeline" @click="router.push('/timeline')">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M3 12h18M3 12l4-4M3 12l4 4M21 12l-4-4M21 12l-4 4" />
        </svg>
        时间线
      </button>
      <router-link to="/admin" class="btn-gold nav-admin">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4" />
        </svg>
        管理
      </router-link>
    </nav>

    <!-- 中上方：标题 + 金线分隔 + 诗句文案 -->
    <section class="home-hero">
      <h1 ref="titleRef" class="home-title" aria-label="星河回忆">
        <span v-for="(ch, i) in TITLE" :key="i" class="t-char">{{ ch }}</span>
      </h1>

      <div class="hero-divider-line" aria-hidden="true">
        <i class="hero-divider divider-l"></i>
        <i class="hero-diamond"></i>
        <i class="hero-divider divider-r"></i>
      </div>

      <p class="home-poem" aria-label="把每一次相遇，折叠成星，当夜空亮起，记忆便有了形状">
        <span v-for="(line, i) in POEM" :key="i" class="poem-line">{{ line }}</span>
      </p>
    </section>

    <!-- 底部照片轮播条 -->
    <div class="home-strip">
      <PhotoStrip v-if="photosStore.loaded" />
    </div>

  </main>
</template>

<style scoped>
.home {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.home-nav {
  position: absolute;
  top: 26px;
  right: 30px;
  display: flex;
  gap: 12px;
}
.home-nav .btn-gold {
  padding: 9px 22px;
  font-size: 16px;
  text-decoration: none;
}

/* ---------- 中上方标题区 ---------- */
.home-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translateY(-17vh);
}

.home-title {
  display: flex;
  /* 补偿末字右侧字距，保持真正居中 */
  padding-left: 0.18em;
  font-family: 'LXGW WenKai', 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: clamp(36px, 5.5vw, 64px);
  font-weight: 400;
  letter-spacing: 0.18em;
  line-height: 1.2;
  color: var(--ink);
  text-shadow:
    0 0 22px rgba(232, 195, 126, 0.25),
    0 0 56px rgba(232, 195, 126, 0.12);
  user-select: none;
}
.t-char {
  display: inline-block;
  opacity: 0;
  will-change: transform, filter, opacity;
}

/* 金线 + 菱形星分隔 */
.hero-divider-line {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 26px;
}
.hero-divider {
  display: block;
  width: 48px;
  height: 1px;
  opacity: 0;
  transform-origin: center;
}
.divider-l {
  background: linear-gradient(to left, var(--gold-dim), transparent);
}
.divider-r {
  background: linear-gradient(to right, var(--gold-dim), transparent);
}
.hero-diamond {
  width: 7px;
  height: 7px;
  opacity: 0;
  background: var(--gold);
  box-shadow: 0 0 12px rgba(232, 195, 126, 0.8);
  transform: rotate(45deg);
}

/* 诗句文案 */
.home-poem {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  text-align: center;
}
.poem-line {
  display: block;
  opacity: 0;
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: clamp(14px, 1.8vw, 20px);
  font-weight: 400;
  letter-spacing: 0.32em;
  /* 补偿字距造成的视觉偏移，保持真正居中 */
  text-indent: 0.32em;
  color: rgba(233, 237, 246, 0.72);
}

.home-strip {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
}

@media (max-width: 768px) {
  .home-nav { top: 18px; right: 16px; }
  .home-hero { transform: translateY(-14vh); }
  .hero-divider { width: 32px; }
  .poem-line { letter-spacing: 0.24em; text-indent: 0.24em; }
  .home-poem { gap: 9px; }
}
</style>
