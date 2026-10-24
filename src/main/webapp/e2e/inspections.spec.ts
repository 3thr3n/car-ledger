import { expect, Page, test } from '@playwright/test';
import { aliceApi, deleteAllCars } from './helpers';

const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF');

test.describe.serial('HU', () => {
  let page: Page;
  let carId: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Golf', year: 2018, odometer: 80000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    await api.dispose();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
    await deleteAllCars();
  });

  const inspection = () => page.getByTestId('inspection').first();
  /** e.g. "October 2028", two years from today */
  const nextMonth = () => {
    const date = new Date();
    date.setDate(1);
    date.setFullYear(date.getFullYear() + 2);
    return new Intl.DateTimeFormat('en-US', {
      month: 'long',
      year: 'numeric',
    }).format(date);
  };

  test('adds an HU and moves the reminder', async () => {
    await page.goto(`/car/${carId}`);
    await page.getByRole('tab', { name: 'HU' }).click();
    await expect(page).toHaveURL(/\/inspections$/);
    await expect(page.getByText('No HU yet')).toBeVisible();

    await page.getByRole('button', { name: 'Add HU' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('textbox', { name: 'Odometer' })).toHaveValue(
      '80,000 km',
    );
    await dialog.getByRole('combobox', { name: 'Testing body' }).fill('DEKRA');
    // no defects, nothing to describe
    await expect(dialog.getByRole('textbox', { name: 'Defects' })).toBeHidden();
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('HU added')).toBeVisible();
    await expect(inspection()).toContainText('No defects');
    await expect(inspection()).toContainText('DEKRA · 80,000 km');
    await expect(inspection()).toContainText(`Next HU ${nextMonth()}`);

    await page.getByRole('tab', { name: 'Reminders' }).click();
    await expect(
      page.getByTestId('reminder').filter({ hasText: 'HU' }),
    ).toBeVisible();
  });

  test('keeps the defects and the report', async () => {
    await page.getByRole('tab', { name: 'HU' }).click();
    await inspection().getByRole('button', { name: 'Edit' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('combobox', { name: 'Result' }).click();
    await page.getByRole('option', { name: 'Minor defects' }).click();
    await dialog
      .getByRole('textbox', { name: 'Defects' })
      .fill('Wiper blades worn');
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText('HU saved')).toBeVisible();
    await expect(inspection()).toContainText('Minor defects');

    await inspection().getByRole('button', { name: 'Details' }).click();
    await expect(inspection()).toContainText('Wiper blades worn');
    await page.getByTestId('inspection-file-input').setInputFiles({
      name: 'hu.pdf',
      mimeType: 'application/pdf',
      buffer: PDF,
    });
    await expect(
      inspection().getByRole('link', { name: 'Open hu.pdf' }),
    ).toBeVisible();
    await expect(
      inspection().getByRole('button', { name: /Details/ }),
    ).toContainText('1');
  });

  test('deletes it and keeps the reminder', async () => {
    await inspection().getByRole('button', { name: 'Delete' }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Delete' })
      .click();

    await expect(page.getByText('HU deleted')).toBeVisible();
    await expect(page.getByText('No HU yet')).toBeVisible();
    await page.getByRole('tab', { name: 'Reminders' }).click();
    await expect(
      page.getByTestId('reminder').filter({ hasText: 'HU' }),
    ).toBeVisible();
  });
});
