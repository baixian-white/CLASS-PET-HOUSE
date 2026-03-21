import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendUrl = env.VITE_BACKEND_URL || 'http://localhost:3001'

  return {
    plugins: [vue(), tailwindcss()],
    server: {
      host: true,
      proxy: {
        '/api': backendUrl,
        '/pet-images': backendUrl,
        '/动物图片': backendUrl
      }
    }
  }
})
