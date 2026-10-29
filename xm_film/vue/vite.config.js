import { fileURLToPath, URL } from 'node:url'
import { existsSync, readdirSync } from 'node:fs'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import ElementPlus from 'unplugin-element-plus/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 组件样式与 `element-plus/es` 都是 resolver 在 transform 阶段才注入的，Vite 启动扫描看不到：
// 首次点进用到新组件的页面时才发现新依赖 → 重新预打包 → 整页 reload，
// 正在进行的路由跳转被丢掉（表现为「点了几下没反应，然后页面刷一下还是原页」）。
// 这里在启动时把它们一次性登记，运行期不再出现「新依赖」。
//
// 只登记扫描不到的这一类；axios / echarts 等源码里的静态 import 由 Vite 自己扫到，
// 换库不用改这里。清单从 node_modules 现读，Element Plus 被移除时自动退化为空（不报错）。
const elementPlusComponentsDir = fileURLToPath(
  new URL('./node_modules/element-plus/es/components', import.meta.url)
)
const elementPlusStyleDeps = existsSync(elementPlusComponentsDir)
  ? [
      'element-plus/es',
      ...readdirSync(elementPlusComponentsDir)
        .map((name) => `element-plus/es/components/${name}/style/index`)
        .filter((id) =>
          existsSync(fileURLToPath(new URL(`./node_modules/${id}.mjs`, import.meta.url)))
        ),
    ]
  : []

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
  // 见文件顶部 elementPlusStyleDeps：只登记扫描不到的那类依赖
  optimizeDeps: {
    include: elementPlusStyleDeps,
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/zrender/')) {
            return 'zrender'
          }
        },
      },
    },
  },
  // 环境变量配置（确保 TypeScript 正确识别 import.meta.env）
  envPrefix: ['VITE_'], // 只加载 VITE_ 前缀的环境变量

  // 文件系统配置（允许访问 src 和 node_modules 目录）
  server: {
    port: 5173, // 前端页面访问端口；后端 CORS 白名单绑定此端口（application.yml 的 cors.allowed-origins），改动需同步
    proxy: {
      '/files': {
        target: 'http://localhost:9090',
        changeOrigin: true
      }
    },
    fs: {
      allow: ['.']
    }
  }
})
