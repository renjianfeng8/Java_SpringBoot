import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import ElementPlus from 'unplugin-element-plus/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    ElementPlus({
      useSource: true,
    }),
    AutoImport({
      imports: [
        'vue',
        'vue-router'
      ],
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @use "@/assets/css/index.scss" as *;
        `
      }
    }
  },
  // 环境变量配置（确保 TypeScript 正确识别 import.meta.env）
  envPrefix: ['VITE_'], // 只加载 VITE_ 前缀的环境变量

  // 文件系统配置（允许访问 src 和 node_modules 目录）
  server: {
    fs: {
      allow: [
        'node_modules',
        'src'
      ]
    }
  }
})