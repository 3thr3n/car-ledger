import { expect, Page, test } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { deleteAllCars, money } from './helpers';

/**
 * One journey through the app, the steps build on each other
 */
test.describe.serial('car ledger', () => {
  let page: Page;
  let carUrl: string;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
  });

  const field = (name: RegExp) => page.getByRole('textbox', { name });
  // the import stepper keeps the buttons of collapsing steps in the page, the
  // active step is the last one
  const visibleButton = (name: string) =>
    page
      .getByRole('button', { name, exact: true })
      .filter({ visible: true })
      .last();

  test('creates a car', async () => {
    await page.goto('/car');
    await page.getByRole('link', { name: 'Add car' }).first().click();

    await field(/^Name or model/).fill('E2E Golf');
    await field(/^Odometer/).fill('42000');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page).toHaveURL(/\/car\/\d+$/);
    carUrl = page.url();
    await expect(page.getByRole('heading', { name: 'E2E Golf' })).toBeVisible();
    await expect(page.getByText('42,000 km')).toBeVisible();
  });

  test('adds plate, VIN and purchase price to the car', async () => {
    await page.goto(`${carUrl}/edit`);

    await field(/^License plate/).fill('M-E2E 42');
    await field(/^VIN/).fill('wvw zzz1kz 6w00000i');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(
      page.getByText('17 letters and digits, without I, O and Q'),
    ).toBeVisible();

    await field(/^VIN/).fill('wvw zzz1kz 6w000001');
    await expect(page.getByText('17 of 17 characters')).toBeVisible();
    await field(/^Purchase price/).fill('18500');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page).toHaveURL(/\/car\/\d+$/);
    await expect(page.getByText(/^M-E2E 42 · Built/)).toBeVisible();
    const details = page.locator('.MuiCard-root', {
      hasText: 'Purchase price',
    });
    await expect(details).toContainText('WVWZZZ1KZ6W000001');
    await expect(details).toContainText(money(18500));
    // no entries yet, the cost of ownership is the purchase
    await expect(details).toContainText(`Purchase and costs${money(18500)}`);
  });

  test('adds a refuel and calculates the price', async () => {
    await page.goto(`${carUrl}/bill/add?type=FUEL`);

    await field(/^Total/).fill('70');
    await field(/^Liters/).fill('40');
    await expect(page.getByText('Calculated: 175.0 ct/l')).toBeVisible();
    await field(/^Distance/).fill('640');
    await expect(page.getByText('Consumption: 6.25 l/100km')).toBeVisible();

    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(page).toHaveURL(/\/bill\/fuel$/);
    const row = page.getByRole('row', { name: /175\.0 ct\/l/ });
    await expect(row).toContainText(money(70));
    await expect(row).toContainText('6.25 l/100km');
  });

  test('edits the refuel and calculates again', async () => {
    await page
      .getByRole('row', { name: /175\.0 ct\/l/ })
      .getByRole('button', { name: 'Edit' })
      .click();

    await expect(
      page.getByRole('heading', { name: 'Edit entry' }),
    ).toBeVisible();
    await expect(field(/^Total/)).toHaveValue('70 €');
    await expect(
      page.getByRole('button', { name: 'Save and add another' }),
    ).toHaveCount(0);

    await field(/^Liters/).fill('35');
    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(page).toHaveURL(/\/bill\/fuel$/);
    const row = page.getByRole('row', { name: /200\.0 ct\/l/ });
    await expect(row).toContainText(money(70));
    await expect(row).toContainText('5.47 l/100km');
    await expect(page.getByRole('row', { name: /175\.0 ct\/l/ })).toHaveCount(
      0,
    );
  });

  test('adds a maintenance with the total of labor and parts', async () => {
    await page.goto(`${carUrl}/bill/add?type=MAINTENANCE`);

    await field(/^Workshop/).fill('E2E Garage');
    await field(/^Description/).fill('Oil change');
    await field(/^Labor/).fill('80');
    await field(/^Parts/).fill('45.5');
    await expect(page.getByText(`Calculated: ${money(125.5)}`)).toBeVisible();
    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(page).toHaveURL(/\/bill\/maintenance$/);
    await expect(page.getByRole('row', { name: /Oil change/ })).toContainText(
      money(125.5),
    );
  });

  test('adds a recurring cost and ends it', async () => {
    await page.goto(`${carUrl}/bill/add?type=RECURRING`);

    await field(/^Name/).fill('E2E Insurance');
    await field(/^Amount/).fill('39.9');
    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(page).toHaveURL(/\/bill\/recurring$/);
    const row = page.getByRole('row', { name: /E2E Insurance/ });
    await expect(row).toContainText('Running');

    await row.getByRole('button', { name: 'End' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('End E2E Insurance?');
    await dialog.getByRole('button', { name: 'End' }).click();

    await expect(row).toContainText('Finished');
    await expect(row.getByRole('button', { name: 'End' })).toHaveCount(0);
  });

  const uploadCsv = (name: string, lines: string[]) =>
    page.locator('input[type=file]').setInputFiles({
      name,
      mimeType: 'text/csv',
      buffer: Buffer.from(lines.join('\n')),
    });
  const column = (field: RegExp) =>
    page.getByRole('combobox', { name: field }).filter({ visible: true });

  test('imports a CSV and reports the failed rows', async () => {
    await page.goto(`${carUrl}/bill/import?type=FUEL`);
    await visibleButton('Continue').click();

    // names the automatic assignment does not recognize
    await uploadCsv('refuels.csv', [
      'Tag,Liter,Cent,Strecke',
      '2026-01-10,41.5,172.9,610',
      '2026-02-07,39.2,169.9,598',
      'not-a-date,40,170,600',
    ]);

    await expect(page.getByText('4 columns found')).toBeVisible();
    await expect(page.getByText('Assignment of your last import')).toHaveCount(
      0,
    );
    for (const [field, name] of [
      [/^Date/, 'Tag'],
      [/^Liters/, 'Liter'],
      [/^Price per liter/, 'Cent'],
      [/^Distance/, 'Strecke'],
    ] as const) {
      await column(field).click();
      await page.getByRole('option', { name, exact: true }).click();
    }
    await visibleButton('Continue').click();
    await visibleButton('Start import').click();

    await expect(page.getByText('2 entries imported')).toBeVisible();
    await expect(page.getByText('1 error', { exact: true })).toBeVisible();
    await expect(
      page.getByText("Could not parse date 'not-a-date'"),
    ).toBeVisible();
  });

  test('remembers the column assignment of the last import', async () => {
    await page.goto(`${carUrl}/bill/import?type=FUEL`);
    await visibleButton('Continue').click();

    // same export, other column order and an additional column
    await uploadCsv('more-refuels.csv', [
      'Strecke,Notiz,Cent,Tag,Liter',
      '590,Highway,171.9,2026-03-05,38.0',
    ]);

    await expect(
      page.getByText('Assignment of your last import restored'),
    ).toBeVisible();
    await expect(column(/^Date/)).toHaveText('Tag');
    await expect(column(/^Liters/)).toHaveText('Liter');
    await expect(column(/^Price per liter/)).toHaveText('Cent');
    await expect(column(/^Distance/)).toHaveText('Strecke');
    await expect(visibleButton('Continue')).toBeEnabled();
  });

  test('exports the refuels as CSV in the format of the import', async () => {
    await page.goto(`${carUrl}/bill/fuel`);

    const download = page.waitForEvent('download');
    await page.getByRole('link', { name: 'Export CSV' }).click();
    const file = await download;

    expect(file.suggestedFilename()).toBe('e2e-golf-fuel.csv');
    const lines = (await readFile(await file.path(), 'utf8'))
      .trim()
      .split('\n');
    expect(lines[0]).toBe(
      'date,total,vatRate,unit,pricePerUnit,distance,estimateConsumption',
    );
    // the refuel added today and the two imported ones, the oldest first
    const dates = lines.slice(1).map((line) => line.split(',')[0]);
    expect(dates).toHaveLength(3);
    expect(dates.slice(0, 2)).toEqual(['2026-01-10', '2026-02-07']);
    expect([...dates].sort()).toEqual(dates);
  });

  test('shows the costs on the dashboard', async () => {
    await page.goto('/dashboard');

    await expect(page.getByText('Cost breakdown')).toBeVisible();
    // 70 + 2 imported refuels (71.75 + 66.60) + 125.50 maintenance
    await expect(page.getByText(money(333.85)).first()).toBeVisible();
    await expect(page.getByText('3 entries')).toBeVisible();

    // the imported refuels of January and February are in the monthly chart
    const chart = page.locator('.MuiCard-root', { hasText: 'Costs per month' });
    await expect(chart.locator('.MuiBarChart-element').first()).toBeVisible();
    await expect(chart.getByText('Jan 26')).toBeVisible();
    // the tooltip names the total of the month above the split by type
    await chart.locator('.MuiBarChart-element').first().hover();
    await expect(
      page.getByText(`January 2026 · Total ${money(71.75)}`),
    ).toBeVisible();
    await chart.getByRole('button', { name: 'Fuel', exact: true }).click();
    await expect(chart.getByText('Fuel price')).toBeVisible();
    await expect(chart.getByText('No refuels in this period')).toHaveCount(0);
  });

  test('deletes an entry from the ledger', async () => {
    await page.goto(carUrl);
    const row = page.getByRole('row', { name: /Maintenance/ });
    await row.getByRole('button', { name: 'Delete' }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Delete' })
      .click();

    await expect(page.getByText('Entry deleted')).toBeVisible();
    await expect(page.getByRole('row', { name: /Maintenance/ })).toHaveCount(0);
  });

  test('deletes the car with its entries', async () => {
    await page.goto(`${carUrl}/edit`);
    await page.getByRole('button', { name: 'Delete car' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Delete E2E Golf?');
    await dialog.getByRole('button', { name: 'Delete car' }).click();

    await expect(page).toHaveURL(/\/car$/);
    await expect(page.getByText('E2E Golf')).toHaveCount(0);
    await expect(page.getByText('Start with your first car')).toBeVisible();
  });
});
