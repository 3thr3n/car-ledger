import { expect, test } from '@playwright/test';
import { aliceApi, day, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test.describe.serial('Summary of the cost lists', () => {
  let carId: number;

  test.beforeAll(async () => {
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Summary', year: 2020, odometer: 20000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    for (const [offset, unit, pricePerUnit] of [
      [-1, 40, 200],
      [-2, 30, 180],
    ]) {
      const fuel = await api.put(`/api/bill/${carId}/fuel`, {
        data: {
          date: day(offset),
          vatRate: 19,
          unit,
          pricePerUnit,
          distance: 500,
          total: (unit * pricePerUnit) / 100,
        },
      });
      expect(fuel.ok()).toBeTruthy();
    }
    const maintenance = await api.put(`/api/bill/${carId}/maintenance`, {
      data: {
        date: day(-3),
        vatRate: 19,
        description: 'Oil change',
        laborCost: 90,
        partsCost: 60,
        total: 150,
      },
    });
    expect(maintenance.ok()).toBeTruthy();
    await api.dispose();
  });

  test('shows the refuels like the display of a pump', async ({ page }) => {
    await page.goto(`/car/${carId}/bill/fuel`);
    const summary = page.getByRole('region', { name: 'Summary of the list' });
    await expect(summary).toContainText('Paid');
    await expect(summary).toContainText('€134.00');
    await expect(summary).toContainText('70.0 l');
    await expect(summary).toContainText('In all years');
    await expect(summary).toContainText('2 refuels');

    // the year of the chip
    const year = day(-2).slice(0, 4);
    await page.getByRole('button', { name: year, exact: true }).click();
    await expect(summary).toContainText(`In ${year}`);
  });

  test('sums up the maintenances in a sentence', async ({ page }) => {
    await page.goto(`/car/${carId}/bill/maintenance`);
    const summary = page.getByRole('region', { name: 'Summary of the list' });
    await expect(summary).toContainText('€150.00');
    await expect(summary).toContainText('In all years for 1 maintenance');
  });

  test('shows the running recurring costs a month', async ({ page }) => {
    const api = await aliceApi();
    for (const data of [
      { name: 'Insurance', billInterval: 'MONTHLY', amount: 30 },
      { name: 'Tax', billInterval: 'ANNUALLY', amount: 120 },
    ]) {
      const created = await api.put(`/api/bill/${carId}/recurring`, {
        data: { ...data, category: 'OTHER', startDate: day(-10) },
      });
      expect(created.ok()).toBeTruthy();
    }
    await api.dispose();

    await page.goto(`/car/${carId}/bill/recurring`);
    const summary = page.getByRole('region', { name: 'Summary of the list' });
    await expect(summary).toContainText('€40.00');
    await expect(summary).toContainText('a month for 2 running costs');
    await expect(summary).toContainText(/^.*Next: Insurance, €30\.00 on/);
  });

  test('shows no summary without entries', async ({ page }) => {
    await page.goto(`/car/${carId}/bill/miscellaneous`);
    await expect(
      page.getByText('Record other expenses like parking or a car wash.'),
    ).toBeVisible();
    await expect(page.getByTestId('cost-summary')).toHaveCount(0);
  });
});
