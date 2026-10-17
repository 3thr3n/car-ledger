import { devices, expect, test } from '@playwright/test';

// without defaultBrowserType, which can only be set for the whole file
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const { defaultBrowserType, ...pixel } = devices['Pixel 7'];

const firefoxAndroid =
  'Mozilla/5.0 (Android 16; Mobile; rv:143.0) Gecko/143.0 Firefox/143.0';

test.describe('install hint on a phone', () => {
  test.use(pixel);

  test('shows the steps once and stays away after "Not now"', async ({
    page,
  }) => {
    await page.goto('/dashboard');
    const hint = page.getByTestId('install-hint');
    await expect(hint).toContainText('Use CarLedger like an app');

    // the dev server has no service worker, so no install dialog: the steps
    await hint.getByRole('button', { name: 'Show me how' }).click();
    await expect(hint.getByTestId('install-steps')).toContainText(
      '“Install app” or “Add to Home screen”',
    );

    await hint.getByRole('button', { name: 'Not now' }).click();
    await expect(hint).toHaveCount(0);
    await page.reload();
    await expect(
      page.getByRole('heading', { name: 'Dashboard' }),
    ).toBeVisible();
    await expect(page.getByTestId('install-hint')).toHaveCount(0);
  });
});

test.describe('install hint in Firefox on Android', () => {
  test.use({ ...pixel, userAgent: firefoxAndroid });

  test('shows the steps of Firefox', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByRole('button', { name: 'Show me how' }).click();
    await expect(page.getByTestId('install-steps')).toContainText(
      'Open the menu ⋮ of Firefox',
    );
  });
});

test('no install hint on the computer, the steps in the settings', async ({
  page,
}) => {
  await page.goto('/dashboard');
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await expect(page.getByTestId('install-hint')).toHaveCount(0);

  await page.goto('/settings');
  await expect(page.getByTestId('install-steps')).toContainText(
    'In Chrome or Edge, click the install icon',
  );
});
