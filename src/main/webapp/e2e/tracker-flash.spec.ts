import { expect, Page, request, Route, test } from '@playwright/test';
import { ALICE, API_URL } from '../playwright.config';
import { aliceApi, deleteAllCars } from './helpers';

const MANIFEST = {
  name: 'CarLedger GPS tracker',
  version: 'v1.0.0',
  builds: [
    {
      chipFamily: 'ESP32',
      parts: [{ path: 'firmware.bin', offset: 65536 }],
    },
  ],
  config: { offset: 4128768, size: 8192, maxLength: 8192, version: 1 },
};

/**
 * The server of the dev mode has no firmware, the tests answer for it. The app
 * calls the API on another origin in dev mode, so the answer needs CORS.
 */
async function withFirmware(page: Page) {
  const answer = (json: unknown) => (route: Route) =>
    route.fulfill({
      json,
      headers: {
        'access-control-allow-origin': route.request().headers()['origin'],
        'access-control-allow-credentials': 'true',
      },
    });
  await page.route(
    '**/api/tracker/firmware',
    answer({ version: MANIFEST.version }),
  );
  await page.route('**/api/tracker/firmware/manifest.json', answer(MANIFEST));
}

/**
 * A port that can't be opened, like one another program uses: esptool-js gets
 * as far as connecting
 */
async function withSerialPort(page: Page) {
  await page.addInitScript(() => {
    const port = {
      getInfo: () => ({ usbVendorId: 0x10c4, usbProductId: 0xea60 }),
      open: () => Promise.reject(new DOMException('Port busy', 'NetworkError')),
      close: () => Promise.resolve(),
    };
    Object.defineProperty(navigator, 'serial', {
      value: {
        requestPort: () => {
          (window as unknown as { portRequested: boolean }).portRequested =
            true;
          return Promise.resolve(port);
        },
      },
    });
  });
}

test.describe.serial('Setting up GPS trackers over USB', () => {
  let page: Page;
  let carId: number;
  let token: string;

  test.beforeAll(async () => {
    await deleteAllCars();
    const api = await aliceApi();
    expect(
      (
        await api.put('/api/car/my', {
          data: { name: 'E2E Polo', year: 2020, odometer: 50000 },
        })
      ).ok(),
    ).toBeTruthy();
    const cars = (await (await api.get('/api/car/my')).json()) as {
      id: number;
    }[];
    carId = cars[0].id;
    const created = await api.put(`/api/tracker/${carId}`, {
      data: { name: 'Glovebox' },
    });
    token = ((await created.json()) as { token: string }).token;
    await api.dispose();
  });

  test.beforeEach(async ({ browser }) => {
    page = await (await browser.newContext({ storageState: ALICE })).newPage();
  });

  test.afterEach(async () => {
    await page.context().close();
  });

  test.afterAll(async () => {
    await deleteAllCars();
  });

  const sendPoint = async (tokenOfTracker: string) => {
    const device = await request.newContext({ baseURL: API_URL });
    const response = await device.post('/api/device/points', {
      headers: { Authorization: `Bearer ${tokenOfTracker}` },
      data: [{ ts: new Date().toISOString(), lat: 51.1, lon: 10.2 }],
    });
    await device.dispose();
    return response.status();
  };

  test('is hidden without firmware on the server', async () => {
    await withSerialPort(page);
    await page.goto(`/car/${carId}/edit`);

    const section = page.getByTestId('trackers');
    await expect(section.getByTestId('tracker')).toContainText('Glovebox');
    await expect(
      section.getByRole('button', { name: 'Set up over USB' }),
    ).toHaveCount(0);
  });

  test('explains that a browser without Web Serial cannot', async () => {
    await withFirmware(page);
    await page.addInitScript(() => {
      delete (Navigator.prototype as { serial?: unknown }).serial;
    });
    await page.goto(`/car/${carId}/edit`);

    const section = page.getByTestId('trackers');
    await expect(section).toContainText('open CarLedger in Chrome or Edge');
    await expect(
      section.getByRole('button', { name: 'Set up over USB' }),
    ).toHaveCount(0);
  });

  test('flashes a new tracker with its token', async () => {
    await withFirmware(page);
    await withSerialPort(page);
    await page.goto(`/car/${carId}/edit`);

    const section = page.getByTestId('trackers');
    await section.getByRole('button', { name: 'Add tracker' }).click();
    const add = page.getByRole('dialog');
    await add.getByRole('textbox', { name: /^Name/ }).fill('Trunk');
    await add.getByRole('button', { name: 'Create' }).click();
    const created = await add.getByTestId('tracker-token').inputValue();
    await add.getByRole('button', { name: 'Set up over USB' }).click();

    const dialog = page.getByRole('dialog', { name: 'Set up Trunk' });
    await expect(dialog.getByTestId('flash-token')).toHaveValue(created);
    await expect(
      dialog.getByRole('textbox', { name: /^Server URL/ }),
    ).toHaveValue(new URL('/api/device/points', page.url()).href);
    // the token of a new tracker needs no new one
    await expect(dialog.getByRole('button', { name: 'New token' })).toHaveCount(
      0,
    );

    const install = dialog.getByRole('button', {
      name: 'Install firmware v1.0.0',
    });
    await install.click();
    await expect(dialog).toContainText('1 to 32 bytes');
    expect(await page.evaluate(() => 'portRequested' in window)).toBe(false);

    await dialog.getByRole('button', { name: 'Add network' }).click();
    const networks = dialog.getByTestId('flash-network');
    await expect(networks).toHaveCount(2);
    await networks
      .nth(0)
      .getByRole('textbox', { name: /^Network 1/ })
      .fill('Home');
    await networks
      .nth(0)
      .getByLabel(/^Password/)
      .fill('short');
    await networks
      .nth(1)
      .getByRole('button', { name: 'Remove network' })
      .click();
    await expect(networks).toHaveCount(1);
    await expect(dialog).toContainText('Empty or 8 to 63 bytes');

    await networks
      .nth(0)
      .getByLabel(/^Password/)
      .fill('secret-password');
    await install.click();

    await expect(dialog.getByTestId('flash-error')).toContainText(
      'No connection to the tracker',
    );
    expect(await page.evaluate(() => 'portRequested' in window)).toBe(true);
    // the form stays for another try
    await expect(
      networks.nth(0).getByRole('textbox', { name: /^Network 1/ }),
    ).toHaveValue('Home');
  });

  test('issues a new token for an existing tracker', async () => {
    await withFirmware(page);
    await withSerialPort(page);
    await page.goto(`/car/${carId}/edit`);
    expect(await sendPoint(token)).toBe(200);

    await page
      .getByTestId('tracker')
      .filter({ hasText: 'Glovebox' })
      .getByRole('button', { name: 'Set up over USB' })
      .click();
    const dialog = page.getByRole('dialog', { name: 'Set up Glovebox' });
    await expect(dialog.getByTestId('flash-token')).toHaveValue('');

    await dialog.getByRole('button', { name: 'New token' }).click();
    const confirm = page.getByRole('dialog', {
      name: 'Issue a new token for Glovebox?',
    });
    await confirm.getByRole('button', { name: 'New token' }).click();
    await expect(page.getByText('New token issued')).toBeVisible();

    const renewed = await dialog.getByTestId('flash-token').inputValue();
    expect(renewed).toMatch(/^cl_[\w-]{43}$/);
    expect(await sendPoint(token)).toBe(401);
    expect(await sendPoint(renewed)).toBe(200);
  });
});
