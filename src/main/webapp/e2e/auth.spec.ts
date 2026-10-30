import { expect, test } from '@playwright/test';
import { API_URL } from '../playwright.config';
import { keycloakLogin, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

// own sessions, not the shared one of the other tests
test.use({ storageState: { cookies: [], origins: [] } });

test('returns to the requested page after the login', async ({ page }) => {
  const deepLink = '/dashboard?from=2026-01-01&to=2026-10-01';
  await page.goto(deepLink);

  await expect(page.locator('#kc-login')).toBeVisible();
  // no error page while the browser leaves for the login
  await expect(page.getByText('Something went wrong')).toHaveCount(0);
  await keycloakLogin(page);

  await expect(page).toHaveURL(new RegExp(`${deepLink.replace('?', '\\?')}$`));
  await expect(page.getByText('Cost per km', { exact: true })).toBeVisible();
});

test('logs out and invalidates the session', async ({ page, request }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Log in to start' }).click();
  await keycloakLogin(page);
  await expect(page).toHaveURL(/\/dashboard/);
  const session = (await page.context().cookies(API_URL)).find(
    (c) => c.name === 'q_session',
  );
  expect(session).toBeDefined();

  await page.getByRole('link', { name: 'Log out' }).click();

  await expect(
    page.getByRole('link', { name: 'Log in to start' }),
  ).toBeVisible();
  const response = await request.get(`${API_URL}/api/user/me`, {
    headers: {
      'X-Requested-With': 'JavaScript',
      Cookie: `q_session=${session!.value}`,
    },
  });
  expect(response.status()).toBe(499);
});

test('rejects API calls without a session', async ({ request }) => {
  const response = await request.get(`${API_URL}/api/car/my`, {
    headers: { 'X-Requested-With': 'JavaScript' },
  });
  expect(response.status()).toBe(499);
});
