import { expect, Page, test } from '@playwright/test';
import { aliceApi, day, deleteAllCars } from './helpers';

test.describe.serial('reminders', () => {
  let page: Page;
  let carId: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    const created = await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2020, odometer: 50000 },
    });
    expect(created.ok()).toBeTruthy();
    const cars = (await (await api.get('/api/car/my')).json()) as {
      id: number;
    }[];
    carId = cars[0].id;

    // overdue for three days, does not repeat
    const added = await api.put(`/api/reminder/${carId}`, {
      data: {
        title: 'Vignette',
        dueDate: day(-3),
      },
    });
    expect(added.status()).toBe(202);
    await api.dispose();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
    await deleteAllCars();
  });

  const reminder = (title: string) =>
    page.getByTestId('reminder').filter({ hasText: title });

  test('shows due reminders on the dashboard', async () => {
    await page.goto('/dashboard');

    const due = page.locator('.MuiCard-root', { hasText: 'Due soon' });
    await expect(reminder('Vignette')).toContainText('Overdue');
    await expect(reminder('Vignette')).toContainText('3 days overdue');
    // it has no due mileage
    await expect(reminder('Vignette')).not.toContainText('km');
    await expect(due.getByRole('link', { name: 'E2E Polo' })).toBeVisible();
  });

  test('adds a repeating reminder from a preset', async () => {
    await page.goto(`/car/${carId}`);
    await page.getByRole('tab', { name: 'Reminders' }).click();
    await expect(page).toHaveURL(/\/reminders$/);

    await page.getByRole('button', { name: 'Add reminder' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'Service' }).click();
    await expect(dialog.getByRole('textbox', { name: /^Title/ })).toHaveValue(
      'Service',
    );
    // one interval from the current odometer
    await expect(dialog.getByRole('textbox', { name: 'Odometer' })).toHaveValue(
      '65,000 km',
    );
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('Reminder added')).toBeVisible();
    await expect(reminder('Service')).toContainText('Upcoming');
    await expect(reminder('Service')).toContainText('at 65,000 km');
    await expect(reminder('Service')).toContainText(
      'Every 12 months or 15,000 km',
    );
    // the most urgent first
    await expect(page.getByTestId('reminder').first()).toContainText(
      'Vignette',
    );
  });

  test('moves a repeating reminder when it is done', async () => {
    await reminder('Service').getByRole('button', { name: 'Done' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Every 12 months or 15,000 km');
    await expect(dialog).toContainText('counted from when it was done');
    await dialog
      .getByRole('textbox', { name: 'Odometer when done' })
      .fill('51000');
    await dialog.getByRole('button', { name: 'Mark as done' }).click();

    await expect(page.getByText('Marked as done')).toBeVisible();
    await expect(reminder('Service')).toContainText('at 66,000 km');
  });

  test('removes a one-off reminder when it is done', async () => {
    await reminder('Vignette').getByRole('button', { name: 'Done' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('It does not repeat');
    await dialog.getByRole('button', { name: 'Mark as done' }).click();

    await expect(reminder('Vignette')).toHaveCount(0);
    await page.goto('/dashboard');
    await expect(page.getByText('Due soon', { exact: true })).toHaveCount(0);
  });

  test('edits and deletes a reminder', async () => {
    await page.goto(`/car/${carId}/reminders`);
    await reminder('Service').getByRole('button', { name: 'Edit' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('textbox', { name: /^Title/ }).fill('Big service');
    await dialog.getByRole('textbox', { name: 'Note' }).fill('Timing belt');
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('Reminder saved')).toBeVisible();
    await expect(reminder('Big service')).toContainText('Timing belt');

    await reminder('Big service')
      .getByRole('button', { name: 'Delete' })
      .click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Delete' })
      .click();
    await expect(page.getByText('No reminders yet')).toBeVisible();
  });
});
