import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  use: { baseURL: 'http://127.0.0.1:1420', browserName: 'chromium', headless: true },
  webServer: { command: 'npx vite --host 127.0.0.1', url: 'http://127.0.0.1:1420', reuseExistingServer: false },
});
