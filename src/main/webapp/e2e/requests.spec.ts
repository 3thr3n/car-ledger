import {
  APIRequestContext,
  BrowserContext,
  expect,
  Page,
  test,
} from '@playwright/test';
import { aliceApi, keycloakLogin } from './helpers';

/**
 * Bob reaches his car limit and requests more, alice approves as administrator
 */
test.describe.serial('car requests', () => {
  let api: APIRequestContext;
  let bobContext: BrowserContext;
  let bob: Page;

  /** bob starts without an account, the login creates it with one car */
  const deleteBob = async () => {
    const accounts = await (
      await api.get('/api/admin/accounts?search=bob')
    ).json();
    for (const account of accounts.data as { id: number; name: string }[]) {
      if (account.name === 'bob') {
        await api.delete(`/api/admin/accounts/${account.id}`);
      }
    }
  };

  test.beforeAll(async ({ browser }) => {
    api = await aliceApi();
    await deleteBob();
    bobContext = await browser.newContext({
      storageState: { cookies: [], origins: [] },
    });
    bob = await bobContext.newPage();
  });

  test.afterAll(async () => {
    await bobContext.close();
    await deleteBob();
    await api.dispose();
  });

  test('requests more cars at the limit', async () => {
    await bob.goto('/');
    await bob.getByRole('link', { name: 'Log in to start' }).click();
    await keycloakLogin(bob, 'bob');
    await expect(bob).toHaveURL(/\/dashboard/);

    await bob.goto('/car/new');
    await bob.getByRole('textbox', { name: /^Name or model/ }).fill('Bobs van');
    await bob.getByRole('button', { name: 'Save' }).click();
    await expect(bob).toHaveURL(/\/car\/\d+$/);

    await bob.goto('/car');
    await expect(
      bob.getByText('You reached the maximum number of cars.'),
    ).toBeVisible();
    await bob.getByRole('button', { name: 'Request more cars' }).click();
    const dialog = bob.getByRole('dialog');
    await dialog
      .getByRole('textbox', { name: 'Reason' })
      .fill('For the trailer');
    await dialog.getByRole('button', { name: 'Send request' }).click();

    await expect(bob.getByText('Request sent')).toBeVisible();
    await expect(
      bob.getByText('Requested 1 more car, waiting for the decision'),
    ).toBeVisible();
    await expect(
      bob.getByRole('button', { name: 'Request more cars' }),
    ).toHaveCount(0);
  });

  test('approves the request as administrator', async ({ page }) => {
    await page.goto('/admin');
    await expect(page.getByText('Open requests')).toBeVisible();
    const request = page
      .locator('.MuiCard-root', { hasText: 'Car requests' })
      .getByText('bob requests 1 more car');
    await expect(request).toBeVisible();
    await expect(page.getByText('“For the trailer”')).toBeVisible();

    await page.getByRole('button', { name: 'Approve', exact: true }).click();
    const dialog = page.getByRole('dialog');
    // the current limit plus the requested car
    await expect(
      dialog.getByRole('textbox', { name: 'Maximum cars' }),
    ).toHaveValue('2');
    await dialog.getByRole('button', { name: 'Approve' }).click();

    await expect(page.getByText('Request approved')).toBeVisible();
    await expect(page.getByText('No requests')).toBeVisible();
    await expect(page.getByRole('row', { name: /bob/ })).toContainText('/ 2');
  });

  test('shows the decision once and lets bob add a car', async () => {
    await bob.goto('/car');
    const outcome = bob.getByText(
      'Your request was approved: you can now have 2 cars.',
    );
    await expect(outcome).toBeVisible();
    await expect(bob.getByRole('main').getByText('Add car')).toBeVisible();

    await bob.getByRole('button', { name: 'Close' }).click();
    await expect(outcome).toHaveCount(0);
    await bob.reload();
    await expect(bob.getByText('1 of 2 cars')).toBeVisible();
    await expect(outcome).toHaveCount(0);
  });
});
