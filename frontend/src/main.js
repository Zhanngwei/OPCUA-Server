import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 全局样式
import './styles/global.css'

// 国际化
import i18n from './i18n'

const app = createApp(App)

// 全局错误处理 - 忽略ResizeObserver错误
window.addEventListener('error', (e) => {
  if (e.message && e.message.includes('ResizeObserver loop completed with undelivered notifications')) {
    e.preventDefault()
    return false
  }
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(i18n)

app.mount('#app')
