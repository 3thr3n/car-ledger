import {
  APIRequestContext,
  BrowserContext,
  expect,
  Page,
  test,
} from '@playwright/test';
import { aliceApi, deleteAllCars } from './helpers';

interface Trip {
  id: string;
  startOdometer: number;
  endOdometer?: number | null;
  distance?: number;
  startedAt: string;
  endedAt?: string;
  destination?: string;
  purpose?: string;
  businessPartner?: string;
  reviewReason?: string | null;
  createdAt: string;
  finishedAt?: string;
}

/**
 * The trip log: start and finish in the dialogs, also without a connection.
 * The trips of one car follow each other, so the tests run in order.
 */
test.describe.serial('trips', () => {
  let api: APIRequestContext;
  let carId: number;

  test.beforeAll(async () => {
    await deleteAllCars();
    api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Trips', year: 2021, odometer: 20000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
  });

  test.afterAll(async () => {
    await api.dispose();
    await deleteAllCars();
  });

  const trips = async () =>
    (
      (await (await api.get(`/api/trip/${carId}?size=50`)).json()) as {
        total: number;
        data: Trip[];
      }
    ).data;

  const odometer = (page: Page, name: RegExp) =>
    page.getByRole('textbox', { name });

  async function openTrips(page: Page) {
    await page.goto(`/car/${carId}/trips`);
    await expect(page.getByTestId('current-trip')).toBeVisible();
  }

  /** a private trip, it needs no details */
  async function startTrip(page: Page, value?: string) {
    await page.getByRole('button', { name: 'Start trip' }).click();
    const field = odometer(page, /Odometer at the start/);
    await expect(field).toBeVisible();
    if (value) await field.fill(value);
    await page.getByRole('combobox', { name: 'Type' }).click();
    await page.getByRole('option', { name: 'Private' }).click();
    await page.getByRole('button', { name: 'Start', exact: true }).click();
  }

  async function finishTrip(page: Page, value: string) {
    await page.getByRole('button', { name: 'Finish trip' }).click();
    await odometer(page, /Odometer at the finish/).fill(value);
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Finish trip' })
      .click();
  }

  test('starts and finishes a trip', async ({ page }) => {
    await openTrips(page);
    await expect(page.getByTestId('current-trip')).toContainText(
      'No trip is open',
    );

    await page.getByRole('button', { name: 'Start trip' }).click();
    const start = odometer(page, /Odometer at the start/);
    // the highest reading of the car
    await expect(start).toHaveValue('20,000 km');
    await start.fill('19999');
    await expect(page.getByText('At least 20,000 km')).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Start', exact: true }),
    ).toBeDisabled();
    await start.fill('20000');
    await page.getByRole('textbox', { name: 'Destination' }).fill('Office');
    await page.getByRole('button', { name: 'Start', exact: true }).click();

    await expect(page.getByText('Trip started')).toBeVisible();
    const current = page.getByTestId('current-trip');
    await expect(current).toContainText('Open trip · Business · Office');

    await page.getByRole('button', { name: 'Finish trip' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText("can't be changed anymore");
    // given at the start, it can't be changed
    await expect(
      dialog.getByRole('textbox', { name: 'Destination' }),
    ).toBeDisabled();
    await odometer(page, /Odometer at the finish/).fill('20042');
    await dialog.getByRole('button', { name: 'Finish trip' }).click();
    // a business trip needs all details, at the latest now
    await expect(dialog.getByText('Needed for a business trip')).toHaveCount(2);
    await dialog.getByRole('textbox', { name: 'Purpose' }).fill('Meeting');
    await dialog
      .getByRole('textbox', { name: 'Business partner' })
      .fill('ACME');
    await dialog.getByRole('button', { name: 'Finish trip' }).click();

    await expect(page.getByText('Trip finished')).toBeVisible();
    await expect(current).toContainText('No trip is open');
    const row = page.getByTestId('trip-list').getByRole('row', {
      name: /Office/,
    });
    await expect(row).toContainText('42 km');
    await expect(row).toContainText('Meeting');

    const [trip] = await trips();
    expect(trip).toMatchObject({
      startOdometer: 20000,
      endOdometer: 20042,
      distance: 42,
      destination: 'Office',
      purpose: 'Meeting',
      businessPartner: 'ACME',
    });
  });

  test('shows the minimum of the server at the field', async ({ page }) => {
    await openTrips(page);
    await page.getByRole('button', { name: 'Start trip' }).click();
    await expect(odometer(page, /Odometer at the start/)).toHaveValue(
      '20,042 km',
    );

    // meanwhile a trip on another device
    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    await api.put(`/api/trip/${carId}/${id}`, {
      data: { type: 'PRIVATE', startOdometer: 20042, startedAt: now },
    });
    await api.put(`/api/trip/${carId}/${id}/finish`, {
      data: { endOdometer: 20100, endedAt: now },
    });

    await page.getByRole('button', { name: 'Start', exact: true }).click();
    await expect(page.getByText('At least 20,100 km')).toBeVisible();
    await page.getByRole('button', { name: 'Cancel' }).click();
  });

  /**
   * Starts online and finishes offline: the finish is kept with the time of
   * the device and sent when the connection is back.
   */
  async function finishOffline(page: Page, context: BrowserContext) {
    await openTrips(page);
    await startTrip(page);
    await expect(page.getByText('Trip started')).toBeVisible();

    await context.setOffline(true);
    await finishTrip(page, '20150');
    await expect(
      page.getByText('No connection: the trip is kept on this device'),
    ).toBeVisible();
    await expect(page.getByTestId('current-trip')).toContainText(
      'No trip is open',
    );
    await expect(page.getByTestId('pending-trips')).toContainText(
      'Finish at 20,150 km',
    );
  }

  test('finishes a trip without a connection', async ({ page, context }) => {
    await finishOffline(page, context);
    const open = (await trips()).find((trip) => trip.startOdometer === 20100);
    expect(open?.endOdometer ?? null).toBeNull();

    // the arrival at the server is later than the finish on the device
    await page.waitForTimeout(1500);
    await context.setOffline(false);

    await expect(page.getByText('1 entry made offline was sent')).toBeVisible();
    await expect(page.getByTestId('pending-trips')).toHaveCount(0);
    const trip = (await trips()).find((t) => t.startOdometer === 20100)!;
    expect(trip).toMatchObject({ endOdometer: 20150, distance: 50 });
    expect(trip.reviewReason ?? null).toBeNull();
    expect(
      new Date(trip.finishedAt!).getTime() - new Date(trip.endedAt!).getTime(),
    ).toBeGreaterThan(1000);
  });

  test('starts and finishes a trip without a connection', async ({
    page,
    context,
  }) => {
    await openTrips(page);
    await context.setOffline(true);

    await startTrip(page, '20150');
    await expect(
      page.getByText('No connection: the trip is kept on this device'),
    ).toBeVisible();
    const current = page.getByTestId('current-trip');
    await expect(current).toContainText('Open trip');
    await expect(current).toContainText('not sent yet');

    await finishTrip(page, '20180');
    await expect(current).toContainText('No trip is open');
    const pending = page.getByTestId('pending-trips');
    await expect(pending).toContainText('Start at 20,150 km');
    await expect(pending).toContainText('Finish at 20,180 km');
    expect(await trips()).toHaveLength(3);

    await context.setOffline(false);

    await expect(
      page.getByText('2 entries made offline were sent'),
    ).toBeVisible();
    await expect(pending).toHaveCount(0);
    const all = await trips();
    expect(all).toHaveLength(4);
    expect(all[0]).toMatchObject({
      startOdometer: 20150,
      endOdometer: 20180,
      distance: 30,
    });
  });
});
