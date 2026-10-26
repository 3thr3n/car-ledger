import { expect, Page, test } from '@playwright/test';
import { API_URL } from '../playwright.config';
import { aliceApi, deleteAllCars, keycloakLogin, money } from './helpers';

/**
 * The access of bob as a driver ends while he drives and refuels offline: his
 * refuel is refused and kept on his device, alice finishes his trip and
 * removes the ended access
 */
test.describe.serial('end of a car access', () => {
  let alice: Page;
  let bob: Page;
  let carId: number;
  let accessId: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Caddy', year: 2022, odometer: 30000, tripLog: true },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    const invited = await api.put(`/api/access/${carId}`, {
      data: { account: 'bob', role: 'DRIVER' },
    });
    accessId = ((await invited.json()) as { id: number }).id;
    await api.dispose();

    alice = await browser.newPage();
    // bob in his own session, without the one of alice
    bob = await (
      await browser.newContext({ storageState: { cookies: [], origins: [] } })
    ).newPage();
    await bob.goto('/');
    await bob.getByRole('link', { name: 'Log in to start' }).click();
    await keycloakLogin(bob, 'bob');
    await expect(bob).toHaveURL(/\/dashboard/);
    const headers = { 'X-Requested-With': 'JavaScript' };
    // a new account gets the invitation with its first request of the app
    await expect
      .poll(async () =>
        (
          (await (
            await bob.request.get(`${API_URL}/api/invitations`, { headers })
          ).json()) as { id: number }[]
        ).map((invitation) => invitation.id),
      )
      .toContain(accessId);
    expect(
      (
        await bob.request.post(
          `${API_URL}/api/invitations/${accessId}/accept`,
          { headers },
        )
      ).status(),
    ).toBe(204);
    // bob is on the road
    const started = await bob.request.put(
      `${API_URL}/api/trip/${carId}/${crypto.randomUUID()}`,
      {
        headers,
        data: {
          type: 'PRIVATE',
          startOdometer: 30000,
          startedAt: new Date(Date.now() - 3_600_000).toISOString(),
        },
      },
    );
    expect(started.status()).toBe(201);
  });

  test.afterAll(async () => {
    await alice.close();
    await bob.context().close();
    await deleteAllCars();
  });

  test('a refuel sent after the end is kept on the device', async () => {
    await bob.goto(`/car/${carId}/bill/fuel`);
    await bob.getByRole('link', { name: 'Add entry' }).first().click();
    await expect(bob.getByRole('textbox', { name: /^Total/ })).toBeVisible();
    await bob.context().setOffline(true);
    await bob.getByRole('textbox', { name: /^Total/ }).fill('60');
    await bob.getByRole('textbox', { name: /^Liters/ }).fill('40');
    await bob.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(bob.getByTestId('queued-entries')).toContainText(money(60));

    // the end of the access passes meanwhile
    const api = await aliceApi();
    const ended = await api.post(`/api/access/${carId}/${accessId}`, {
      data: {
        role: 'DRIVER',
        validUntil: new Date(Date.now() - 60_000).toISOString(),
      },
    });
    expect(ended.status()).toBe(200);
    await api.dispose();

    await bob.context().setOffline(false);
    await expect(
      bob.getByText('1 entry made offline was rejected'),
    ).toBeVisible();

    await bob.goto('/car');
    const orphaned = bob.getByTestId('orphaned-entries');
    await expect(orphaned).toContainText(money(60));
    await expect(orphaned).toContainText(
      'you no longer have access to the car',
    );
    await expect(bob.getByText('E2E Caddy')).toHaveCount(0);

    await orphaned.getByRole('button', { name: 'Discard' }).click();
    await bob
      .getByRole('dialog')
      .getByRole('button', { name: 'Discard' })
      .click();
    await expect(orphaned).toHaveCount(0);
  });

  test('alice finishes the trip of bob and removes the access', async () => {
    await alice.goto(`/car/${carId}/trips`);
    const current = alice.getByTestId('current-trip');
    await expect(current).toContainText('by bob');

    await alice.getByRole('button', { name: 'Finish trip' }).click();
    await alice
      .getByRole('textbox', { name: /Odometer at the finish/ })
      .fill('30042');
    await alice
      .getByRole('dialog')
      .getByRole('button', { name: 'Finish trip' })
      .click();
    await expect(alice.getByText('Trip finished')).toBeVisible();
    await expect(current).not.toContainText('by bob');

    await alice.goto(`/car/${carId}/edit`);
    const row = alice
      .getByTestId('car-access')
      .getByTestId('access')
      .filter({ hasText: 'bob' });
    await expect(row).toContainText('Ended');
    await row.getByRole('button', { name: 'Remove' }).click();
    await alice
      .getByRole('dialog', { name: 'Remove the ended access of bob?' })
      .getByRole('button', { name: 'Remove' })
      .click();
    await expect(row).toHaveCount(0);
  });
});
