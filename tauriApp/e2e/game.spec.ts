import { test, expect } from '@playwright/test';

test('edit dice, enter the dungeon and play a physics round', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.goto('/');
  await expect(page.getByRole('heading', { name: '探索の準備' })).toBeVisible();
  await page.getByRole('button', { name: 'ダイス編集', exact: true }).click();
  await page.getByRole('button', { name: '削除', exact: true }).last().click();
  await expect(page.getByRole('button', { name: '置換', exact: true })).toHaveCount(5);
  await page.getByRole('button', { name: '探索', exact: true }).click();
  await page.getByLabel('シード', { exact: true }).fill('42');
  await page.getByRole('button', { name: '探索開始', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'ダンジョン', exact: true })).toBeVisible();
  for (let i = 0; i < 20; i++) {
    if (await page.getByRole('button', { name: '戦闘開始', exact: true }).count()) break;
    const action = page.locator('button[data-action="reward"],button[data-action="rest"],button[data-action="event"]');
    if (await action.count()) await action.first().click();
    else await page.locator('button[data-action="move"]:enabled').first().click();
  }
  await page.getByRole('button', { name: '戦闘開始', exact: true }).click();
  await page.getByRole('button', { name: '投げる', exact: true }).click();
  await expect(page.getByRole('button', { name: '転がり中…', exact: true })).toBeDisabled();
  await expect(page.locator('.log li').first()).toBeVisible({ timeout: 15000 });
  await page.screenshot({ path: 'test-results/battle.png', fullPage: true });
  expect(errors).toEqual([]);
});

test('unreadable saves never get replaced by a fresh game', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('craftdice.tauri.preview.v1', 'broken'));
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'セーブを読み込めませんでした' })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('craftdice.tauri.preview.v1'))).toBe('broken');
});
