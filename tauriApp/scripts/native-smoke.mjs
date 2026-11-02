// Windows WebView2 smoke check. Builds must finish before this script runs.
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
if (process.platform !== 'win32') throw new Error('This smoke check requires Windows/WebView2');
const exe = fileURLToPath(new URL('../src-tauri/target/release/craftdice.exe', import.meta.url));
const port = 19243;
const child = spawn(exe, [], {
  windowsHide: true,
  env: { ...process.env, WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS: `--remote-debugging-port=${port}` },
  stdio: 'ignore',
});
let browser;
try {
  const deadline = Date.now() + 30000;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) throw new Error(`CraftDice exited with ${child.exitCode}`);
    try { browser = await chromium.connectOverCDP(`http://127.0.0.1:${port}`, { timeout: 1000 }); break; }
    catch { await new Promise(resolve => setTimeout(resolve, 300)); }
  }
  if (!browser) throw new Error('WebView2 debug endpoint did not become ready');
  const context = browser.contexts()[0];
  const page = context.pages()[0] ?? await context.waitForEvent('page');
  await page.getByRole('heading', { name: '探索の準備' }).waitFor({ timeout: 15000 });
  // Read through the real IPC boundary without modifying the user's save.
  const saved = await page.evaluate(() => window.__TAURI_INTERNALS__.invoke('load_progress'));
  if (typeof saved !== 'string') throw new Error('Native save reader returned an invalid value');
  await page.getByRole('button', { name: 'ダイス編集', exact: true }).click();
  await page.getByRole('heading', { name: 'ダイス編集', exact: true }).waitFor();
  await page.getByRole('button', { name: '工房', exact: true }).click();
  await page.getByRole('heading', { name: '永続強化', exact: true }).waitFor();
  await page.getByRole('button', { name: '探索', exact: true }).click();
  await mkdir(new URL('../test-results/', import.meta.url), { recursive: true });
  await page.screenshot({ path: fileURLToPath(new URL('../test-results/native-home.png', import.meta.url)) });
  console.log('Windows app launch, bundled UI and native load_progress IPC passed');
} finally {
  if (browser) await browser.close();
  child.kill();
}
