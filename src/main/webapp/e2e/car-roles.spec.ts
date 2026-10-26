import { expect, Page, test } from '@playwright/test';
import { API_URL } from '../playwright.config';
import { aliceApi, deleteAllCars, keycloakLogin } from './helpers';

/**
 * What bob sees of the car of alice by his role: a viewer everything without
 * the actions, an editor the entries without the car, a driver only the own
 * refuels and trips
 */
test.describe.serial('car roles', () => {
  let bob: Page;
  let carId: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2021, odometer: 12000, tripLog: true },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    await api.put(`/api/bill/${carId}/miscellaneous`, {
      data: {
        date: '2026-04-01',
        vatRate: 19,
        total: 12,
        description: 'Car wash',
      },
    });
    await api.dispose();

    // bob in his own session, without the one of alice
    bob = await (
      await browser.newContext({ storageState: { cookies: [], origins: [] } })
    ).newPage();
    await bob.goto('/');
    await bob.getByRole('link', { name: 'Log in to start' }).click();
    await keycloakLogin(bob, 'bob');
    await expect(bob).toHaveURL(/\/dashboard/);
  });

  test.afterAll(async () => {
    await bob.context().close();
    await deleteAllCars();
  });

  /** alice gives bob the role, bob accepts it */
  async function share(role: 'VIEWER' | 'EDITOR' | 'DRIVER') {
    const api = await aliceApi();
    const accesses = (await (await api.get(`/api/access/${carId}`)).json()) as {
      id: number;
    }[];
    for (const access of accesses) {
      await api.delete(`/api/access/${carId}/${access.id}`);
    }
    const invited = await api.put(`/api/access/${carId}`, {
      data: { account: 'bob', role },
    });
    expect(invited.status()).toBe(201);
    const { id } = (await invited.json()) as { id: number };
    await api.dispose();

    const accepted = await bob.request.post(
      `${API_URL}/api/invitations/${id}/accept`,
      { headers: { 'X-Requested-With': 'JavaScript' } },
    );
    expect(accepted.status()).toBe(204);
  }

  const tab = (name: string) => bob.getByRole('tab', { name, exact: true });
  // exact, e.g. "Edit" is part of the role "Editor" in the sidebar
  const button = (name: string) =>
    bob.getByRole('button', { name, exact: true });
  const link = (name: string) => bob.getByRole('link', { name, exact: true });

  test('a viewer sees everything and changes nothing', async () => {
    await share('VIEWER');
    await bob.goto(`/car/${carId}`);

    await expect(bob.getByText(/Shared · Viewer · /)).toBeVisible();
    for (const name of ['Overview', 'Miscellaneous', 'Trips', 'Reminders']) {
      await expect(tab(name)).toBeVisible();
    }
    await expect(link('Add entry')).toHaveCount(0);
    await expect(link('Import CSV')).toHaveCount(0);
    await expect(link('Edit')).toHaveCount(0);

    await tab('Miscellaneous').click();
    await expect(bob.getByText('Car wash')).toBeVisible();
    await expect(button('Delete')).toHaveCount(0);

    await tab('Trips').click();
    await expect(bob.getByText('No trip is open')).toBeVisible();
    await expect(button('Start trip')).toHaveCount(0);
    await expect(button('Add past trip')).toHaveCount(0);

    await tab('Reminders').click();
    await expect(button('Add reminder')).toHaveCount(0);

    // the link of a form leads back to the car
    await bob.goto(`/car/${carId}/bill/add`);
    await expect(bob).toHaveURL(new RegExp(`/car/${carId}$`));
  });

  test('an editor changes the entries, not the car', async () => {
    await share('EDITOR');
    await bob.goto(`/car/${carId}`);

    await expect(bob.getByText(/Shared · Editor · /)).toBeVisible();
    await expect(link('Add entry')).toBeVisible();
    await expect(link('Import CSV')).toBeVisible();
    await expect(link('Edit')).toHaveCount(0);

    await tab('Reminders').click();
    await expect(button('Add reminder')).toBeVisible();

    await bob.goto(`/car/${carId}/edit`);
    await expect(bob).toHaveURL(new RegExp(`/car/${carId}$`));
  });

  test('a driver gets the own refuels and trips only', async () => {
    await share('DRIVER');
    await bob.goto(`/car/${carId}`);

    // no overview, the first own list instead
    await expect(bob).toHaveURL(new RegExp(`/car/${carId}/bill/fuel$`));
    await expect(bob.getByText(/Shared · Driver · /)).toBeVisible();
    await expect(tab('Fuel')).toBeVisible();
    await expect(tab('Trips')).toBeVisible();
    for (const name of ['Overview', 'Miscellaneous', 'Reminders', 'HU']) {
      await expect(tab(name)).toHaveCount(0);
    }
    // in the header and in the empty list
    await expect(link('Add entry').first()).toBeVisible();
    await expect(link('Import CSV')).toHaveCount(0);

    await tab('Trips').click();
    await expect(button('Start trip')).toBeVisible();
    await expect(button('Export')).toHaveCount(0);

    // only refuels and charges to add
    await link('Add entry').first().click();
    await expect(bob.getByText('Fuel', { exact: true }).first()).toBeVisible();
    await expect(bob.getByText('Maintenance', { exact: true })).toHaveCount(0);

    await bob.goto(`/car/${carId}/reminders`);
    await expect(bob).toHaveURL(new RegExp(`/car/${carId}/bill/fuel$`));
  });
});
