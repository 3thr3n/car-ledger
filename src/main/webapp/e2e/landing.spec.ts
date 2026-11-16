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

test('links the legal pages of the operator before the login', async ({
  page,
}) => {
  // set up in the dev mode of the server
  await page.goto('/');

  const legal = page
    .locator('#LandingPage')
    .getByRole('navigation', { name: 'Legal' });
  await expect(legal.getByRole('link', { name: 'Imprint' })).toHaveAttribute(
    'href',
    'https://example.com/impressum',
  );
  await expect(
    legal.getByRole('link', { name: 'Privacy policy' }),
  ).toHaveAttribute('href', 'https://example.com/datenschutz');
});
