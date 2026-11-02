import { expect, test } from '@playwright/test';
import { aliceApi, day, deleteAllCars, money, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test('shows the cluster of a car with its dials and lights', async ({
  page,
}) => {
  const api = await aliceApi();
  await api.put('/api/car/my', {
    data: { name: 'E2E Octavia', year: 2019, odometer: 80000, tankSize: 50 },
  });
  const [car] = (await (await api.get('/api/car/my')).json()) as {
    id: number;
  }[];
  // two refuels: 40 l on 600 km and 30 l on 600 km, at 2 € a liter
  for (const [offset, unit] of [
    [-40, 40],
    [-10, 30],
  ]) {
    expect(
      (
        await api.put(`/api/bill/${car.id}/fuel`, {
          data: {
            date: day(offset),
            vatRate: 19,
            unit,
            pricePerUnit: 200,
            distance: 600,
          },
        })
      ).ok(),
    ).toBeTruthy();
  }
  expect(
    (
      await api.put(`/api/reminder/${car.id}`, {
        data: { title: 'Oil change', dueDate: day(5) },
      })
    ).status(),
  ).toBe(202);
  await api.dispose();

  await page.goto(`/car/${car.id}`);
  const cluster = page.getByRole('region', { name: 'Instrument cluster' });
  await expect(cluster).toContainText(money(140));
  // the average of 6.67 and 5.00 l/100km, the lowest and the highest
  await expect(
    cluster.getByRole('img', { name: /^Consumption .* on average$/ }),
  ).toBeVisible();
  await expect(cluster.getByText('Low 5.00')).toBeVisible();
  await expect(cluster.getByText('High 6.67')).toBeVisible();
  // the reading of today, the refuels before it add nothing
  await expect(
    cluster.getByRole('img', { name: 'Odometer 80,000 km' }),
  ).toBeVisible();

  // the reminder due soon is a lit warning light, it leads to the reminders
  const status = cluster.getByRole('list', { name: 'Status' });
  const reminder = status.getByRole('link', { name: /Oil change/ });
  await expect(reminder).toContainText('in 5 days');
  await reminder.click();
  await expect(page).toHaveURL(new RegExp(`/car/${car.id}/reminders$`));

  // the trip computer: the range of a full tank at the average consumption
  await page.goto(`/car/${car.id}`);
  const trip = page.locator('.MuiCard-root', {
    has: page.getByRole('heading', { name: 'Trip computer' }),
  });
  await expect(trip).toContainText('Refuels2');
  await expect(trip).toContainText('Fuel tanked70.0 l');
});

test('shows no dials for a car without entries', async ({ page }) => {
  // the account has room for one car
  await deleteAllCars();
  const api = await aliceApi();
  await api.put('/api/car/my', {
    data: { name: 'E2E Empty', year: 2020, odometer: 1000 },
  });
  const [car] = (await (await api.get('/api/car/my')).json()) as {
    id: number;
  }[];
  await api.dispose();

  await page.goto(`/car/${car.id}`);
  const cluster = page.getByRole('region', { name: 'Instrument cluster' });
  await expect(cluster.getByText('No consumption yet')).toBeVisible();
  await expect(cluster.getByText('No distance yet')).toBeVisible();
  const trip = page.locator('.MuiCard-root', {
    has: page.getByRole('heading', { name: 'Trip computer' }),
  });
  await expect(trip).toContainText('Fuel price—');
});
