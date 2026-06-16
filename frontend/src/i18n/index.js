import { createI18n } from 'vue-i18n'
import en from './locales/en.json'
import zh from './locales/zh.json'

// 获取系统语言
function getDefaultLocale() {
  const browserLang = navigator.language || navigator.userLanguage
  
  // 如果是中文相关语言，返回中文
  if (browserLang.startsWith('zh')) {
    return 'zh'
  }
  
  // 默认返回英文
  return 'en'
}

// 从localStorage获取用户设置的语言，如果没有则使用系统语言
function getStoredLocale() {
  const stored = localStorage.getItem('opcua-locale')
  if (stored && ['en', 'zh'].includes(stored)) {
    return stored
  }
  return getDefaultLocale()
}

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: getStoredLocale(), // 设置默认语言
  fallbackLocale: 'en', // 设置备用语言
  messages: {
    en,
    zh
  }
})

// 切换语言的函数
export function setLocale(locale) {
  if (['en', 'zh'].includes(locale)) {
    i18n.global.locale.value = locale
    localStorage.setItem('opcua-locale', locale)
    
    // 更新HTML的lang属性
    document.documentElement.setAttribute('lang', locale)
  }
}

// 获取当前语言
export function getCurrentLocale() {
  return i18n.global.locale.value
}

// 获取可用语言列表
export function getAvailableLocales() {
  return [
    { code: 'en', name: 'English', nativeName: 'English' },
    { code: 'zh', name: 'Chinese', nativeName: '中文' }
  ]
}

export default i18n
