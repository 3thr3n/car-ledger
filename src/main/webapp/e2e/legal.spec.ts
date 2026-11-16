import { expect, test } from '@playwright/test';
import { resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test('links the legal pages in the sidebar', async ({ page }) => {
  // set up in the dev mode of the server
  await page.goto('/dashboard');

  const legal = page.getByRole('navigation', { name: 'Legal' });
  await expect(legal.getByRole('link', { name: 'Imprint' })).toHaveAttribute(
    'href',
    'https://example.com/impressum',
  );
  await expect(
    legal.getByRole('link', { name: 'Privacy policy' }),
  ).toHaveAttribute('target', '_blank');
});
