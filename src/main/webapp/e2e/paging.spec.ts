import { APIRequestContext, expect, test } from '@playwright/test';
import { aliceApi, day, deleteAllCars } from './helpers';

test.describe.serial('paging', () => {
  let api: APIRequestContext;
  let carId: number;

  test.beforeAll(async () => {
    await deleteAllCars();
    api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2021, odometer: 10000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    // 25 refuels, one per day: two pages of 20
    for (let i = 0; i < 25; i++) {
      expect(
        (
          await api.put(`/api/bill/${carId}/fuel`, {
            data: {
              date: day(-i),
              vatRate: 19,
              unit: 30,
              pricePerUnit: 170,
              distance: 400,
            },
          })
        ).ok(),
      ).toBeTruthy();
    }
  });

  test.afterAll(async () => {
    await api.dispose();
    await deleteAllCars();
  });

  test('stays on the next page of the refuels', async ({ page }) => {
    await page.goto(`/car/${carId}/bill/fuel`);
    await expect(page.getByText('1–20 of 25')).toBeVisible();

    await page.getByRole('button', { name: 'Go to next page' }).click();

    await expect(page.getByText('21–25 of 25')).toBeVisible();
    // the rows of the second page are loaded and the table does not jump back
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('21–25 of 25')).toBeVisible();
    await expect(page.getByRole('row')).toHaveCount(6);
  });
});
