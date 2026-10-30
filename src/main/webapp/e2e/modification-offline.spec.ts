import { expect, Page, test } from '@playwright/test';
import { aliceApi, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF');
/** 1×1 PNG */
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);

/**
 * The documents of the installed parts and the report of the HU at a roadside
 * check without a connection. The dev server has no service worker, the app opens the kept
 * copy itself.
 */
test.describe.serial('modification documents offline', () => {
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
    const { id } = (await (
      await api.put(`/api/modification/${carId}`, {
        data: { part: 'Coilovers', approvalType: 'PARTS_REPORT' },
      })
    ).json()) as { id: number };
    for (const [kind, name, mimeType, buffer] of [
      ['APPROVAL', 'gutachten.pdf', 'application/pdf', PDF],
      ['PHOTO', 'front.png', 'image/png', PNG],
    ] as const) {
      const added = await api.post(
        `/api/attachment/${carId}/modification/${id}`,
        { multipart: { kind, file: { name, mimeType, buffer } } },
      );
      expect(added.status()).toBe(201);
    }
    const hu = (await (
      await api.put(`/api/technical-inspection/${carId}`, {
        data: {
          date: '2024-05-06',
          result: 'NO_DEFECTS',
          nextDue: '2026-05-01',
        },
      })
    ).json()) as { id: number };
    const report = await api.post(
      `/api/attachment/${carId}/technical-inspection/${hu.id}`,
      {
        multipart: {
          file: { name: 'hu.png', mimeType: 'image/png', buffer: PNG },
        },
      },
    );
    expect(report.status()).toBe(201);
    await api.dispose();
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.context().setOffline(false);
    await page.close();
  });

  const panel = () => page.getByTestId('offline-documents');
  // the switch changes when the app has saved or dropped the files
  const toggle = async (on: boolean) => {
    const toggle = panel().getByRole('switch', {
      name: 'Documents available offline',
    });
    await toggle.click();
    await expect(toggle).toBeChecked({ checked: on });
  };
  const keptFiles = () =>
    page.evaluate(
      async () =>
        (await (await caches.open('modification-documents')).keys()).length,
    );

  test('keeps the documents of the installed parts and the HU', async () => {
    await page.goto(`/car/${carId}/modifications`);
    await expect(panel()).toContainText(/Keeps the documents/);

    await toggle(true);

    await expect(panel()).toContainText('3 documents saved');
    await expect(panel()).toContainText('may be out of date');
    expect(await keptFiles()).toBe(3);
  });

  test('opens a kept document without a connection', async () => {
    await page
      .getByTestId('modification')
      .getByRole('button', { name: /Details/ })
      .click();
    await expect(
      page.getByRole('link', { name: 'Open gutachten.pdf' }),
    ).toBeVisible();
    await page.context().setOffline(true);

    // headless Chromium downloads a PDF instead of showing it
    const opened = page.waitForEvent('popup');
    await page.getByRole('link', { name: 'Open front.png' }).click();
    const tab = await opened;
    await expect(tab).toHaveURL(/^blob:/);
    await tab.close();
    await page.context().setOffline(false);
  });

  test('shows the HU and opens its report without a connection', async () => {
    // the dev server serves the code of a tab only online, the build has it
    await page.getByRole('tab', { name: 'HU' }).click();
    await expect(page.getByTestId('inspection')).toBeVisible();
    await page.getByRole('tab', { name: 'Modifications' }).click();
    await page.context().setOffline(true);
    await page.getByRole('tab', { name: 'HU' }).click();
    const inspection = page.getByTestId('inspection');
    await expect(inspection).toContainText('No defects');
    await expect(panel()).toContainText('3 documents saved');

    await inspection.getByRole('button', { name: /Details/ }).click();
    const opened = page.waitForEvent('popup');
    await inspection.getByRole('link', { name: 'Open hu.png' }).click();
    const tab = await opened;
    await expect(tab).toHaveURL(/^blob:/);
    await tab.close();
    await page.context().setOffline(false);
    await page.getByRole('tab', { name: 'Modifications' }).click();
  });

  test('drops the documents of a removed part', async () => {
    await page
      .getByTestId('modification')
      .getByRole('button', { name: 'Remove part' })
      .click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Remove', exact: true })
      .click();

    // the report of the HU stays
    await expect(panel()).toContainText('1 document saved');
    expect(await keptFiles()).toBe(1);
  });

  test('keeps nothing when it is turned off', async () => {
    await toggle(false);
    await expect(panel()).toContainText(/Keeps the documents/);
  });
});
