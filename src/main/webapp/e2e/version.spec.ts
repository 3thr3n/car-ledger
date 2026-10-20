import { expect, test } from '@playwright/test';

test.describe('version of the app', () => {
  test('is up to date with the server', async ({ page }) => {
    // the dev server and Quarkus dev are both "dev"
    await page.goto('/settings');
    await expect(page.getByTestId('app-version')).toHaveText('CarLedger dev');
    await expect(page.getByTestId('version-details')).toHaveText(
      'Version of the app dev, of the server dev',
    );
    await expect(page.getByText('The app is up to date.')).toBeVisible();
    // section 13 of the AGPL
    await expect(
      page.getByRole('link', { name: 'source code' }),
    ).toHaveAttribute('href', 'https://github.com/3thr3n/car-ledger');
    await expect(
      page.getByRole('button', { name: 'Update available' }),
    ).toHaveCount(0);
  });

  test('offers the newer version of the server', async ({ page }) => {
    await page.route('**/api/version', (route) =>
      route.fulfill({ json: { version: '0.5.0-dev.999' } }),
    );
    await page.goto('/settings');
    await expect(
      page.getByRole('button', { name: 'Update available' }),
    ).toBeVisible();
    await expect(page.getByTestId('version-details')).toHaveText(
      'Version of the app dev, of the server 0.5.0-dev.999',
    );
    await expect(
      page.getByText(
        'This device still runs an older version of the app than the server.',
      ),
    ).toBeVisible();
  });
});
