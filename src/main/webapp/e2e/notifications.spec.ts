import { expect, Page, test } from '@playwright/test';
import { aliceApi, deleteAllCars, keycloakLogin } from './helpers';

/**
 * alice invites bob to a car: the invitation waits in his notifications, also
 * without e-mail and push
 */
test.describe.serial('Notifications', () => {
  let bob: Page;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    bob = await (
      await browser.newContext({ storageState: undefined })
    ).newPage();
    await bob.goto('/');
    await bob.getByRole('link', { name: 'Log in to start' }).click();
    await keycloakLogin(bob, 'bob');
    await expect(bob).toHaveURL(/\/dashboard/);

    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Inbox', year: 2022, odometer: 10 },
    });
    const carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    const invited = await api.put(`/api/access/${carId}`, {
      data: { account: 'bob', role: 'VIEWER' },
    });
    expect(invited.status()).toBe(201);
    await api.dispose();
  });

  test.afterAll(async () => {
    await bob.context().close();
    await deleteAllCars();
  });

  const entry = () =>
    bob.getByTestId('inbox-entry').filter({ hasText: 'E2E Inbox' });

  test('lists the invitation as unread', async () => {
    await bob.goto('/notifications');
    await expect(entry()).toHaveCount(1);
    await expect(entry()).toHaveAttribute('data-unread', 'true');
    await expect(entry()).toContainText(
      'As a viewer. Accept or decline the invitation.',
    );
  });

  test('marks all as read', async () => {
    await bob.getByRole('button', { name: 'Mark all as read' }).click();

    await expect(entry()).toHaveAttribute('data-unread', 'false');
    await expect(
      bob.getByRole('button', { name: 'Mark all as read' }),
    ).toBeDisabled();
    await expect(bob.getByLabel(/unread notification/)).toHaveCount(0);
  });

  test('deletes a notification', async () => {
    await entry()
      .getByRole('button', { name: /^Delete "/ })
      .click();

    await expect(bob.getByText('Notification deleted')).toBeVisible();
    await expect(entry()).toHaveCount(0);
  });
});
