import { fileURLToPath, URL } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      proxy: {
        // Forward API calls to admin-console/backend (Spring Boot, port 8082) during local dev
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8082',
          changeOrigin: true,
        },
      },
    },
  };
});
