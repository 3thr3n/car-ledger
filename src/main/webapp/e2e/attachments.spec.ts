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
    // the limits of the server
    await expect(page.getByText(/up to 10 MB each and 10 files/)).toBeVisible();

    await input().setInputFiles([
      { name: 'receipt.png', mimeType: 'image/png', buffer: PNG },
      { name: 'invoice.pdf', mimeType: 'application/pdf', buffer: PDF },
    ]);
    await expect(
      page.getByRole('link', { name: 'Open receipt.png' }),
    ).toBeVisible();
    // the preview of the picked file is loaded
    await expect(
      page.getByRole('link', { name: 'Open receipt.png' }).locator('img'),
    ).toHaveJSProperty('naturalWidth', 1);
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
    // the photo small instead of the paperclip
    await expect(
      row.getByLabel('2 files attached').locator('img'),
    ).toHaveJSProperty('naturalWidth', 1);
  });

  test('shows the files from the list without the form', async () => {
    await page
      .getByRole('row', { name: /Car wash/ })
      .getByRole('button', { name: '2 files attached' })
      .click();

    const dialog = page.getByRole('dialog', { name: 'Receipts and files' });
    const image = dialog.getByRole('link', { name: 'Open receipt.png' });
    await expect(image.locator('img')).toHaveJSProperty('naturalWidth', 1);
    await expect(
      dialog.getByRole('link', { name: 'Open invoice.pdf' }),
    ).toBeVisible();

    await dialog.getByRole('button', { name: 'Close' }).click();
    await expect(dialog).toBeHidden();
    await expect(page).toHaveURL(/\/bill\/miscellaneous$/);
  });

  test('shows, opens and removes the files when editing', async () => {
    await page
      .getByRole('row', { name: /Car wash/ })
      .getByRole('button', { name: 'Edit' })
      .click();

    const image = page.getByRole('link', { name: 'Open receipt.png' });
    await expect(image).toBeVisible();
    await expect(image.locator('img')).toHaveJSProperty('naturalWidth', 1);
    // the tile shows the preview, the link opens the file
    await expect(image.locator('img')).toHaveAttribute('src', /\/preview$/);
    await expect(image).toHaveAttribute('href', /\/content$/);
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

    // Chromium cannot decode it, it is sent as it is and explained
    await input().setInputFiles({
      name: 'IMG_0001.HEIC',
      mimeType: 'image/heic',
      buffer: Buffer.from('\0\0\0\u0018ftypheic\0\0\0\0mif1heic', 'latin1'),
    });
    await expect(page.getByText(/HEIC photos cannot be read/)).toBeVisible();
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
    // the tile shows the preview of the server
    await expect(photo.locator('img')).toHaveJSProperty('naturalWidth', 256);
    await expect(photo.locator('img')).toHaveJSProperty('naturalHeight', 192);

    // the file itself
    const size = await page.evaluate(
      async (url) => {
        const image = new Image();
        image.src = url;
        await image.decode();
        return [image.naturalWidth, image.naturalHeight];
      },
      (await photo.getAttribute('href'))!,
    );
    expect(size).toEqual([2000, 1500]);
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

  test('attaches the contract to a recurring cost', async () => {
    const created = await api.put(`/api/bill/${carId}/recurring`, {
      data: {
        name: 'Leasing',
        category: 'FINANCE',
        billInterval: 'MONTHLY',
        startDate: '2025-01-01',
        amount: 299,
      },
    });
    const { id } = (await created.json()) as { id: number };

    await page.goto(`/car/${carId}/bill/${id}/edit?type=RECURRING`);
    await input().setInputFiles({
      name: 'contract.pdf',
      mimeType: 'application/pdf',
      buffer: PDF,
    });
    await expect(
      page.getByRole('link', { name: 'Open contract.pdf' }),
    ).toBeVisible();

    await page.goto(`/car/${carId}/bill/recurring`);
    await page
      .getByRole('row', { name: /Leasing/ })
      .getByRole('button', { name: '1 file attached' })
      .click();
    await expect(
      page
        .getByRole('dialog', { name: 'Receipts and files' })
        .getByRole('link', { name: 'Open contract.pdf' }),
    ).toBeVisible();
  });
});
