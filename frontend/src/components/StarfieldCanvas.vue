<script setup>
// ============================================================
// 星空引擎：三层星场 / 星星闪烁 / 随机流星 / 星云微光
// 鼠标视差 + 滚动纵深（通过 window 事件 starfield:depth 接收 0~1）
// ============================================================
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as THREE from 'three'

const canvasRef = ref(null)

let renderer, scene, camera, raf
let starPoints, nebulaGroup
let meteors = []
let lastNow = 0
let elapsed = 0

// 交互状态
const pointer = { x: 0, y: 0, tx: 0, ty: 0 }
let depth = 0
let depthTarget = 0

const isMobile = () => window.innerWidth < 768

/* ---------- 纹理生成 ---------- */
function radialTexture(size, stops) {
  const c = document.createElement('canvas')
  c.width = c.height = size
  const ctx = c.getContext('2d')
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  stops.forEach(([o, col]) => g.addColorStop(o, col))
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  const tex = new THREE.CanvasTexture(c)
  return tex
}

function meteorTexture() {
  const c = document.createElement('canvas')
  c.width = 256
  c.height = 32
  const ctx = c.getContext('2d')
  const g = ctx.createLinearGradient(0, 16, 256, 16)
  g.addColorStop(0, 'rgba(255,255,255,0)')
  g.addColorStop(0.7, 'rgba(245,217,164,0.55)')
  g.addColorStop(1, 'rgba(255,255,255,1)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 256, 32)
  return new THREE.CanvasTexture(c)
}

/* ---------- 星场 ---------- */
function buildStars() {
  const count = isMobile() ? 900 : 2200
  const positions = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const phases = new Float32Array(count)
  const speeds = new Float32Array(count)
  const mixes = new Float32Array(count)

  for (let i = 0; i < count; i++) {
    // 三层纵深：近 / 中 / 远
    const layer = i % 3
    const z = layer === 0 ? -80 - Math.random() * 120 : layer === 1 ? -220 - Math.random() * 240 : -480 - Math.random() * 420
    const spread = 1 + (-z / 500)
    positions[i * 3] = (Math.random() - 0.5) * 900 * spread
    positions[i * 3 + 1] = (Math.random() - 0.5) * 560 * spread
    positions[i * 3 + 2] = z
    // 粒子尺寸放大一倍
    sizes[i] = layer === 0 ? 4.4 + Math.random() * 4.8 : 2.4 + Math.random() * 3.6
    phases[i] = Math.random() * Math.PI * 2
    speeds[i] = 0.6 + Math.random() * 1.8
    mixes[i] = Math.random() < 0.22 ? 1 : 0 // 22% 暖金色星
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  geo.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  geo.setAttribute('aSpeed', new THREE.BufferAttribute(speeds, 1))
  geo.setAttribute('aMix', new THREE.BufferAttribute(mixes, 1))

  const mat = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: {
      uTime: { value: 0 },
      uPixelRatio: { value: Math.min(window.devicePixelRatio, 2) },
      uColorA: { value: new THREE.Color('#dfe8ff') },
      uColorB: { value: new THREE.Color('#e8c37e') },
    },
    vertexShader: `
      uniform float uTime;
      uniform float uPixelRatio;
      attribute float aSize;
      attribute float aPhase;
      attribute float aSpeed;
      attribute float aMix;
      varying float vMix;
      varying float vTwinkle;
      void main() {
        vec4 mv = modelViewMatrix * vec4(position, 1.0);
        float tw = 0.72 + 0.28 * sin(uTime * aSpeed + aPhase);
        vTwinkle = tw;
        vMix = aMix;
        gl_PointSize = aSize * uPixelRatio * tw * (160.0 / -mv.z);
        gl_Position = projectionMatrix * mv;
      }
    `,
    fragmentShader: `
      uniform vec3 uColorA;
      uniform vec3 uColorB;
      varying float vMix;
      varying float vTwinkle;
      void main() {
        vec2 uv = gl_PointCoord - 0.5;
        float d = length(uv);
        float alpha = smoothstep(0.5, 0.0, d);
        alpha *= alpha;
        vec3 col = mix(uColorA, uColorB, vMix);
        gl_FragColor = vec4(col, alpha * vTwinkle);
      }
    `,
  })

  starPoints = new THREE.Points(geo, mat)
  scene.add(starPoints)
}

/* ---------- 星云 ---------- */
function buildNebula() {
  nebulaGroup = new THREE.Group()
  const defs = [
    { color: 'rgba(26,42,94,0.55)', x: -260, y: 120, z: -700, s: 900 },
    { color: 'rgba(232,195,126,0.30)', x: 300, y: -80, z: -750, s: 760 },
    { color: 'rgba(64,52,110,0.40)', x: 60, y: 200, z: -820, s: 820 },
  ]
  defs.forEach((d, i) => {
    const tex = radialTexture(256, [
      [0, d.color],
      [0.4, d.color.replace(/[\d.]+\)$/, '0.12)')],
      [1, 'rgba(0,0,0,0)'],
    ])
    const mat = new THREE.SpriteMaterial({
      map: tex,
      transparent: true,
      opacity: 0.5,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    })
    const sp = new THREE.Sprite(mat)
    sp.position.set(d.x, d.y, d.z)
    sp.scale.setScalar(d.s)
    sp.userData.spin = (i % 2 === 0 ? 1 : -1) * 0.004
    nebulaGroup.add(sp)
  })
  scene.add(nebulaGroup)
}

