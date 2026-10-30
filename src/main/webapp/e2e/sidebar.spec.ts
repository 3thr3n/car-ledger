import { expect, test } from '@playwright/test';
import { resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test('hides the sidebar on a large screen and keeps it open again', async ({
  page,
}) => {
  await page.goto('/dashboard');
  const settings = page.getByRole('link', { name: 'Settings' });
  const menu = page.getByRole('button', { name: 'menu' });
  await expect(settings).toBeVisible();
  await expect(menu).toBeHidden();

  // e.g. a phone in landscape, the pages get the whole width
  await page.getByRole('button', { name: 'Hide the sidebar' }).click();
  await expect(settings).toBeHidden();
  await expect(menu).toBeVisible();

  // remembered on this device
  await page.reload();
  await expect(menu).toBeVisible();
  await menu.click();
  await expect(settings).toBeVisible();
  await page.getByRole('button', { name: 'Keep the sidebar open' }).click();

  await expect(menu).toBeHidden();
  await expect(settings).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole('button', { name: 'Hide the sidebar' }),
  ).toBeVisible();
});

test('has no buttons for the sidebar on a phone', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/dashboard');
  await page.getByRole('button', { name: 'menu' }).click();
  await expect(page.getByRole('link', { name: 'Settings' })).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Hide the sidebar' }),
  ).toBeHidden();
  await expect(
    page.getByRole('button', { name: 'Keep the sidebar open' }),
  ).toBeHidden();
});
