import { expect, test } from '@playwright/test';
import { keycloakLogin } from './helpers';

// bob in his own session, the account of alice is shared by the other tests
test.use({ storageState: { cookies: [], origins: [] } });

test('deletes the own account with its cars and logs out', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Log in to start' }).click();
  await keycloakLogin(page, 'bob');
  await expect(page).toHaveURL(/\/dashboard/);

  await page.goto('/car/new');
  await page.getByRole('textbox', { name: /^Name or model/ }).fill('Bobs car');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByRole('heading', { name: 'Bobs car' })).toBeVisible();

  await page.goto('/settings');
  await page.getByRole('button', { name: 'Delete account' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText(
    'Your login at the identity provider stays',
  );
  const confirm = dialog.getByRole('button', { name: 'Delete account' });
  await expect(confirm).toBeDisabled();
  await dialog.getByRole('textbox', { name: 'Type bob to confirm' }).fill('bo');
  await expect(confirm).toBeDisabled();
  await dialog
    .getByRole('textbox', { name: 'Type bob to confirm' })
    .fill('bob');
  await confirm.click();

  await expect(page.getByText('Your account was deleted')).toBeVisible();
  await expect(
    page.getByRole('link', { name: 'Log in to start' }),
  ).toBeVisible();

  // a new login starts with an empty account; the session at Keycloak may
  // still exist, then there is no login form
  await page.getByRole('link', { name: 'Log in to start' }).click();
  await page.waitForURL(/\/dashboard|\/realms\//);
  if (page.url().includes('/realms/')) await keycloakLogin(page, 'bob');
  await expect(page).toHaveURL(/\/dashboard/);
  await page.goto('/car');
  await expect(page.getByText('Start with your first car')).toBeVisible();
  await expect(page.getByText('Bobs car')).toHaveCount(0);
});
