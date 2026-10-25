import { expect, test } from '@playwright/test';
import { aliceApi } from './helpers';

/** as the server has it */
async function accentOfTheAccount() {
  const api = await aliceApi();
  const { accent } = (await (await api.get('/api/user/me')).json()) as {
    accent: string;
  };
  await api.dispose();
  return accent;
}

test.describe.serial('settings', () => {
  test.afterAll(async () => {
    // back to the defaults, the other tests expect them
    const api = await aliceApi();
    await api.put('/api/user/me/settings', {
      data: { vatCountry: 'DE', currency: 'EUR', accent: 'TEAL' },
    });
    await api.dispose();
  });

  test('changes the currency of all amounts', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByRole('link', { name: 'Settings' }).click();
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible();

    await page.getByRole('combobox', { name: 'Currency' }).click();
    await page.getByRole('option', { name: /USD/ }).click();
    await expect(
      page.getByText('Amounts are shown like $1,234.50'),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Settings saved')).toBeVisible();

    await page.goto('/dashboard');
    await expect(page.getByText('$0.00').first()).toBeVisible();
  });

  test('keeps the settings after a reload', async ({ page }) => {
    await page.goto('/settings');
    await expect(
      page.getByRole('combobox', { name: 'Currency' }),
    ).toContainText('USD');
  });

  test('switches the language with the account', async ({ page }) => {
    await page.goto('/settings');
    await page.getByRole('combobox', { name: 'Language' }).click();
    await page.getByRole('option', { name: 'Deutsch' }).click();
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(
      page.getByRole('heading', { name: 'Einstellungen' }),
    ).toBeVisible();

    // the language is part of the account, not only of this browser
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(
      page.getByRole('heading', { name: 'Einstellungen' }),
    ).toBeVisible();

    await page.getByRole('combobox', { name: 'Sprache' }).click();
    await page.getByRole('option', { name: 'English' }).click();
    await page.getByRole('button', { name: 'Speichern' }).click();
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible();
  });

  test('colors the app with the chosen accent', async ({ page }) => {
    await page.goto('/settings');
    const save = page.getByRole('button', { name: 'Save' });
    // teal
    await expect(save).toHaveCSS('background-color', 'rgb(15, 118, 110)');

    await page.getByRole('radio', { name: 'Violet' }).check();
    await expect(page.getByRole('radio', { name: 'Violet' })).toBeChecked();
    await expect(save).toHaveCSS('background-color', 'rgb(109, 40, 217)');
    await expect.poll(accentOfTheAccount).toBe('VIOLET');

    // with the account, not only in this browser
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(save).toHaveCSS('background-color', 'rgb(109, 40, 217)');
    await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute(
      'content',
      '#6d28d9',
    );
  });

  test('colors the bar before the app starts', async ({ page }) => {
    await page.goto('/settings');
    const themeColor = page.locator('meta[name="theme-color"]');
    await expect(themeColor).toHaveAttribute('content', '#6d28d9');

    // some browsers take the color only while the page loads
    await page.route('**/src/main.tsx', (route) => route.abort());
    await page.reload();
    await expect(themeColor).toHaveAttribute('content', '#6d28d9');
  });

  test('changes the accent without a connection', async ({ page, context }) => {
    await page.goto('/settings');
    const save = page.getByRole('button', { name: 'Save' });
    await expect(save).toHaveCSS('background-color', 'rgb(109, 40, 217)');
    await context.setOffline(true);

    // shown at once, and changed again
    await page.getByRole('radio', { name: 'Amber' }).check();
    await expect(save).toHaveCSS('background-color', 'rgb(180, 83, 9)');
    await page.getByRole('radio', { name: 'Rose' }).check();
    await expect(page.getByRole('radio', { name: 'Rose' })).toBeChecked();
    await expect(save).toHaveCSS('background-color', 'rgb(190, 18, 60)');
    expect(await accentOfTheAccount()).toBe('VIOLET');

    // the last choice is saved once the connection is back
    await context.setOffline(false);
    await expect.poll(accentOfTheAccount).toBe('ROSE');
    await expect(save).toHaveCSS('background-color', 'rgb(190, 18, 60)');
  });

  test('shows why no notifications can be sent', async ({ page }) => {
    // the users of the Keycloak dev service have no address, and the dev server
    // has no service worker for push
    await page.goto('/settings');
    await expect(
      page.getByText('Your login has no e-mail address.', { exact: false }),
    ).toBeVisible();
    await expect(
      page.getByRole('switch', { name: 'By e-mail' }),
    ).toBeDisabled();
    await expect(page.getByTestId('push-hint')).toHaveText(
      'This browser cannot receive push notifications here. Open the installed app or another browser.',
    );
    await expect(
      page.getByRole('switch', { name: 'Push on this device' }),
    ).toBeDisabled();
    await expect(
      page.getByText('Turn on e-mail or push first', { exact: false }),
    ).toBeVisible();
    await expect(
      page.getByRole('switch', {
        name: 'Reminders of my cars that are due soon (once a day)',
      }),
    ).toBeDisabled();
  });

  test('turns a notification off', async ({ page }) => {
    // as if the login had an address, the change is saved by the backend
    await page.route('**/api/user/me/notifications', (route) =>
      route.request().method() === 'GET'
        ? route.fulfill({
            json: {
              available: true,
              byMail: true,
              email: 'alice@example.com',
              newRequests: true,
              requestDecided: true,
              reminders: true,
            },
          })
        : route.continue(),
    );
    await page.goto('/settings');
    await expect(
      page.getByText('Sent to alice@example.com, the address of your login.'),
    ).toBeVisible();
    const newRequests = page.getByRole('switch', {
      name: 'New requests for more cars (as administrator)',
    });
    await expect(newRequests).toBeChecked();
    await expect(page.getByRole('switch', { name: 'By e-mail' })).toBeChecked();
    await expect(
      page.getByRole('switch', {
        name: 'Reminders of my cars that are due soon (once a day)',
      }),
    ).toBeChecked();

    await newRequests.click();
    await expect(page.getByText('Notifications saved')).toBeVisible();

    const api = await aliceApi();
    const saved = await (await api.get('/api/user/me/notifications')).json();
    expect(saved.newRequests).toBe(false);
    expect(saved.requestDecided).toBe(true);
    expect(saved.reminders).toBe(true);
    await api.put('/api/user/me/notifications', {
      data: { newRequests: true },
    });
    await api.dispose();
  });
});
