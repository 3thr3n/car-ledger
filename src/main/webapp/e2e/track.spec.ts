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
  let token: string;

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
    token = ((await tracker.json()) as { token: string }).token;
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
    // the last 3 days by default
    await expect(
      page
        .getByRole('group', { name: 'Period' })
        .getByRole('button', { name: 'Last 3 days' }),
    ).toHaveAttribute('aria-pressed', 'true');
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

  test('shows the map in fullscreen and returns', async () => {
    const area = page.getByTestId('track-area');
    const enter = page.getByRole('button', {
      name: 'Show the map in fullscreen',
    });
    await enter.click();

    // the area with the period and the legend covers the window
    const viewport = page.viewportSize()!;
    await expect
      .poll(async () => (await area.boundingBox())?.height)
      .toBeGreaterThanOrEqual(viewport.height - 1);
    const map = await page.getByTestId('track-map').boundingBox();
    expect(map!.height).toBeGreaterThan(viewport.height / 2);
    await expect(page.getByTestId('speed-legend')).toBeInViewport();
    await expect(
      page.getByRole('button', { name: 'Earlier' }),
    ).toBeInViewport();
    // no slip that deletes the whole period
    const remove = page.getByRole('button', { name: 'Delete this track' });
    await expect(remove).toHaveCount(0);

    await page.keyboard.press('Escape');
    await expect(enter).toBeVisible();
    await expect(remove).toBeVisible();
    await expect
      .poll(
        async () => (await page.getByTestId('track-map').boundingBox())?.height,
      )
      .toBeLessThan(500);

    await enter.click();
    await page.getByRole('button', { name: 'Leave the fullscreen' }).click();
    await expect(enter).toBeVisible();
    // the canvas stays inside the map on the page, not over the window
    const back = await page.getByTestId('track-map').boundingBox();
    const canvas = await page
      .getByTestId('track-map')
      .locator('canvas')
      .boundingBox();
    expect(canvas!.y).toBeCloseTo(back!.y, 0);
    expect(canvas!.x).toBeCloseTo(back!.x, 0);
  });

  test('colors the track by how often it was driven', async () => {
    await page.getByRole('button', { name: 'Frequency' }).click();

    const legend = page.getByTestId('frequency-legend');
    await expect(legend).toContainText('Times driven');
    // driven once, the scale still has a range
    await expect(legend.getByRole('img')).toHaveAttribute(
      'aria-label',
      'Driven from 1× (light, thin) to 2× (dark, wide)',
    );
    await expect(page.getByTestId('speed-legend')).toHaveCount(0);

    await page.getByRole('button', { name: 'Speed' }).click();
    await expect(page.getByTestId('speed-legend')).toBeVisible();
  });

  test('shows the places and names them', async () => {
    const api = await aliceApi();
    const named = await api.put(`/api/track/${carId}/places`, {
      data: { lat: 50, lon: 10, name: 'Home' },
    });
    expect(named.status()).toBe(204);
    await api.dispose();

    await page.getByRole('switch', { name: 'Places' }).check();
    // named, but not stopped at today
    const home = page.getByRole('button', {
      name: 'Home, Not visited in this period',
    });
    await expect(home).toBeVisible();

    await home.click();
    const dialog = page.getByRole('dialog', { name: 'Rename Home' });
    await dialog.getByRole('textbox', { name: 'Name' }).fill('Garage');
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Place named')).toBeVisible();
    const garage = page.getByRole('button', { name: /^Garage, / });
    await expect(garage).toBeVisible();

    await garage.click();
    await page
      .getByRole('dialog', { name: 'Rename Garage' })
      .getByRole('button', { name: 'Remove the name' })
      .click();
    await expect(page.getByText('Name removed')).toBeVisible();
    await expect(garage).toHaveCount(0);

    await page.getByRole('switch', { name: 'Places' }).uncheck();
  });

  test('names a place the car stops at', async () => {
    // three stops take a while, the list gets one as the server sends it
    await page.route(
      /\/api\/track\/\d+\/places(\?|$)/,
      async (route) => {
        const response = await route.fetch();
        await route.fulfill({
          response,
          json: [
            ...((await response.json()) as unknown[]),
            {
              id: null,
              name: null,
              lat: 50.0005,
              lon: 10,
              visits: 3,
              lastVisit: new Date(start).toISOString(),
            },
          ],
        });
      },
      { times: 1 },
    );
    await page.getByRole('switch', { name: 'Places' }).check();

    await page.getByRole('button', { name: /^Place, Visited 3×/ }).click();
    const dialog = page.getByRole('dialog', { name: 'Name this place' });
    await dialog.getByRole('textbox', { name: 'Name' }).fill('Office');
    const named = page.waitForRequest(
      (request) => request.method() === 'PUT' && /\/places/.test(request.url()),
    );
    await dialog.getByRole('button', { name: 'Save' }).click();
    expect(new URL((await named).url()).pathname).toBe(
      `/api/track/${carId}/places`,
    );
    // the toast of the rename before may still show
    await expect(page.getByText('Place named').last()).toBeVisible();
    const office = page.getByRole('button', { name: /^Office, / });
    await expect(office).toBeVisible();

    await office.click();
    await page
      .getByRole('dialog', { name: 'Rename Office' })
      .getByRole('button', { name: 'Remove the name' })
      .click();
    await expect(office).toHaveCount(0);
    await page.getByRole('switch', { name: 'Places' }).uncheck();
  });

  test('shows no track in the days before', async () => {
    await page.getByRole('button', { name: 'Earlier' }).click();

    await expect(page.getByText('No track in this period.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Later' })).toBeEnabled();
  });

  test('sets the period with a shortcut', async () => {
    const shortcuts = page.getByRole('group', { name: 'Period' });
    // the day before, moved with the arrows: no shortcut
    await expect(shortcuts.getByRole('button', { pressed: true })).toHaveCount(
      0,
    );

    const week = shortcuts.getByRole('button', { name: 'Last 7 days' });
    const loaded = page.waitForResponse(
      (response) => /\/api\/track\/\d+\?/.test(response.url()) && response.ok(),
    );
    await week.click();
    const url = new URL((await loaded).url());
    const days =
      (Date.parse(url.searchParams.get('to')!) -
        Date.parse(url.searchParams.get('from')!)) /
      86_400_000;
    expect(Math.round(days)).toBe(7);
    await expect(week).toHaveAttribute('aria-pressed', 'true');
    await expect(
      page.getByText('0.1 km GPS distance in 1 section'),
    ).toBeVisible();

    await page.getByRole('button', { name: 'Earlier' }).click();
    await expect(shortcuts.getByRole('button', { pressed: true })).toHaveCount(
      0,
    );

    // back to the default for the next tests
    const last3 = shortcuts.getByRole('button', { name: 'Last 3 days' });
    await last3.click();
    await expect(last3).toHaveAttribute('aria-pressed', 'true');
    await expect(
      page.getByText('0.1 km GPS distance in 1 section'),
    ).toBeVisible();
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

  test('deletes the track of a day, the day before stays', async () => {
    const device = await request.newContext({ baseURL: API_URL });
    const sent = await device.post('/api/device/points', {
      headers: { Authorization: `Bearer ${token}` },
      data: [
        {
          ts: new Date(Date.now() - 24 * 3600_000).toISOString(),
          lat: 50,
          lon: 10,
        },
      ],
    });
    expect(sent.status()).toBe(200);
    await device.dispose();

    await page.getByRole('tab', { name: 'Map' }).click();
    // only today, the default of the last 3 days has yesterday's point as well
    await page
      .getByRole('group', { name: 'Period' })
      .getByRole('button', { name: 'Today' })
      .click();
    await expect(
      page.getByText('0.1 km GPS distance in 1 section'),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Delete this track' }).click();

    const dialog = page.getByRole('dialog', { name: /^Delete the track of/ });
    await expect(dialog).toContainText('11 points of this period');
    await dialog.getByRole('button', { name: 'Delete this track' }).click();

    await expect(page.getByText('11 points deleted')).toBeVisible();
    await expect(page.getByText('No track in this period.')).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Delete this track' }),
    ).toHaveCount(0);

    await page.getByRole('button', { name: 'Earlier' }).click();
    await expect(
      page.getByRole('button', { name: 'Delete this track' }),
    ).toBeVisible();
    await expect(page.getByText('No track in this period.')).toHaveCount(0);
  });
});