/* ---------- 流星 ---------- */
function spawnMeteor(tex) {
  const mat = new THREE.MeshBasicMaterial({
    map: tex,
    transparent: true,
    opacity: 0,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    side: THREE.DoubleSide,
  })
  const len = 90 + Math.random() * 90
  const mesh = new THREE.Mesh(new THREE.PlaneGeometry(len, 2.2), mat)
  const dir = Math.random() < 0.5 ? -1 : 1
  const angle = (28 + Math.random() * 18) * (Math.PI / 180)
  mesh.position.set(
    (Math.random() - 0.5) * 700,
    140 + Math.random() * 160,
    -260 - Math.random() * 200
  )
  mesh.userData = {
    vx: Math.cos(angle) * (140 + Math.random() * 80) * dir,
    vy: -Math.sin(angle) * (140 + Math.random() * 80),
    life: 0,
    maxLife: 1.1 + Math.random() * 0.7,
    angle: Math.atan2(-Math.sin(angle), Math.cos(angle) * dir),
  }
  mesh.rotation.z = mesh.userData.angle
  scene.add(mesh)
  meteors.push(mesh)
}

let meteorTimer = 1.2
function updateMeteors(dt, tex) {
  meteorTimer -= dt
  if (meteorTimer <= 0 && meteors.length < 3) {
    spawnMeteor(tex)
    meteorTimer = 2.2 + Math.random() * 4.5
  }
  meteors = meteors.filter((m) => {
    const u = m.userData
    u.life += dt
    const t = u.life / u.maxLife
    if (t >= 1) {
      scene.remove(m)
      m.geometry.dispose()
      m.material.dispose()
      return false
    }
    m.position.x += u.vx * dt
    m.position.y += u.vy * dt
    m.material.opacity = Math.sin(t * Math.PI) * 0.9
    return true
  })
}

/* ---------- 事件 ---------- */
function onPointerMove(e) {
  pointer.tx = (e.clientX / window.innerWidth - 0.5) * 2
  pointer.ty = (e.clientY / window.innerHeight - 0.5) * 2
}
function onDepth(e) {
  depthTarget = Math.min(1, Math.max(0, e.detail))
}
function onResize() {
  camera.aspect = window.innerWidth / window.innerHeight
  camera.updateProjectionMatrix()
  renderer.setSize(window.innerWidth, window.innerHeight)
  starPoints.material.uniforms.uPixelRatio.value = Math.min(window.devicePixelRatio, 2)
}

onMounted(() => {
  renderer = new THREE.WebGLRenderer({
    canvas: canvasRef.value,
    antialias: true,
    alpha: true,
    powerPreference: 'high-performance',
  })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(window.innerWidth, window.innerHeight)

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(62, window.innerWidth / window.innerHeight, 1, 2000)
  camera.position.z = 0

  buildStars()
  buildNebula()
  const mTex = meteorTexture()

  const animate = () => {
    raf = requestAnimationFrame(animate)
    const now = performance.now()
    const dt = lastNow ? Math.min((now - lastNow) / 1000, 0.05) : 0.016
    lastNow = now
    elapsed += dt
    const t = elapsed

    starPoints.material.uniforms.uTime.value = t

    // 鼠标视差（缓动跟随）
    pointer.x += (pointer.tx - pointer.x) * 0.04
    pointer.y += (pointer.ty - pointer.y) * 0.04
    starPoints.rotation.y = pointer.x * 0.045
    starPoints.rotation.x = pointer.y * 0.03

    // 滚动纵深：相机穿星而行
    depth += (depthTarget - depth) * 0.05
    camera.position.z = -depth * 380
    camera.position.x += (pointer.x * 26 - camera.position.x) * 0.03
    camera.position.y += (-pointer.y * 16 - camera.position.y) * 0.03

    // 星云缓旋
    nebulaGroup.children.forEach((sp) => {
      sp.material.rotation += sp.userData.spin * dt * 10
    })

    updateMeteors(dt, mTex)
    renderer.render(scene, camera)
  }
  animate()

  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('starfield:depth', onDepth)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('starfield:depth', onDepth)
  window.removeEventListener('resize', onResize)
  renderer?.dispose()
})
</script>

<template>
  <canvas ref="canvasRef" class="starfield" aria-hidden="true"></canvas>
</template>

<style scoped>
.starfield {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: radial-gradient(ellipse at 50% 120%, #0a1128 0%, #04060f 60%);
}
</style>
