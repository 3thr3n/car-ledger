import { expect, test } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { aliceApi, deleteAllCars } from './helpers';

const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF');
/** 1×1 PNG */
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);

test.describe('papers', () => {
  let carId: number;

  test.beforeAll(async () => {
    await deleteAllCars();
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: {
        name: 'E2E Golf GTI',
        year: 2018,
        odometer: 80000,
        licensePlate: 'M-AB 123',
      },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    const { id } = (await (
      await api.put(`/api/modification/${carId}`, {
        data: {
          part: 'Coilovers',
          manufacturer: 'KW Variant 3',
          approvalType: 'PARTS_REPORT',
          approvalNumber: 'TG 4711',
          conditions: 'Only with the rims 8x18 ET45 and tires 225/40 R18.',
          inspectionDate: '2025-05-02',
          testingBody: 'TÜV Süd',
          registered: true,
          installedDate: '2025-04-28',
          installedOdometer: 78000,
        },
      })
    ).json()) as { id: number };
    for (const [kind, name, mimeType, buffer] of [
      ['APPROVAL', 'gutachten.pdf', 'application/pdf', PDF],
      ['PHOTO', 'front.png', 'image/png', PNG],
    ] as const) {
      await api.post(`/api/attachment/${carId}/modification/${id}`, {
        multipart: { kind, file: { name, mimeType, buffer } },
      });
    }
    await api.put(`/api/modification/${carId}`, {
      data: {
        part: 'Exhaust',
        manufacturer: 'Akrapovič',
        approvalType: 'SINGLE_APPROVAL',
        installedDate: '2026-09-01',
        installedOdometer: 79500,
      },
    });
    await api.put(`/api/technical-inspection/${carId}`, {
      data: {
        date: '2024-05-06',
        odometer: 66000,
        testingBody: 'DEKRA',
        result: 'MINOR_DEFECTS',
        defects: 'Wiper blades worn',
        nextDue: '2026-05-01',
      },
    });
    await api.dispose();
  });

  test.afterAll(async () => {
    await deleteAllCars();
  });

  test('downloads the modifications and the HU as PDF', async ({ page }) => {
    await page.goto(`/car/${carId}/modifications`);
    await page.getByRole('button', { name: 'Export PDF' }).click();
    const dialog = page.getByRole('dialog', {
      name: 'Modifications and HU as PDF',
    });
    await dialog.getByRole('radio', { name: /For selling/ }).check();

    const downloading = page.waitForEvent('download');
    await dialog.getByRole('link', { name: 'PDF' }).click();
    const download = await downloading;

    expect(download.suggestedFilename()).toMatch(
      /^e2e-golf-gti-papers-\d{4}-\d{2}-\d{2}\.pdf$/,
    );
    const file = await readFile(await download.path());
    expect(file.subarray(0, 5).toString()).toBe('%PDF-');
    if (process.env.PAPERS_PDF) await download.saveAs(process.env.PAPERS_PDF);
  });

  test('is on the tab HU as well', async ({ page }) => {
    await page.goto(`/car/${carId}/inspections`);
    await page.getByRole('button', { name: 'Export PDF' }).click();
    await expect(
      page.getByRole('dialog', { name: 'Modifications and HU as PDF' }),
    ).toBeVisible();
  });
});
