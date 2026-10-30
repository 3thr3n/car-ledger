import { expect, test } from '@playwright/test';
import { resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

// before the login
test.use({ storageState: { cookies: [], origins: [] } });

test('shows all features on the landing page', async ({ page }) => {
  await page.goto('/');

  await expect(
    page.getByRole('heading', { name: 'Know what your car really costs.' }),
  ).toBeVisible();
  await expect(page.getByText(/keep the papers of your car/)).toBeVisible();
  for (const feature of [
    'Refuels',
    'Charging',
    'Modifications',
    'HU',
    'Papers as PDF',
    'Reminders',
    'Trip log',
    'GPS tracking',
    'Sharing',
    'App and offline',
    'Backup',
  ]) {
    await expect(
      page.locator('.MuiCard-root').getByText(feature, { exact: true }),
    ).toBeVisible();
  }
});
