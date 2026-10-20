import {
  APIRequestContext,
  BrowserContext,
  expect,
  Page,
  test,
} from '@playwright/test';
import { aliceApi, deleteAllCars, money } from './helpers';

/**
 * Refuels without a connection: kept on the device, sent when online again.
 * The app is opened online first, the dev server has no service worker.
 */
test.describe.serial('offline', () => {
  let api: APIRequestContext;
  let carId: number;

  test.beforeAll(async () => {
    await deleteAllCars();
    api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Offline', year: 2021, odometer: 20000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
  });

  test.afterAll(async () => {
    await api.dispose();
    await deleteAllCars();
  });

  const field = (page: Page, name: RegExp) =>
    page.getByRole('textbox', { name });

  /** the form of a new refuel, opened in the app, then the connection is lost */
  async function openFormOffline(page: Page, context: BrowserContext) {
    await page.goto(`/car/${carId}/bill/fuel`);
    await page.getByRole('link', { name: 'Add entry' }).first().click();
    await expect(field(page, /^Total/)).toBeVisible();
    await context.setOffline(true);
  }

  const refuels = async () =>
    (
      (await (await api.get(`/api/bill/${carId}/fuel/all`)).json()) as {
        total: number;
      }
    ).total;

  test('keeps a refuel without a connection and sends it later', async ({
    page,
    context,
  }) => {
    await openFormOffline(page, context);
    await field(page, /^Total/).fill('60');
    await field(page, /^Liters/).fill('40');
    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(
      page.getByText('No connection: saved on this device'),
    ).toBeVisible();
    await expect(page).toHaveURL(/\/bill\/fuel$/);
    const queued = page.getByTestId('queued-entries');
    await expect(queued).toContainText('Is sent as soon as you are online');
    await expect(queued).toContainText(money(60));
    expect(await refuels()).toBe(0);

    await context.setOffline(false);

    await expect(page.getByText('1 entry made offline was sent')).toBeVisible();
    await expect(queued).toHaveCount(0);
    await expect(page.getByRole('row', { name: /150\.0 ct\/l/ })).toBeVisible();
    expect(await refuels()).toBe(1);
  });

  test('keeps a rejected refuel to correct it', async ({ page, context }) => {
    // the same date and total as the refuel of the test before
    await openFormOffline(page, context);
    await field(page, /^Total/).fill('60');
    await field(page, /^Liters/).fill('30');
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByTestId('queued-entries')).toBeVisible();

    await context.setOffline(false);

    await expect(
      page.getByText('1 entry made offline was rejected'),
    ).toBeVisible();
    const queued = page.getByTestId('queued-entries');
    await expect(queued).toContainText('already exists');

    await queued.getByRole('link', { name: 'Correct' }).click();
    await expect(field(page, /^Total/)).toHaveValue('60 €');
    await expect(field(page, /^Liters/)).toHaveValue('30 l');
    await field(page, /^Total/).fill('61');
    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(page).toHaveURL(/\/bill\/fuel$/);
    await expect(page.getByTestId('queued-entries')).toHaveCount(0);
    await expect(page.getByRole('row', { name: money(61) })).toBeVisible();
    expect(await refuels()).toBe(2);
  });
});
