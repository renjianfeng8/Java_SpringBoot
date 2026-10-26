import pluginVue from 'eslint-plugin-vue'
import tsParser from '@typescript-eslint/parser'

export default [
  { ignores: ['dist/**', 'node_modules/**', '.playwright-mcp/**'] },
  ...pluginVue.configs['flat/essential'],
  {
    // 页面组件按业务命名（Login.vue、Home.vue），不做多词限制
    rules: { 'vue/multi-word-component-names': 'off' },
  },
  {
    // 部分 .vue 用 <script setup lang="ts">，交给 TS 解析器
    files: ['**/*.vue'],
    languageOptions: { parserOptions: { parser: tsParser } },
  },
]
