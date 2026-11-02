import { defineConfig } from 'vite';
export default defineConfig({
  clearScreen: false,
  server: { port: 1420, strictPort: true, watch: { ignored: ['**/src-tauri/**', '**/game-core/**'] } },
  build: { target: ['es2022', 'chrome105', 'safari15'] },
});
