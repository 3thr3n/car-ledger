import { expect, test } from '@playwright/test';
import { API_URL } from '../playwright.config';
import { keycloakLogin } from './helpers';

test.describe.serial('administration', () => {
  test('is not available to normal users', async ({ browser }) => {
    // bob has no admin role, the login also creates his account
    const context = await browser.newContext({
      storageState: { cookies: [], origins: [] },
    });
    const page = await context.newPage();
    await page.goto('/');
    await page.getByRole('link', { name: 'Log in to start' }).click();
    await keycloakLogin(page, 'bob');
    await expect(page).toHaveURL(/\/dashboard/);

    await expect(
      page.getByRole('link', { name: 'Administration' }),
    ).toHaveCount(0);
    await page.goto('/admin');
    await expect(page.getByText('No access')).toBeVisible();

    const response = await page.request.get(`${API_URL}/api/admin/stats`, {
      headers: { 'X-Requested-With': 'JavaScript' },
    });
    expect(response.status()).toBe(403);
    await context.close();
  });

  test('shows the usage and the accounts', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByRole('link', { name: 'Administration' }).click();

    await expect(page.getByText('Active (30 days)')).toBeVisible();
    const own = page.getByRole('row', { name: /alice/ });
    await expect(own).toContainText('You');
    await expect(own.getByRole('button', { name: 'Delete' })).toBeDisabled();

    await page.getByPlaceholder('Search accounts').fill('bo');
    await expect(page.getByRole('row', { name: /bob/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /alice/ })).toHaveCount(0);
  });

  test('changes the car limit', async ({ page }) => {
    await page.goto('/admin');
    const own = page.getByRole('row', { name: /alice/ });
    await own.getByRole('button', { name: 'Change car limit' }).click();

    const dialog = page.getByRole('dialog');
    await dialog.getByRole('textbox', { name: 'Maximum cars' }).fill('2');
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Car limit changed')).toBeVisible();
    await expect(own).toContainText('/ 2');

    await page.goto('/car');
    await expect(page.getByText(/of 2 cars/)).toBeVisible();

    // back to the default
    await page.goto('/admin');
    await page
      .getByRole('row', { name: /alice/ })
      .getByRole('button', { name: 'Change car limit' })
      .click();
    await page
      .getByRole('dialog')
      .getByRole('textbox', { name: 'Maximum cars' })
      .fill('1');
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Save' })
      .click();
    await expect(page.getByRole('row', { name: /alice/ })).toContainText('/ 1');
  });

  test('deletes an account', async ({ page }) => {
    await page.goto('/admin');
    const bob = page.getByRole('row', { name: /bob/ });
    await bob.getByRole('button', { name: 'Delete' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Delete bob?');
    await dialog.getByRole('button', { name: 'Delete' }).click();

    await expect(page.getByText('Account deleted')).toBeVisible();
    await expect(page.getByRole('row', { name: /bob/ })).toHaveCount(0);
  });
});
