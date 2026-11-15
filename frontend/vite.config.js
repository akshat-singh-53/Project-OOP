import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// `npm run build` writes straight into the Spring Boot static folder so one JAR serves UI + API.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/target/classes/static',
    emptyOutDir: true,
    chunkSizeWarningLimit: 1200,
  },
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
