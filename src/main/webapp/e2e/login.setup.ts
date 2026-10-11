import { expect, test as setup } from '@playwright/test';
import { ALICE } from '../playwright.config';
import { keycloakLogin } from './helpers';

setup('log in as alice', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Log in to start' }).click();
  await keycloakLogin(page);

  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  await page.context().storageState({ path: ALICE });
});
