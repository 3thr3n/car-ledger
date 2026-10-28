import { expect, Page, test } from '@playwright/test';
import { aliceApi, deleteAllCars, keycloakLogin } from './helpers';

/**
 * alice shares her car with bob: bob accepts or declines the invitation, alice
 * changes and revokes the access
 */
test.describe.serial('car access', () => {
  let alice: Page;
  let bob: Page;
  let carId: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Up', year: 2019, odometer: 30000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
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
  });

  test.afterAll(async () => {
    await alice.close();
    await bob.context().close();
    await deleteAllCars();
  });

  const section = () => alice.getByTestId('car-access');
  // the card in the garage of bob, the sidebar has the car as well
  const sharedCar = (role: string) =>
    bob.getByText(new RegExp(`^Shared · ${role} · `));
  const bobsRow = () =>
    section().getByTestId('access').filter({ hasText: 'bob' });

  test('alice invites bob as a viewer', async () => {
    await alice.goto(`/car/${carId}/edit`);
    await section().getByRole('button', { name: 'Invite' }).click();

    const dialog = alice.getByRole('dialog', { name: 'Invite to the car' });
    await dialog
      .getByRole('textbox', { name: 'User name or e-mail address' })
      .fill('Bob');
    await dialog.getByRole('combobox', { name: 'Role' }).click();
    await alice.getByRole('option', { name: 'Viewer' }).click();
    await dialog.getByRole('button', { name: 'Invite' }).click();

    await expect(alice.getByText('Invitation sent')).toBeVisible();
    await expect(bobsRow()).toContainText('Viewer');
    await expect(bobsRow()).toContainText('Waiting for the answer');
    await expect(bobsRow()).toContainText('Without an end');
  });

  test('bob sees the invitation in his notifications', async () => {
    await bob.goto('/dashboard');
    const badge = bob.getByLabel('1 unread notification');
    await expect(badge.first()).toBeVisible();

    await bob.getByRole('link', { name: /^Notifications/ }).click();
    const entry = bob.getByTestId('inbox-entry');
    await expect(entry).toHaveCount(1);
    await expect(entry).toContainText('alice shares the car "E2E Up" with you');
    await expect(entry).toHaveAttribute('data-unread', 'true');

    // opens the garage to answer, and is read
    await entry.getByRole('button').first().click();
    await expect(bob).toHaveURL(/\/car$/);
    await expect(bob.getByLabel('1 unread notification')).toHaveCount(0);
  });

  test('bob sees the car only after accepting', async () => {
    await bob.goto('/car');
    const invitation = bob.getByTestId('invitation');
    await expect(invitation).toContainText(
      'alice invites you to the car "E2E Up" as Viewer.',
    );
    await expect(bob.getByText('E2E Up', { exact: true })).toHaveCount(0);

    await invitation.getByRole('button', { name: 'Accept' }).click();

    await expect(bob.getByTestId('invitation')).toHaveCount(0);
    await expect(sharedCar('Viewer')).toBeVisible();

    await alice.reload();
    await expect(bobsRow()).toContainText('Viewer');
    await expect(bobsRow()).not.toContainText('Waiting');
  });

  test('alice makes bob a driver until a day', async () => {
    await bobsRow().getByRole('button', { name: 'Change' }).click();
    const dialog = alice.getByRole('dialog', {
      name: 'Change the access of bob',
    });
    await dialog.getByRole('combobox', { name: 'Role' }).click();
    await alice.getByRole('option', { name: 'Driver' }).click();
    // the sections of the picker one after the other
    await dialog
      .getByRole('group', { name: 'Until' })
      .getByRole('spinbutton')
      .first()
      .click();
    await alice.keyboard.type('12312099');
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(alice.getByText('Access changed')).toBeVisible();
    await expect(bobsRow()).toContainText('Driver');
    await expect(bobsRow()).toContainText('Until Dec 31, 2099');

    await bob.reload();
    await expect(sharedCar('Driver')).toBeVisible();
  });

  test('alice revokes the access', async () => {
    await bobsRow().getByRole('button', { name: 'Revoke' }).click();
    await alice
      .getByRole('dialog', { name: 'Revoke the access of bob?' })
      .getByRole('button', { name: 'Revoke' })
      .click();

    await expect(alice.getByText('Access revoked')).toBeVisible();
    await expect(bobsRow()).toHaveCount(0);

    await bob.reload();
    await expect(sharedCar('Driver')).toHaveCount(0);
  });

  test('bob declines a new invitation', async () => {
    const api = await aliceApi();
    const invited = await api.put(`/api/access/${carId}`, {
      data: { account: 'bob', role: 'EDITOR' },
    });
    expect(invited.status()).toBe(201);
    await api.dispose();

    await bob.reload();
    await bob
      .getByTestId('invitation')
      .getByRole('button', { name: 'Decline' })
      .click();
    await expect(bob.getByText('Invitation declined')).toBeVisible();
    await expect(bob.getByTestId('invitation')).toHaveCount(0);

    await alice.reload();
    await expect(bobsRow()).toContainText('Declined');
    await expect(
      bobsRow().getByRole('button', { name: 'Withdraw' }),
    ).toBeVisible();
  });
});
