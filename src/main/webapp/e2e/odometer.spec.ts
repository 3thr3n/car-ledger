import { APIRequestContext, expect, test } from '@playwright/test';
import { aliceApi, day, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test.describe.serial('odometer', () => {
  let api: APIRequestContext;
  let carId: number;

  test.beforeAll(async () => {
    api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Corsa', year: 2019, odometer: 30000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
  });

  test.afterAll(async () => {
    await api.dispose();
  });

  test('adds the refuels since the latest reading', async ({ page }) => {
    // a maintenance today is the latest reading, the refuel tomorrow is added
    expect(
      (
        await api.put(`/api/bill/${carId}/maintenance`, {
          data: { date: day(0), vatRate: 19, total: 99, odometer: 31000 },
        })
      ).ok(),
    ).toBeTruthy();
    expect(
      (
        await api.put(`/api/bill/${carId}/fuel`, {
          data: {
            date: day(1),
            vatRate: 19,
            unit: 30,
            pricePerUnit: 170,
            distance: 400,
          },
        })
      ).ok(),
    ).toBeTruthy();

    await page.goto(`/car/${carId}`);
    const odometer = page.getByText('31,400 km');
    await expect(odometer).toBeVisible();
    await odometer.hover();
    await expect(page.getByRole('tooltip')).toContainText(
      'Last reading 31,000 km on',
    );
    await expect(page.getByRole('tooltip')).toContainText(
      'plus 400 km from the refuels since',
    );

    await page.goto(`/car/${carId}/bill/add?type=MAINTENANCE`);
    await expect(page.getByText('Currently about 31,400 km')).toBeVisible();
  });

  test('leaves out charging for a petrol car', async ({ page }) => {
    // the refuel and the maintenance of the test before are the costs
    await page.goto(`/car/${carId}`);
    const chart = page.locator('.MuiCard-root', { hasText: 'Costs per month' });
    // the legend, the table for screen readers has the same names
    const legend = chart.getByRole('list');
    await expect(
      legend.getByText('Maintenance', { exact: true }),
    ).toBeVisible();
    await expect(legend.getByText('Charging', { exact: true })).toHaveCount(0);

    await page.goto('/dashboard');
    const split = page.locator('.MuiCard-root', { hasText: 'Cost breakdown' });
    await expect(split.getByText('Fuel', { exact: false })).toBeVisible();
    await expect(split.getByText(/^Charging/)).toHaveCount(0);
  });

  test('keeps the reading when the car is saved unchanged', async ({
    page,
  }) => {
    await page.goto(`/car/${carId}/edit`);
    const field = page.getByRole('textbox', { name: /^Odometer/ });
    await expect(field).toHaveValue('31,400 km');
    await page.getByRole('textbox', { name: /^Name/ }).fill('E2E Corsa GS');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page).toHaveURL(new RegExp(`/car/${carId}$`));
    await expect(page.getByText('31,400 km')).toBeVisible();
    const car = await (await api.get(`/api/car/my/${carId}`)).json();
    expect(car.odometer).toBe(30000);
  });

  test('saves a changed odometer as a new reading', async ({ page }) => {
    await page.goto(`/car/${carId}/edit`);
    await page.getByRole('textbox', { name: /^Odometer/ }).fill('32000');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page).toHaveURL(new RegExp(`/car/${carId}$`));
    // the refuel of tomorrow is after the new reading
    await expect(page.getByText('32,400 km')).toBeVisible();
  });
});
