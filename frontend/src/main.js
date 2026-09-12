import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// 思源宋体本地打包（国内网络下 Google Fonts 不稳定，本地化保证清晰一致）
import '@fontsource/noto-serif-sc/300.css'
import '@fontsource/noto-serif-sc/400.css'
import '@fontsource/noto-serif-sc/600.css'
// 首页艺术字体：马善政毛笔行书（标题）+ ZCOOL XiaoWei 文艺宋（诗句）
// 使用按字符子集化的本地 woff2（共约 13KB），由 tools/gen-art-fonts.mjs 生成
import './styles/art-fonts.css'
import './styles/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
