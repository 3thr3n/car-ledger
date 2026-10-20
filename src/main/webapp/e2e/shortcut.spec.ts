import { expect, test } from '@playwright/test';
import { aliceApi, deleteAllCars } from './helpers';

/** creates the only car of alice, returns its id */
async function createCar(energyType: string, tripLog = false) {
  await deleteAllCars();
  const api = await aliceApi();
  const created = await api.put('/api/car/my', {
    data: {
      name: `E2E ${energyType}`,
      year: 2020,
      odometer: 1000,
      energyType,
      tripLog,
    },
  });
  expect(created.ok()).toBeTruthy();
  const cars = (await (await api.get('/api/car/my')).json()) as {
    id: number;
  }[];
  await api.dispose();
  return cars[0].id;
}

/**
 * The shortcut "Add refuel" of the installed app opens /refuel: the form of a
 * new entry of the last opened car
 */
test.describe.serial('shortcut "Add refuel"', () => {
  test.afterAll(deleteAllCars);

  test('opens the garage without cars', async ({ page }) => {
    await deleteAllCars();
    await page.goto('/refuel');
    await expect(page).toHaveURL(/\/car$/);
  });

  test('opens the refuel of the car', async ({ page }) => {
    const id = await createCar('PETROL');
    await page.goto(`/car/${id}`);
    await page.goto('/refuel');
    await expect(page).toHaveURL(
      new RegExp(`/car/${id}/bill/add\\?type=FUEL$`),
    );
  });

  test('opens the charging of an electric car', async ({ page }) => {
    const id = await createCar('ELECTRIC');
    await page.goto('/refuel');
    await expect(page).toHaveURL(
      new RegExp(`/car/${id}/bill/add\\?type=CHARGING$`),
    );
  });
});

/** The shortcut "Trip" opens /trip: the trips of the last opened car */
test.describe.serial('shortcut "Trip"', () => {
  test.afterAll(deleteAllCars);

  test('opens the garage without cars', async ({ page }) => {
    await deleteAllCars();
    await page.goto('/trip');
    await expect(page).toHaveURL(/\/car$/);
  });

  test('opens the garage without a car with the trip log', async ({ page }) => {
    await createCar('PETROL');
    await page.goto('/trip');
    await expect(page).toHaveURL(/\/car$/);
  });

  test('opens the trips of the car', async ({ page }) => {
    const id = await createCar('PETROL', true);
    await page.goto('/trip');
    await expect(page).toHaveURL(new RegExp(`/car/${id}/trips$`));
    await expect(
      page.getByRole('button', { name: 'Start trip' }),
    ).toBeVisible();
  });
});
