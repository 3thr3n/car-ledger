import { expect, Page, request, test } from '@playwright/test';
import { API_URL } from '../playwright.config';
import { aliceApi, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

/** degrees of latitude per meter */
const LAT_PER_METER = 1 / 111_195;

test.describe.serial('Trips suggested from the GPS tracks', () => {
  let page: Page;
  let carId: number;

  /**
   * Yesterday: 10 minutes at 50 km/h, a stop of 20 minutes while the tracker
   * keeps sending, 5 more minutes, then parked
   */
  const sendDrives = async (token: string) => {
    const start = Date.now() - 24 * 3600_000;
    const phases: [number, number][] = [
      // seconds, meters per second
      [60, 0],
      [600, 14],
      [1200, 0],
      [300, 14],
      [600, 0],
    ];
    const points = [];
    let second = 0;
    let north = 0;
    for (const [seconds, speed] of phases) {
      // a point every 5 seconds
      for (let i = 0; i < seconds; i += 5) {
        north += speed * 5;
        points.push({
          ts: new Date(start + second * 1000).toISOString(),
          lat: 50 + north * LAT_PER_METER,
          lon: 10,
          speed: speed * 3.6,
        });
        second += 5;
      }
    }
    const device = await request.newContext({ baseURL: API_URL });
    for (let i = 0; i < points.length; i += 500) {
      const sent = await device.post('/api/device/points', {
        headers: { Authorization: `Bearer ${token}` },
        data: points.slice(i, i + 500),
      });
      expect(sent.status()).toBe(200);
    }
    await device.dispose();
  };

  test.beforeAll(async ({ browser }) => {
    const api = await aliceApi();
    const created = await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2020, odometer: 50000, tripLog: true },
    });
    expect(created.ok()).toBeTruthy();
    const cars = (await (await api.get('/api/car/my')).json()) as {
      id: number;
    }[];
    carId = cars[0].id;
    const tracker = await api.put(`/api/tracker/${carId}`, {
      data: { name: 'ESP32' },
    });
    const { token } = (await tracker.json()) as { token: string };
    await api.dispose();
    await sendDrives(token);
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
  });

  test('adds a drive as a trip, prefilled', async () => {
    await page.goto(`/car/${carId}/trips`);
    const suggestions = page.getByTestId('trip-suggestion');
    await expect(suggestions).toHaveCount(2);
    await expect(suggestions.nth(0)).toContainText(/8\.\d km by GPS/);

    await suggestions
      .nth(0)
      .getByRole('button', { name: 'Add as trip' })
      .click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Prefilled from the GPS track');
    await expect(
      dialog.getByRole('textbox', { name: /Odometer at the start/ }),
    ).toHaveValue(/50,000/);
    await expect(
      dialog.getByRole('textbox', { name: /Odometer at the finish/ }),
    ).toHaveValue(/50,008/);
    await page.getByRole('combobox', { name: 'Type' }).click();
    await page.getByRole('option', { name: 'Private' }).click();
    await dialog.getByRole('button', { name: 'Add trip' }).click();

    await expect(page.getByText('Trip added')).toBeVisible();
    await expect(
      page.getByTestId('trip-list').getByRole('row', { name: /50,008 km/ }),
    ).toContainText('Entered later');
    // the window starts after the new trip
    await expect(suggestions).toHaveCount(1);
    await expect(suggestions.nth(0)).toContainText(/4\.\d km by GPS/);
  });

  test('dismisses a drive for good', async () => {
    await page
      .getByTestId('trip-suggestion')
      .getByRole('button', { name: 'Dismiss' })
      .click();

    await expect(page.getByText('Not suggested again')).toBeVisible();
    await expect(page.getByTestId('trip-suggestions')).toHaveCount(0);
    await page.reload();
    await expect(page.getByTestId('trip-list')).toBeVisible();
    await expect(page.getByTestId('trip-suggestions')).toHaveCount(0);
  });
});
