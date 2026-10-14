import { APIRequestContext, expect, test } from '@playwright/test';
import { aliceApi, deleteAllCars } from './helpers';

/**
 * Miles and gallons: the API keeps km and liters, the app shows and takes the
 * units of the account
 */
test.describe.serial('units', () => {
  let api: APIRequestContext;
  let carId: number;

  test.beforeAll(async () => {
    await deleteAllCars();
    api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Mustang', year: 2020, odometer: 1000, tankSize: 60 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    // 10 gallons for $3.459 each, 300 miles: 30 mpg (US)
    await api.put(`/api/bill/${carId}/fuel`, {
      data: {
        date: '2026-03-01',
        vatRate: 0,
        unit: 37.85,
        pricePerUnit: 91.38,
        distance: 482.8,
      },
    });
  });

  test.afterAll(async () => {
    // back to the defaults, the other tests expect them
    await api.put('/api/user/me/settings', {
      data: {
        vatCountry: 'DE',
        currency: 'EUR',
        unitSystem: 'METRIC',
      },
    });
    await api.dispose();
    await deleteAllCars();
  });

  test('shows the refuels in miles, gallons and mpg', async ({ page }) => {
    await page.goto('/settings');
    await page.getByRole('combobox', { name: 'Units' }).click();
    await page.getByRole('option', { name: /^US/ }).click();
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('Settings saved')).toBeVisible();

    await page.goto(`/car/${carId}/bill/fuel`);
    await expect(
      page.getByRole('columnheader', { name: 'Gallons' }),
    ).toBeVisible();
    const row = page.getByRole('row', { name: /10\.00 gal/ });
    await expect(row).toContainText(/3\.459\/gal/);
    await expect(row).toContainText('300.0 mi');
    await expect(row).toContainText('30.0 mpg');
  });

  test('opens the edit form in gallons on a direct load', async ({ page }) => {
    const bills = await (await api.get(`/api/bill/${carId}/fuel/all`)).json();
    const billId = (bills.data as { id: number }[])[0].id;

    // the form starts with the units of the account, loaded before the page
    await page.goto(`/car/${carId}/bill/${billId}/edit?type=FUEL`);
    await expect(page.getByRole('textbox', { name: /^Gallons/ })).toHaveValue(
      '10 gal',
    );
    await expect(page.getByRole('textbox', { name: /^Distance/ })).toHaveValue(
      '300 mi',
    );
  });

  test('takes a refuel in gallons and miles', async ({ page }) => {
    await page.goto(`/car/${carId}/bill/add?type=FUEL`);
    const field = (name: RegExp) => page.getByRole('textbox', { name });

    // more than the tank of 60 l (15.9 gal) holds
    await field(/^Gallons/).fill('20');
    await expect(
      page.getByText(/20\.00 gal is more than the tank holds \(15\.9 gal\)/),
    ).toBeVisible();
    await field(/^Gallons/).fill('12');
    await field(/^Total/).fill('42');
    await expect(page.getByText(/Calculated: 3\.500 .\/gal/)).toBeVisible();
    await field(/^Distance/).fill('348');
    await expect(page.getByText('Consumption: 29.0 mpg')).toBeVisible();
    await expect(page.getByText('Miles since the last refuel')).toHaveCount(0);
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page).toHaveURL(/\/bill\/fuel$/);
    await expect(page.getByRole('row', { name: /12\.00 gal/ })).toBeVisible();

    // the API keeps liters and km
    const bills = await (
      await api.get(`/api/bill/${carId}/fuel/all?sort=date&order=DESC`)
    ).json();
    const added = (
      bills.data as { unit: number; distance: number; total: number }[]
    ).find((bill) => bill.total === 42);
    expect(added?.unit).toBeCloseTo(45.42, 2);
    expect(added?.distance).toBeCloseTo(560.05, 2);
  });
});
