// 生成首页艺术字体（子集化 woff2，本地自托管）+ src/styles/art-fonts.css
//   标题「星河回忆」：霞鹜文楷（lxgw-wenkai-webfont，OFL 协议），按字裁剪 + unicode-range
//   诗句文案：ZCOOL XiaoWei（@fontsource/zcool-xiaowei），整段子集
// 依赖：pip install fonttools brotli；npm i @fontsource/zcool-xiaowei lxgw-wenkai-webfont
// 用法：改动下方 TITLE / POEM_TEXT 后执行 node tools/gen-art-fonts.mjs
import { execFileSync } from 'node:child_process'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')
const fontDir = resolve(root, 'src/assets/fonts')
mkdirSync(fontDir, { recursive: true })

const TITLE = '星河回忆'
const POEM_TEXT = '把每一次相遇，折叠成星当夜空亮起记忆便有了形状'

const XIAOWEI_SRC = resolve(
  root,
  'node_modules/@fontsource/zcool-xiaowei/files/zcool-xiaowei-chinese-simplified-400-normal.woff2'
)
const LXGW_CSS = resolve(root, 'node_modules/lxgw-wenkai-webfont/lxgwwenkai-regular.css')
const LXGW_FILES = resolve(root, 'node_modules/lxgw-wenkai-webfont/files')

function pyftsubset(src, text, out) {
  execFileSync(
    'python',
    ['-m', 'fontTools.subset', src, `--text=${text}`, '--flavor=woff2', '--layout-features=', `--output-file=${out}`],
    { stdio: 'inherit' }
  )
}

/* ---------- 1) 诗句：ZCOOL XiaoWei 单文件子集 ---------- */
const xiaoweiOut = join(fontDir, 'zcool-xiaowei-subset.woff2')
pyftsubset(XIAOWEI_SRC, POEM_TEXT, xiaoweiOut)
console.log('ZCOOL XiaoWei 子集 →', xiaoweiOut)

/* ---------- 2) 标题：霞鹜文楷按字裁剪（定位所在 unicode-range 分包） ---------- */
const css = readFileSync(LXGW_CSS, 'utf8')
const rangeMap = new Map() // 文件 → [{lo, hi}]
for (const b of css.split('@font-face').slice(1)) {
  const url = b.match(/url\('\.\/files\/([^']+)'\)/)?.[1]
  const range = b.match(/unicode-range:\s*([^;]+)/)?.[1]
  if (!url || !range) continue
  const spans = range.split(',').map((p) => {
    const [lo, hi] = p.trim().split(/-(?:U\+)?/).map((s) => parseInt(s.replace('U+', ''), 16))
    return [lo, hi ?? lo]
  })
  rangeMap.set(url, spans)
}

function findSourceFile(cp) {
  for (const [url, spans] of rangeMap) {
    if (spans.some(([lo, hi]) => cp >= lo && cp <= hi)) return join(LXGW_FILES, url)
  }
  throw new Error(`lxgw-wenkai-webfont 中找不到字符 U+${cp.toString(16)}`)
}

const faces = []
for (const ch of new Set(TITLE)) {
  const cp = ch.codePointAt(0)
  const hex = cp.toString(16).toUpperCase().padStart(4, '0')
  const out = join(fontDir, `lxgw-title-${hex}.woff2`)
  pyftsubset(findSourceFile(cp), ch, out)
  faces.push({ ch, hex, file: `lxgw-title-${hex}.woff2` })
  console.log(`霞鹜文楷「${ch}」→ ${out}`)
}

/* ---------- 3) 生成 art-fonts.css ---------- */
const faceCss = faces
  .map(
    (f) => `@font-face {
  font-family: 'LXGW WenKai';
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url('../assets/fonts/${f.file}') format('woff2');
  unicode-range: U+${f.hex};
}`
  )
  .join('\n')

writeFileSync(
  resolve(root, 'src/styles/art-fonts.css'),
  `/* 首页艺术字体，由 tools/gen-art-fonts.mjs 生成，勿手改。
   改动标题或诗句文案后需重跑：node tools/gen-art-fonts.mjs */
${faceCss}
@font-face {
  font-family: 'ZCOOL XiaoWei';
  font-style: normal;
  font-weight: 400;
  font-display: swap;
  src: url('../assets/fonts/zcool-xiaowei-subset.woff2') format('woff2');
}
`
)
console.log('art-fonts.css 已生成')
