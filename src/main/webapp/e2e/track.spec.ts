import { expect, Page, request, test } from '@playwright/test';
import { API_URL } from '../playwright.config';
import { aliceApi, deleteAllCars } from './helpers';

/** degrees of latitude per meter */
const LAT_PER_METER = 1 / 111_195;

test.describe.serial('GPS track', () => {
  let page: Page;
  let carId: number;
  /** time of the first point */
  let start: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    const created = await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2020, odometer: 50000, tripLog: true },
    });
    expect(created.ok()).toBeTruthy();
    const cars = (await (await api.get('/api/car/my')).json()) as {
      id: number;
    }[];
    carId = cars[0].id;
    await api.dispose();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
    await deleteAllCars();
  });

  test('shows the map tab only with a tracker', async () => {
    await page.goto(`/car/${carId}`);
    await expect(page.getByRole('tab', { name: 'Reminders' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Map' })).toHaveCount(0);

    const api = await aliceApi();
    const tracker = await api.put(`/api/tracker/${carId}`, {
      data: { name: 'ESP32' },
    });
    expect(tracker.status()).toBe(201);
    const { token } = (await tracker.json()) as { token: string };
    await api.dispose();

    // 11 points in the last minute, 10 m apart to the north, faster and faster
    start = Date.now() - 60_000;
    const device = await request.newContext({ baseURL: API_URL });
    const sent = await device.post('/api/device/points', {
      headers: { Authorization: `Bearer ${token}` },
      data: Array.from({ length: 11 }, (_, i) => ({
        ts: new Date(start + i * 1000).toISOString(),
        lat: 50 + i * 10 * LAT_PER_METER,
        lon: 10,
        speed: i * 15,
      })),
    });
    expect(sent.status()).toBe(200);
    await device.dispose();

    await page.reload();
    await page.getByRole('tab', { name: 'Map' }).click();
    await expect(page).toHaveURL(/\/track$/);
  });

  test('shows the track of today with the speed scale', async () => {
    await expect(
      page.getByText('0.1 km GPS distance in 1 section'),
    ).toBeVisible();
    await expect(page.getByTestId('track-map').locator('canvas')).toBeVisible();

    const legend = page.getByTestId('speed-legend');
    await expect(legend).toContainText('Speed (km/h)');
    await expect(legend).toContainText('160');
    await expect(legend.getByRole('img')).toHaveAttribute(
      'aria-label',
      'Speed from 0 km/h (light) to 160 km/h (dark)',
    );
  });

  test('shows no track on the day before', async () => {
    await page.getByRole('button', { name: 'Earlier' }).click();

    await expect(page.getByText('No track in this period.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Later' })).toBeEnabled();
  });

  test('shows the track of a trip next to its odometer distance', async () => {
    const api = await aliceApi();
    const trip = await api.put(
      `/api/trip/${carId}/${crypto.randomUUID()}/past`,
      {
        data: {
          type: 'PRIVATE',
          startOdometer: 50000,
          startedAt: new Date(start - 1000).toISOString(),
          endOdometer: 50001,
          endedAt: new Date(start + 11_000).toISOString(),
        },
      },
    );
    expect(trip.status()).toBe(201);
    await api.dispose();

    await page.getByRole('tab', { name: 'Trips' }).click();
    const show = page.getByRole('button', { name: 'Show GPS track' });
    await show.click();
    await expect(show).toHaveAttribute('aria-expanded', 'true');

    const track = page.getByTestId('trip-track');
    await expect(track).toContainText('GPS distance 0.1 km · by odometer 1 km');
    await expect(track).toContainText('Only a hint');
    await expect(
      track.getByTestId('track-map').locator('canvas'),
    ).toBeVisible();
  });
});
