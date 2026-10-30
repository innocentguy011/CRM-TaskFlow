import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// A production build without a valid backend URL would silently call
// localhost from the deployed site, so fail the build instead.
function assertApiUrl(apiUrl) {
  const problem = !apiUrl
    ? 'is not set'
    : !/^https?:\/\/[^/]+/.test(apiUrl)
      ? `must be an absolute http(s) URL (got "${apiUrl}")`
      : /\/(api)?$/.test(apiUrl)
        ? `must not end with "/" or "/api" (got "${apiUrl}")`
        : null
  if (problem) {
    throw new Error(
      `VITE_API_URL ${problem}. Set it to the backend base URL, e.g. ` +
      'https://your-backend.onrender.com (on Vercel: Project Settings → Environment Variables).'
    )
  }
}

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  if (command === 'build' && mode === 'production') {
    assertApiUrl(loadEnv(mode, process.cwd(), 'VITE_').VITE_API_URL)
  }

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        global: 'globalThis',
      },
    },
    define: {
      global: 'globalThis',
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
        },
        '/ws': {
          target: 'ws://localhost:8081',
          ws: true,
          secure: false,
        },
      },
    },
  }
})
