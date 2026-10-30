import { expect, Locator, Page, test } from '@playwright/test';
import { resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

/** picks an image of the size, drawn in the browser */
async function pickImage(input: Locator, width: number, height: number) {
  await input.evaluate(
    async (element: HTMLInputElement, [w, h]) => {
      const canvas = document.createElement('canvas');
      canvas.width = w;
      canvas.height = h;
      const context = canvas.getContext('2d')!;
      context.fillStyle = '#2563eb';
      context.fillRect(0, 0, w, h);
      const blob = await new Promise<Blob>((resolve) =>
        canvas.toBlob((b) => resolve(b!), 'image/png'),
      );
      const transfer = new DataTransfer();
      transfer.items.add(new File([blob], 'car.png', { type: 'image/png' }));
      element.files = transfer.files;
      element.dispatchEvent(new Event('change', { bubbles: true }));
    },
    [width, height],
  );
}

test.describe.serial('car picture', () => {
  let page: Page;
  let carUrl: string;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
  });

  test.afterAll(async () => {
    await page.close();
  });

  const header = () => page.getByRole('img', { name: 'E2E Fiat' }).first();

  test('saves the picture of a new car as a square', async () => {
    await page.goto('/car/new');
    await page
      .getByRole('textbox', { name: /^Name or model/ })
      .fill('E2E Fiat');
    await pickImage(page.getByTestId('car-picture-input'), 1200, 800);
    await expect(
      page.getByRole('button', { name: 'Change picture' }),
    ).toBeVisible();
    // the preview of the picked photo, before the car exists
    await expect(page.getByRole('img', { name: 'E2E Fiat' })).toHaveJSProperty(
      'naturalWidth',
      1200,
    );
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page).toHaveURL(/\/car\/\d+$/);
    carUrl = page.url();
    // cropped to the centered 800×800 square, scaled to 512
    await expect(header()).toHaveJSProperty('naturalWidth', 512);
    await expect(header()).toHaveJSProperty('naturalHeight', 512);
  });

  test('shows the picture in the garage and the sidebar', async () => {
    await page.goto('/car');
    // sidebar and garage
    await expect(page.getByRole('img', { name: 'E2E Fiat' })).toHaveCount(2);
  });

  test('replaces and removes the picture', async () => {
    await page.goto(`${carUrl}/edit`);
    const before = await header().getAttribute('src');

    await pickImage(page.getByTestId('car-picture-input'), 300, 300);
    await expect(page.getByText('Picture saved')).toBeVisible();
    await expect(header()).not.toHaveAttribute('src', before!);
    await expect(header()).toHaveJSProperty('naturalWidth', 300);

    await page.getByRole('button', { name: 'Remove' }).click();
    await expect(page.getByText('Picture removed')).toBeVisible();
    await expect(page.getByRole('img', { name: 'E2E Fiat' })).toHaveCount(0);
    await expect(
      page.getByRole('button', { name: 'Choose picture' }),
    ).toBeVisible();
  });
});
