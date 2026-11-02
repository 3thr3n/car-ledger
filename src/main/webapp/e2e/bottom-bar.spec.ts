import { expect, test } from '@playwright/test';
import { aliceApi, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test.describe('bottom bar on a phone', () => {
  test.use({ viewport: { width: 390, height: 760 } });

  test('has the main places, without a car no refuel', async ({ page }) => {
    await page.goto('/dashboard');
    const bar = page.getByRole('navigation', { name: 'Shortcuts' });
    await expect(bar).toBeVisible();
    for (const name of ['Dashboard', 'Garage', 'Inbox']) {
      await expect(bar.getByRole('link', { name })).toBeVisible();
    }
    await expect(bar.getByRole('button', { name: 'Menu' })).toBeVisible();
    // no car yet: nothing to refuel
    await expect(bar.getByRole('link', { name: 'Refuel' })).toHaveCount(0);
    await expect(bar.getByRole('link', { name: 'Dashboard' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  test('opens the form of a new refuel and the menu', async ({ page }) => {
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Fabia', year: 2021, odometer: 12000 },
    });
    const [car] = (await (await api.get('/api/car/my')).json()) as {
      id: number;
    }[];
    await api.dispose();

    await page.goto('/car');
    const bar = page.getByRole('navigation', { name: 'Shortcuts' });
    await bar.getByRole('link', { name: 'Refuel' }).click();
    await expect(page).toHaveURL(
      new RegExp(`/car/${car.id}/bill/add\\?type=FUEL$`),
    );

    await bar.getByRole('button', { name: 'Menu' }).click();
    const menu = page.getByRole('navigation', { name: 'Main menu' });
    await expect(menu.getByRole('link', { name: /E2E Fabia/ })).toBeVisible();
    await menu.getByRole('link', { name: 'Settings' }).click();
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible();
  });
});

test('shows no bottom bar on a large screen', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(
    page.getByRole('heading', { name: 'Dashboard', level: 1 }),
  ).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Shortcuts' })).toHaveCount(
    0,
  );
});
