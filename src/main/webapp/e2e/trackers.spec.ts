import { expect, Page, request, test } from '@playwright/test';
import { ALICE, API_URL } from '../playwright.config';
import { aliceApi, deleteAllCars } from './helpers';

test.describe.serial('GPS trackers', () => {
  let page: Page;
  let carId: number;
  let token: string;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    const created = await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2020, odometer: 50000 },
    });
    expect(created.ok()).toBeTruthy();
    const cars = (await (await api.get('/api/car/my')).json()) as {
      id: number;
    }[];
    carId = cars[0].id;
    await api.dispose();

    const context = await browser.newContext({
      storageState: ALICE,
      permissions: ['clipboard-read', 'clipboard-write'],
    });
    page = await context.newPage();
  });

  test.afterAll(async () => {
    await page.context().close();
    await deleteAllCars();
  });

  /** sends a point as the tracker, without the session of the browser */
  const sendPoint = async () => {
    const device = await request.newContext({ baseURL: API_URL });
    const response = await device.post('/api/device/points', {
      headers: { Authorization: `Bearer ${token}` },
      data: [{ ts: new Date().toISOString(), lat: 51.1, lon: 10.2 }],
    });
    await device.dispose();
    return response.status();
  };

  test('creates a tracker and shows its token once', async () => {
    await page.goto(`/car/${carId}/edit`);
    const section = page.getByTestId('trackers');
    await section.getByRole('button', { name: 'Add tracker' }).click();

    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'Create' }).click();
    await expect(dialog).toContainText('Please enter a name');

    await dialog.getByRole('textbox', { name: /^Name/ }).fill('ESP32');
    await dialog.getByRole('button', { name: 'Create' }).click();

    await expect(dialog).toContainText('It is shown only once');
    token = await dialog.getByTestId('tracker-token').inputValue();
    expect(token).toMatch(/^cl_[\w-]{43}$/);

    await dialog.getByRole('button', { name: 'Copy token' }).click();
    await expect(page.getByText('Token copied')).toBeVisible();
    expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
      token,
    );

    await dialog.getByRole('button', { name: 'Done' }).click();
    await expect(dialog).toBeHidden();
    const tracker = section.getByTestId('tracker').filter({ hasText: 'ESP32' });
    await expect(tracker).toContainText('Not seen yet');
    // the token is gone with the dialog
    await expect(page.getByText(token)).toHaveCount(0);
  });

  test('shows when the tracker sent the last time', async () => {
    expect(await sendPoint()).toBe(200);

    await page.reload();
    const tracker = page.getByTestId('tracker').filter({ hasText: 'ESP32' });
    await expect(tracker).toContainText('Last seen');
  });

  test('revokes the tracker after a confirmation', async () => {
    const tracker = page.getByTestId('tracker').filter({ hasText: 'ESP32' });
    await tracker.getByRole('button', { name: 'Revoke' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Revoke ESP32?');
    await dialog.getByRole('button', { name: 'Revoke' }).click();

    await expect(page.getByText('Tracker revoked')).toBeVisible();
    await expect(tracker).toContainText('Revoked on');
    await expect(tracker.getByRole('button', { name: 'Revoke' })).toHaveCount(
      0,
    );
    expect(await sendPoint()).toBe(401);
  });

  test('keeps the tracks for a while after a confirmation', async () => {
    const history = page.getByTestId('track-history');
    await expect(history).toContainText('The tracks are part of the backup');

    await history.getByRole('combobox', { name: 'Keep the tracks' }).click();
    await page.getByRole('option', { name: '30 days' }).click();
    const dialog = page.getByRole('dialog', {
      name: 'Keep the tracks for 30 days?',
    });
    await expect(dialog).toContainText('deleted tonight');
    await dialog.getByRole('button', { name: 'Keep for 30 days' }).click();

    await expect(history).toContainText('not part of the backup');
    await page.reload();
    await expect(
      page.getByRole('combobox', { name: 'Keep the tracks' }),
    ).toHaveText('30 days');

    // longer needs no confirmation
    await page.getByRole('combobox', { name: 'Keep the tracks' }).click();
    await page.getByRole('option', { name: 'Forever' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(history).toContainText('The tracks are part of the backup');
  });

  test('deletes all tracks after a confirmation', async () => {
    const history = page.getByTestId('track-history');
    await history.getByRole('button', { name: 'Delete all tracks' }).click();

    const dialog = page.getByRole('dialog', {
      name: 'Delete all tracks of this car?',
    });
    await dialog.getByRole('button', { name: 'Delete all tracks' }).click();
    // the point of the tracker before it was revoked
    await expect(page.getByText('1 point deleted')).toBeVisible();
  });
});
