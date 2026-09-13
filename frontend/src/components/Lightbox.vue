<script setup>
// ============================================================
// Lightbox：大图（卡片正面）⇄ 翻转背面（20字留言 + 语音播放）
// 键盘 ←/→ 切换、ESC 关闭；移动端左右滑动切换
// ============================================================
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useUiStore } from '../stores/ui'
import { storeToRefs } from 'pinia'

const ui = useUiStore()
const { lightbox, currentPhoto } = storeToRefs(ui)

const flipped = ref(false)
const audioEl = ref(null)
const playing = ref(false)
const duration = ref(0)
const touchX = ref(null)

const dateText = computed(() => {
  if (!currentPhoto.value) return ''
  const [y, m, d] = currentPhoto.value.takenAt.split('-')
  return `${y} 年 ${+m} 月 ${+d} 日`
})

/* ---------- 音频控制 ---------- */
function stopAudio() {
  if (audioEl.value) {
    audioEl.value.pause()
    audioEl.value.currentTime = 0
  }
  playing.value = false
}

function togglePlay() {
  if (!audioEl.value) return
  if (playing.value) {
    audioEl.value.pause()
  } else {
    audioEl.value.play()
  }
}

function onLoaded() {
  duration.value = audioEl.value?.duration || 0
}
function onEnded() {
  playing.value = false
}

/* ---------- 切换 / 关闭 ---------- */
function step(dir) {
  flipped.value = false
  stopAudio()
  ui.step(dir)
}
function close() {
  stopAudio()
  ui.closeLightbox()
}

watch(currentPhoto, () => {
  flipped.value = false
  stopAudio()
})

/* ---------- 键盘 & 触摸 ---------- */
function onKey(e) {
  if (!lightbox.value.open) return
  if (e.key === 'Escape') close()
  if (e.key === 'ArrowLeft') step(-1)
  if (e.key === 'ArrowRight') step(1)
}
function onTouchStart(e) {
  touchX.value = e.touches[0].clientX
}
function onTouchEnd(e) {
  if (touchX.value == null) return
  const dx = e.changedTouches[0].clientX - touchX.value
  if (Math.abs(dx) > 56) step(dx < 0 ? 1 : -1)
  touchX.value = null
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  stopAudio()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="lb">
      <div v-if="lightbox.open && currentPhoto" class="lightbox" @click.self="close">
        <!-- 顶部信息 -->
        <header class="lb-top">
          <span class="lb-date serif">{{ dateText }}</span>
          <button class="btn-icon lb-close" @click="close" aria-label="关闭">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </header>

        <!-- 翻转卡片 -->
        <div
          class="flip"
          :class="{ flipped }"
          @click="flipped = !flipped"
          @touchstart="onTouchStart"
          @touchend="onTouchEnd"
        >
          <div class="flip-inner">
            <!-- 正面：大图 -->
            <figure class="face front">
              <img :src="currentPhoto.url" :alt="currentPhoto.note" />
            </figure>

            <!-- 背面：留言 + 语音 -->
            <div class="face back">
              <!-- 管理端：右上角编辑按钮 -->
              <button
                v-if="lightbox.editable"
                class="back-edit"
                aria-label="编辑这张回忆"
                @click.stop="ui.editRequest = currentPhoto"
              >
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path d="M17 3l4 4L8 20H4v-4z" />
                </svg>
              </button>
              <div class="back-inner">
                <span class="back-label">回忆背面</span>
                <p class="back-note serif">{{ currentPhoto.note || '（还没有留言）' }}</p>

                <!-- 语音播放器 -->
                <div v-if="currentPhoto.audioUrl" class="voice" @click.stop>
                  <button class="voice-btn" @click="togglePlay">
                    <svg v-if="!playing" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M8 5v14l11-7z" />
                    </svg>
                    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M7 5h4v14H7zM13 5h4v14h-4z" />
                    </svg>
                  </button>

                  <div class="voice-bars" :class="{ playing }">
                    <i v-for="n in 12" :key="n" :style="{ animationDelay: `${(n % 7) * 0.09}s` }"></i>
                  </div>

                  <span class="voice-sec">{{ Math.round(duration || currentPhoto.audioDuration || 0) }}″</span>

                  <audio
                    ref="audioEl"
                    :src="currentPhoto.audioUrl"
                    @loadedmetadata="onLoaded"
                    @play="playing = true"
                    @pause="playing = false"
                    @ended="onEnded"
                  ></audio>
                </div>
                <p v-else class="voice-empty">这张回忆还没有语音</p>
              </div>
            </div>
          </div>
        </div>

        <p class="lb-hint">轻触卡片 · 翻看背面</p>

        <!-- 切换箭头 -->
        <button class="lb-arrow left" @click="step(-1)" aria-label="上一张">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M15 5l-7 7 7 7" />
          </svg>
        </button>
        <button class="lb-arrow right" @click="step(1)" aria-label="下一张">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(3, 5, 12, 0.82);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.lb-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
}
.lb-date {
  color: var(--gold-dim);
  font-size: 15px;
  letter-spacing: 0.2em;
}

