import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8091',
        changeOrigin: true,
      },
      '/api/events': {
        target: 'http://localhost:8092',
        changeOrigin: true,
      },
      '/api/categories': {
        target: 'http://localhost:8092',
        changeOrigin: true,
      },
      '/api/tickets': {
        target: 'http://localhost:8093',
        changeOrigin: true,
      },
      '/api/ticket-types': {
        target: 'http://localhost:8093',
        changeOrigin: true,
      },
      '/api/payments': {
        target: 'http://localhost:8094',
        changeOrigin: true,
      },
      '/api/notifications': {
        target: 'http://localhost:8095',
        changeOrigin: true,
      },
    },
  },
})
