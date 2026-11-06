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
      page.getByRole('heading', { name: feature, exact: true, level: 3 }),
    ).toBeVisible();
  }
  // the groups and the example of the page of a car
  await expect(
    page.getByRole('heading', { name: 'Every cost of the car', level: 2 }),
  ).toBeVisible();
  await expect(
    page.getByRole('figure', {
      name: 'An example: the page of a car after a year of entries.',
    }),
  ).toBeVisible();
});
