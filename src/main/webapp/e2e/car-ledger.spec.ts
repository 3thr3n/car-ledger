import { expect, Page, test } from '@playwright/test';
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

  test('imports a CSV and reports the failed rows', async () => {
    await page.goto(`${carUrl}/bill/import?type=FUEL`);
    await visibleButton('Continue').click();

    const csv = [
      'date,unit,pricePerUnit,distance',
      '2026-01-10,41.5,172.9,610',
      '2026-02-07,39.2,169.9,598',
      'not-a-date,40,170,600',
    ].join('\n');
    await page.locator('input[type=file]').setInputFiles({
      name: 'refuels.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv),
    });

    await expect(page.getByText('4 columns found')).toBeVisible();
    await visibleButton('Continue').click();
    await visibleButton('Start import').click();

    await expect(page.getByText('2 entries imported')).toBeVisible();
    await expect(page.getByText('1 error', { exact: true })).toBeVisible();
    await expect(
      page.getByText("Could not parse date 'not-a-date'"),
    ).toBeVisible();
  });

  test('shows the costs on the dashboard', async () => {
    await page.goto('/dashboard');

    await expect(page.getByText('Cost breakdown')).toBeVisible();
    // 70 + 2 imported refuels (71.75 + 66.60) + 125.50 maintenance
    await expect(page.getByText(money(333.85)).first()).toBeVisible();
    await expect(page.getByText('3 entries')).toBeVisible();
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
