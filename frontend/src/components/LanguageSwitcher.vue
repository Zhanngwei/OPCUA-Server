<template>
  <el-dropdown @command="handleLanguageChange" trigger="click" class="language-switcher">
    <div class="language-button">
      <el-icon><Setting /></el-icon>
      <span class="current-language">{{ getCurrentLanguageName() }}</span>
      <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item 
          v-for="locale in availableLocales" 
          :key="locale.code"
          :command="locale.code"
          :class="{ 'is-active': currentLocale === locale.code }"
        >
          <div class="language-option">
            <span class="language-name">{{ locale.nativeName }}</span>
            <span class="language-code">{{ locale.name }}</span>
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script>
import { ref, onMounted } from 'vue'
import { Setting, ArrowDown } from '@element-plus/icons-vue'
import { setLocale, getCurrentLocale, getAvailableLocales } from '../i18n'

export default {
  name: 'LanguageSwitcher',
  components: {
    Setting,
    ArrowDown
  },
  setup() {
    const currentLocale = ref(getCurrentLocale())
    const availableLocales = ref(getAvailableLocales())

    const getCurrentLanguageName = () => {
      const current = availableLocales.value.find(l => l.code === currentLocale.value)
      return current ? current.nativeName : 'English'
    }

    const handleLanguageChange = (langCode) => {
      if (langCode !== currentLocale.value) {
        setLocale(langCode)
        currentLocale.value = langCode
        
        // 刷新页面以确保所有组件都使用新语言
        // 在实际应用中，你可能不需要刷新页面
        window.location.reload()
      }
    }

    onMounted(() => {
      // 确保当前语言状态正确
      currentLocale.value = getCurrentLocale()
    })

    return {
      currentLocale,
      availableLocales,
      getCurrentLanguageName,
      handleLanguageChange
    }
  }
}
</script>

<style scoped>
.language-switcher {
  margin-left: auto;
}

.language-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.3s;
  color: #fff;
}

.language-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.current-language {
  font-size: 14px;
  font-weight: 500;
}

.dropdown-arrow {
  font-size: 12px;
  transition: transform 0.3s;
}

.language-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.language-name {
  font-weight: 500;
  font-size: 14px;
}

.language-code {
  font-size: 12px;
  color: #909399;
}

.is-active {
  background-color: #f5f7fa;
}

.is-active .language-name {
  color: #409eff;
  font-weight: 600;
}
</style>
