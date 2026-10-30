import { expect, Page, test } from '@playwright/test';
import { aliceApi, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF');

test.describe.serial('modifications', () => {
  let page: Page;
  let carId: number;

  test.beforeAll(async ({ browser }) => {
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Golf', year: 2018, odometer: 80000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    // the part and the workshop
    await api.put(`/api/bill/${carId}/miscellaneous`, {
      data: {
        date: '2025-04-20',
        vatRate: 19,
        total: 1450,
        description: 'KW coilovers',
      },
    });
    await api.put(`/api/bill/${carId}/maintenance`, {
      data: {
        date: '2025-04-28',
        vatRate: 19,
        total: 320,
        odometer: 80000,
        description: 'Mounting',
      },
    });
    await api.dispose();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
  });

  const modification = (part: string) =>
    page.getByTestId('modification').filter({ hasText: part });

  /** types a date into a picker, its sections one after the other */
  async function fillDate(name: string, date: string) {
    await page
      .getByRole('dialog')
      .getByRole('group', { name })
      .getByRole('spinbutton')
      .first()
      .click();
    const [year, month, day] = date.split('-');
    await page.keyboard.type(`${month}${day}${year}`);
  }

  test('adds coilovers with a Teilegutachten and an open Abnahme', async () => {
    await page.goto(`/car/${carId}`);
    await page.getByRole('tab', { name: 'Modifications' }).click();
    await expect(page).toHaveURL(/\/modifications$/);
    await expect(page.getByText('No modifications yet')).toBeVisible();
    await expect(page.getByText(/originals of the ABE/)).toBeVisible();

    await page.getByRole('button', { name: 'Add modification' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('textbox', { name: /^Part/ }).fill('Coilovers');
    await dialog.getByRole('textbox', { name: 'Manufacturer' }).fill('KW');
    // an ABE needs no Abnahme
    await dialog.getByRole('combobox', { name: 'Approval' }).click();
    await page.getByRole('option', { name: 'ABE' }).click();
    await expect(dialog.getByText('Date of the Abnahme')).toBeHidden();
    await dialog.getByRole('combobox', { name: 'Approval' }).click();
    await page.getByRole('option', { name: 'Teilegutachten' }).click();
    await expect(
      dialog.getByRole('group', { name: 'Date of the Abnahme' }),
    ).toBeVisible();
    await dialog
      .getByRole('textbox', { name: 'Approval number' })
      .fill('TG 4711');
    await dialog
      .getByRole('textbox', { name: 'Conditions' })
      .fill('Only with 225/40 R18');
    // the current odometer of the car
    await expect(dialog.getByRole('textbox', { name: 'Odometer' })).toHaveValue(
      '80,000 km',
    );
    for (const bill of ['KW coilovers', 'Mounting']) {
      await dialog.getByRole('combobox', { name: 'Bills' }).click();
      await page.getByRole('option', { name: new RegExp(bill) }).click();
    }
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('Modification added')).toBeVisible();
    await expect(modification('Coilovers')).toContainText(
      'Teilegutachten · TG 4711',
    );
    await expect(modification('Coilovers')).toContainText('Abnahme open');
    await expect(modification('Coilovers')).toContainText('80,000 km');

    // what is open shows on the overview of the car
    await page.getByRole('tab', { name: 'Overview' }).click();
    const summary = page.getByTestId('papers-summary');
    await expect(summary).toContainText('1 installed modification');
    await expect(summary).toContainText('1 Abnahme open');
    await expect(summary).toContainText('No next HU');
    await page.getByRole('tab', { name: 'Modifications' }).click();
  });

  test('enters the Abnahme and the registration', async () => {
    await modification('Coilovers')
      .getByRole('button', { name: 'Edit' })
      .click();
    const dialog = page.getByRole('dialog');
    await fillDate('Date of the Abnahme', '2025-05-02');
    await dialog.getByRole('combobox', { name: 'Testing body' }).fill('TÜV');
    await dialog.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText('Modification saved')).toBeVisible();
    await expect(modification('Coilovers')).not.toContainText('Abnahme open');
    await expect(modification('Coilovers')).toContainText(
      'Not in the registration yet',
    );

    await modification('Coilovers')
      .getByRole('button', { name: 'Edit' })
      .click();
    await dialog
      .getByRole('checkbox', { name: /Entered in the registration/ })
      .check();
    await dialog.getByRole('button', { name: 'Save' }).click();
    await expect(modification('Coilovers')).not.toContainText(
      'Not in the registration yet',
    );
  });

  test('keeps the Gutachten in the details', async () => {
    await modification('Coilovers')
      .getByRole('button', { name: 'Details' })
      .click();
    const details = modification('Coilovers');
    await expect(details).toContainText('Only with 225/40 R18');
    await expect(details).toContainText('May 2, 2025 · TÜV');
    await expect(details).toContainText(
      'Apr 20, 2025 · KW coilovers · €1,450.00',
    );
    await expect(details).toContainText('Apr 28, 2025 · Mounting · €320.00');
    await expect(details).toContainText('Total €1,770.00');

    await details.getByRole('button', { name: 'Add document' }).click();
    await page.getByRole('menuitem', { name: 'Gutachten / ABE' }).click();
    await page.getByTestId('document-input').setInputFiles({
      name: 'gutachten.pdf',
      mimeType: 'application/pdf',
      buffer: PDF,
    });

    const documents = details.getByTestId('modification-documents');
    await expect(
      documents.getByRole('link', { name: 'Open gutachten.pdf' }),
    ).toBeVisible();
    await expect(documents).toContainText('Gutachten / ABE');
    await expect(
      details.getByRole('button', { name: /Details/ }),
    ).toContainText('1');
  });

  test('moves a removed part into the history', async () => {
    await modification('Coilovers')
      .getByRole('button', { name: 'Remove part' })
      .click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Remove Coilovers?');
    await dialog.getByRole('textbox', { name: 'Odometer' }).fill('92000');
    await dialog.getByRole('button', { name: 'Remove', exact: true }).click();

    await expect(page.getByText('Part moved to the history')).toBeVisible();
    await expect(page.getByText('No modifications yet')).toBeVisible();

    await page.getByRole('button', { name: 'Removed' }).click();
    await expect(modification('Coilovers')).toContainText('Removed');
    await expect(modification('Coilovers')).toContainText('92,000 km');
    await expect(
      modification('Coilovers').getByRole('button', { name: 'Remove part' }),
    ).toBeHidden();
  });

  test('deletes it from the history', async () => {
    await modification('Coilovers')
      .getByRole('button', { name: 'Delete' })
      .click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /Delete/ })
      .click();

    await expect(page.getByText('Modification deleted')).toBeVisible();
    await expect(page.getByText('No removed parts')).toBeVisible();
  });
});