/* ---------- 翻转 ---------- */
.flip {
  perspective: 1600px;
  cursor: pointer;
  max-width: min(82vw, 520px);
  max-height: 68vh;
}
.flip-inner {
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.9s var(--ease-out);
}
.flip.flipped .flip-inner {
  transform: rotateY(180deg);
}
.face {
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  border-radius: var(--r-md);
  overflow: hidden;
  border: 1px solid rgba(232, 195, 126, 0.22);
  box-shadow: var(--shadow-card), 0 0 60px rgba(232, 195, 126, 0.08);
}
.face.front {
  background: #0a0e1c;
}
.face.front img {
  max-width: min(82vw, 520px);
  max-height: 68vh;
  width: auto;
  height: auto;
  object-fit: contain;
}
.face.back {
  position: absolute;
  inset: 0;
  transform: rotateY(180deg);
  background:
    radial-gradient(ellipse at 30% 20%, rgba(232, 195, 126, 0.1), transparent 55%),
    linear-gradient(160deg, #101527, #070a16);
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-inner {
  padding: 40px 36px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 26px;
  width: 100%;
}
.back-edit {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--gold-bright);
  background: rgba(232, 195, 126, 0.1);
  border: 1px solid var(--gold-faint);
  transition: transform 0.25s var(--ease-spring), background 0.25s;
  z-index: 2;
}
.back-edit:active {
  transform: scale(0.9);
}
.back-label {
  font-size: 12px;
  letter-spacing: 0.5em;
  color: var(--gold-dim);
  border: 1px solid var(--gold-faint);
  padding: 4px 14px 4px 18px;
  border-radius: 999px;
}
.back-note {
  font-size: clamp(18px, 3.4vw, 24px);
  font-weight: 400;
  line-height: 1.9;
  color: var(--ink);
  text-shadow: 0 0 30px rgba(232, 195, 126, 0.25);
}

/* ---------- 语音播放器 ---------- */
.voice {
  display: flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  padding: 7px 14px 7px 7px;
  border-radius: 999px;
  background: rgba(232, 195, 126, 0.07);
  border: 1px solid var(--gold-faint);
  cursor: default;
}
.voice-btn {
  flex: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--gold), #c9a05c);
  color: #1a1408;
  transition: transform 0.3s var(--ease-spring);
}
.voice-btn:active {
  transform: scale(0.92);
}

.voice-bars {
  display: flex;
  align-items: center;
  gap: 2.5px;
  height: 22px;
  flex: none;
}
.voice-bars i {
  width: 2.5px;
  height: 30%;
  border-radius: 2px;
  background: var(--gold);
  opacity: 0.7;
  animation: wave 1s ease-in-out infinite;
  animation-play-state: paused;
}
.voice-bars.playing i {
  animation-play-state: running;
}
@keyframes wave {
  0%, 100% { height: 25%; }
  50% { height: 95%; }
}

.voice-sec {
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.08em;
  color: var(--gold-dim);
  font-variant-numeric: tabular-nums;
}
.voice-empty {
  font-size: 13px;
  color: var(--ink-faint);
  letter-spacing: 0.2em;
}

.lb-hint {
  position: absolute;
  bottom: 26px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 13px;
  letter-spacing: 0.35em;
  color: var(--ink-faint);
}

/* ---------- 箭头 ---------- */
.lb-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-dim);
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(8px);
  transition: all 0.3s var(--ease-out);
}
.lb-arrow:hover {
  color: var(--gold-bright);
  border-color: var(--gold-dim);
  box-shadow: var(--glow-gold);
}
.lb-arrow.left { left: max(3vw, 16px); }
.lb-arrow.right { right: max(3vw, 16px); }

/* ---------- 过渡 ---------- */
.lb-enter-active { transition: opacity 0.4s var(--ease-out); }
.lb-leave-active { transition: opacity 0.3s ease; }
.lb-enter-from, .lb-leave-to { opacity: 0; }
.lb-enter-active .flip { animation: lbIn 0.55s var(--ease-spring); }
@keyframes lbIn {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@media (max-width: 768px) {
  .lb-arrow { width: 42px; height: 42px; }
  .lb-hint { display: none; }
  .back-inner { padding: 28px 20px; }
}
</style>
