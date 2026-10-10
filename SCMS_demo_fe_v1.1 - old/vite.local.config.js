import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath } from 'node:url';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // dev:local/build:local are intentionally the backend-free UI demo.
  define: {
    'import.meta.env.VITE_API_ENABLED': JSON.stringify('false'),
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    dedupe: ['react', 'react-dom'],
  },
  server: { host: 'localhost', port: 5173, strictPort: false },
});