test.describe.serial('Drives of the GPS track', () => {
  let page: Page;
  let carId: number;

  /** 80 points, 10 m per second to the north or back */
  const drive = (start: number, north: boolean) =>
    Array.from({ length: 80 }, (_, i) => ({
      ts: new Date(start + i * 1000).toISOString(),
      lat: 50 + (north ? i : 79 - i) * 10 * LAT_PER_METER,
      lon: 10,
      speed: 36,
    }));

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Golf', year: 2021, odometer: 1000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    const tracker = await api.put(`/api/tracker/${carId}`, {
      data: { name: 'ESP32' },
    });
    const { token } = (await tracker.json()) as { token: string };
    await api.dispose();

    // there and back an hour later, the tracker off in between
    const device = await request.newContext({ baseURL: API_URL });
    const sent = await device.post('/api/device/points', {
      headers: { Authorization: `Bearer ${token}` },
      data: [
        ...drive(Date.now() - 2 * 3600_000, true),
        ...drive(Date.now() - 3600_000, false),
      ],
    });
    expect(sent.status()).toBe(200);
    await device.dispose();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
    await deleteAllCars();
  });

  test('fetches the detail of the shown part when zoomed in', async () => {
    const detail = page.waitForResponse(
      (response) =>
        /\/api\/track\/\d+\?.*bbox=.*zoom=/.test(response.url()) &&
        response.ok(),
    );
    await page.goto(`/car/${carId}/track`);

    // the drives fill the map: the lines of 3 days (15 m) would be off
    const response = await detail;
    const body = (await response.json()) as { tolerance: number };
    expect(body.tolerance).toBeLessThan(15);
    // the summary stays the one of the whole period
    await expect(
      page.getByText('1.6 km GPS distance in 2 sections'),
    ).toBeVisible();
  });

  test('lists the drives and shows only the selected ones', async () => {
    await page.goto(`/car/${carId}/track`);
    const list = page.getByTestId('track-drives');
    await expect(list).toContainText('2 drives');
    await expect(
      page.getByText('1.6 km GPS distance in 2 sections'),
    ).toBeVisible();

    // the newest first
    const drives = list.getByTestId('track-drive');
    await expect(drives).toHaveCount(2);
    await drives.first().getByRole('checkbox').click();

    await expect(list).toContainText('1 of 2 drives shown');
    await expect(
      page.getByText('0.7 km GPS distance in 1 section'),
    ).toBeVisible();
    await expect(page).toHaveURL(/drives=/);
    await expect(drives.first().getByRole('checkbox')).toBeChecked();
  });

  test('keeps the selection on a reload', async () => {
    await page.reload();

    const list = page.getByTestId('track-drives');
    await expect(list).toContainText('1 of 2 drives shown');
    await expect(
      page.getByText('0.7 km GPS distance in 1 section'),
    ).toBeVisible();

    await list.getByRole('button', { name: 'Show all' }).click();
    await expect(list).toContainText('2 drives');
    await expect(page).not.toHaveURL(/drives=/);
    await expect(
      page.getByText('1.6 km GPS distance in 2 sections'),
    ).toBeVisible();
  });

  test('deletes a single drive', async () => {
    const list = page.getByTestId('track-drives');
    await list
      .getByTestId('track-drive')
      .last()
      .getByRole('button', { name: /^Delete the drive/ })
      .click();

    const dialog = page.getByRole('dialog', { name: /^Delete the drive of/ });
    await expect(dialog).toContainText('0.7 km');
    await dialog.getByRole('button', { name: 'Delete drive' }).click();

    await expect(page.getByText(/points deleted/)).toBeVisible();
    await expect(list).toContainText('1 drive');
    await expect(list.getByTestId('track-drive')).toHaveCount(1);
  });
});
