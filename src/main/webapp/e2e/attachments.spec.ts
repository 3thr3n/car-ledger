import { APIRequestContext, expect, Page, test } from '@playwright/test';
import { aliceApi, deleteAllCars } from './helpers';
import { API_URL } from '../playwright.config';

/** 1×1 PNG */
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);
const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF');

test.describe.serial('attachments', () => {
  let api: APIRequestContext;
  let page: Page;
  let carId: number;

  test.beforeAll(async ({ browser }) => {
    await deleteAllCars();
    api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E Polo', year: 2020, odometer: 1000 },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
    await api.dispose();
    await deleteAllCars();
  });

  const input = () => page.getByTestId('attachment-input');

  test('attaches the files of a new entry after saving it', async () => {
    await page.goto(`/car/${carId}/bill/add?type=MISCELLANEOUS`);
    await page.getByRole('textbox', { name: /^Description/ }).fill('Car wash');
    await page.getByRole('textbox', { name: /^Total/ }).fill('12');

    await input().setInputFiles([
      { name: 'receipt.png', mimeType: 'image/png', buffer: PNG },
      { name: 'invoice.pdf', mimeType: 'application/pdf', buffer: PDF },
    ]);
    await expect(
      page.getByRole('link', { name: 'Open receipt.png' }),
    ).toBeVisible();
    // picked by mistake and removed again before saving
    await input().setInputFiles({
      name: 'wrong.png',
      mimeType: 'image/png',
      buffer: PNG,
    });
    await page.getByRole('button', { name: 'Remove wrong.png' }).click();
    await page.getByRole('button', { name: 'Save', exact: true }).click();

    await expect(page).toHaveURL(/\/bill\/miscellaneous$/);
    const row = page.getByRole('row', { name: /Car wash/ });
    await expect(row.getByLabel('2 files attached')).toBeVisible();
  });

  test('shows, opens and removes the files when editing', async () => {
    await page
      .getByRole('row', { name: /Car wash/ })
      .getByRole('button', { name: 'Edit' })
      .click();

    const image = page.getByRole('link', { name: 'Open receipt.png' });
    await expect(image).toBeVisible();
    await expect(image.locator('img')).toHaveJSProperty('naturalWidth', 1);
    const href = await page
      .getByRole('link', { name: 'Open invoice.pdf' })
      .getAttribute('href');
    const pdf = await api.get(href!.replace(/^.*\/api\//, `${API_URL}/api/`));
    expect(pdf.headers()['content-type']).toBe('application/pdf');

    await page.getByRole('button', { name: 'Remove invoice.pdf' }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Delete' })
      .click();
    await expect(page.getByText('File removed')).toBeVisible();
    await expect(
      page.getByRole('link', { name: 'Open invoice.pdf' }),
    ).toHaveCount(0);
  });

  test('rejects other file types', async () => {
    await input().setInputFiles({
      name: 'notes.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('just text'),
    });
    await expect(
      page.getByText('Only JPEG, PNG, WebP and PDF files can be attached!'),
    ).toBeVisible();
    await expect(page.getByRole('link', { name: /^Open / })).toHaveCount(1);
  });

  test('scales large photos down to JPEG', async () => {
    // a 4000×3000 PNG of noise, too large to be kept as it is
    await input().evaluate(async (element: HTMLInputElement) => {
      const canvas = document.createElement('canvas');
      canvas.width = 4000;
      canvas.height = 3000;
      const context = canvas.getContext('2d')!;
      const pixels = context.createImageData(4000, 3000);
      for (let i = 0; i < pixels.data.length; i++) {
        pixels.data[i] = i % 4 === 3 ? 255 : Math.random() * 256;
      }
      context.putImageData(pixels, 0, 0);
      const blob = await new Promise<Blob>((resolve) =>
        canvas.toBlob((b) => resolve(b!), 'image/png'),
      );
      const transfer = new DataTransfer();
      transfer.items.add(new File([blob], 'photo.png', { type: 'image/png' }));
      element.files = transfer.files;
      element.dispatchEvent(new Event('change', { bubbles: true }));
    });

    const photo = page.getByRole('link', { name: 'Open photo.jpg' });
    await expect(photo).toBeVisible();
    await expect(photo.locator('img')).toHaveJSProperty('naturalWidth', 2000);
    await expect(photo.locator('img')).toHaveJSProperty('naturalHeight', 1500);
  });

  test('removes the files with the entry', async () => {
    const bills = await (
      await api.get(`/api/bill/${carId}/miscellaneous/all`)
    ).json();
    const billId = bills.data[0].id;
    expect((await api.delete(`/api/bill/${carId}/${billId}`)).status()).toBe(
      204,
    );
    const attachments = await api.get(
      `/api/attachment/${carId}/bill/${billId}`,
    );
    expect(attachments.status()).toBe(404);
  });
});
