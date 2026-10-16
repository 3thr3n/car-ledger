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

  test('shows the notifications by e-mail', async ({ page }) => {
    // the users of the Keycloak dev service have no address
    await page.goto('/settings');
    await expect(
      page.getByText('Your login has no e-mail address.', { exact: false }),
    ).toBeVisible();
    await expect(page.getByRole('switch')).toHaveCount(0);
  });

  test('turns a notification off', async ({ page }) => {
    // as if the login had an address, the change is saved by the backend
    await page.route('**/api/user/me/notifications', (route) =>
      route.request().method() === 'GET'
        ? route.fulfill({
            json: {
              available: true,
              email: 'alice@example.com',
              newRequests: true,
              requestDecided: true,
              reminders: true,
            },
          })
        : route.continue(),
    );
    await page.goto('/settings');
    await expect(
      page.getByText('Sent to alice@example.com, the address of your login.'),
    ).toBeVisible();
    const newRequests = page.getByRole('switch', {
      name: 'New requests for more cars (as administrator)',
    });
    await expect(newRequests).toBeChecked();
    await expect(
      page.getByRole('switch', {
        name: 'Reminders of my cars that are due soon (once a day)',
      }),
    ).toBeChecked();

    await newRequests.click();
    await expect(page.getByText('Notifications saved')).toBeVisible();

    const api = await aliceApi();
    const saved = await (await api.get('/api/user/me/notifications')).json();
    expect(saved.newRequests).toBe(false);
    expect(saved.requestDecided).toBe(true);
    expect(saved.reminders).toBe(true);
    await api.put('/api/user/me/notifications', {
      data: { newRequests: true },
    });
    await api.dispose();
  });
});
