import { expect, test } from '@playwright/test';
import { aliceApi } from './helpers';

test.describe.serial('settings', () => {
  test.afterAll(async () => {
    // back to the defaults, the other tests expect them
    const api = await aliceApi();
    await api.put('/api/user/me/settings', {
      data: { vatCountry: 'DE', currency: 'EUR' },
    });
    await api.dispose();
  });

  test('changes the currency of all amounts', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByRole('link', { name: 'Settings' }).click();
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible();

    await page.getByRole('combobox', { name: 'Currency' }).click();
    await page.getByRole('option', { name: /USD/ }).click();
    await expect(
      page.getByText('Amounts are shown like $1,234.50'),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Settings saved')).toBeVisible();

    await page.goto('/dashboard');
    await expect(page.getByText('$0.00').first()).toBeVisible();
  });

  test('keeps the settings after a reload', async ({ page }) => {
    await page.goto('/settings');
    await expect(
      page.getByRole('combobox', { name: 'Currency' }),
    ).toContainText('USD');
  });

  test('switches the language with the account', async ({ page }) => {
    await page.goto('/settings');
    await page.getByRole('combobox', { name: 'Language' }).click();
    await page.getByRole('option', { name: 'Deutsch' }).click();
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(
      page.getByRole('heading', { name: 'Einstellungen' }),
    ).toBeVisible();

    // the language is part of the account, not only of this browser
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(
      page.getByRole('heading', { name: 'Einstellungen' }),
    ).toBeVisible();

    await page.getByRole('combobox', { name: 'Sprache' }).click();
    await page.getByRole('option', { name: 'English' }).click();
    await page.getByRole('button', { name: 'Speichern' }).click();
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible();
  });
});
