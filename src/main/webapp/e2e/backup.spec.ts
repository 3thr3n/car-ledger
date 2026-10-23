import { expect, test } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { aliceApi, deleteAllCars } from './helpers';

test('downloads a backup and restores it into the empty account', async ({
  page,
}) => {
  await deleteAllCars();
  const api = await aliceApi();
  await api.put('/api/car/my', {
    data: { name: 'E2E Backup', year: 2021, odometer: 5000 },
  });
  const carId = (
    (await (await api.get('/api/car/my')).json()) as { id: number }[]
  )[0].id;
  await api.put(`/api/bill/${carId}/miscellaneous`, {
    data: {
      date: '2026-04-01',
      vatRate: 19,
      total: 12,
      description: 'Car wash',
    },
  });

  await page.goto('/settings');
  // only into an account without cars
  await expect(
    page.getByRole('button', { name: 'Restore backup' }),
  ).toBeDisabled();
  const download = page.waitForEvent('download');
  await page.getByRole('link', { name: 'Download backup' }).click();
  const file = await download;
  expect(file.suggestedFilename()).toMatch(
    /^car-ledger-backup-alice-\d{4}-\d{2}-\d{2}\.zip$/,
  );
  const zip = await readFile(await file.path());

  await deleteAllCars();
  await page.reload();
  await expect(
    page.getByRole('button', { name: 'Restore backup' }),
  ).toBeEnabled();
  await page.getByTestId('backup-input').setInputFiles({
    name: 'backup.zip',
    mimeType: 'application/zip',
    buffer: zip,
  });
  await expect(
    page.getByText('Backup restored: 1 car(s), 1 entries, 0 files'),
  ).toBeVisible();

  await page.goto('/car');
  await expect(page.getByRole('main').getByText('E2E Backup')).toBeVisible();
  await deleteAllCars();
  await api.dispose();
});

test('explains a backup larger than the server accepts', async ({ page }) => {
  await deleteAllCars();
  await page.goto('/settings');
  await expect(
    page.getByRole('button', { name: 'Restore backup' }),
  ).toBeEnabled();

  // above the limit of 12 MB, the server answers 413 before reading it
  await page.getByTestId('backup-input').setInputFiles({
    name: 'backup.zip',
    mimeType: 'application/zip',
    buffer: Buffer.alloc(13 * 1024 * 1024),
  });

  await expect(
    page.getByText('The backup is larger than the server accepts'),
  ).toBeVisible();
});
