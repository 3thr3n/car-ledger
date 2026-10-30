import { readFileSync } from 'node:fs';
import { expect, Page, request, Route, test } from '@playwright/test';
import { ALICE, API_URL } from '../playwright.config';
import { aliceApi, deleteAllCars, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

/** the release of the tests of the backend, spec 1 */
const MANIFEST = JSON.parse(
  readFileSync(
    new URL('../../../test/resources/firmware/manifest.json', import.meta.url),
    'utf8',
  ),
) as { version: string; capabilities: string[] };

/**
 * The server of the dev mode has no firmware, the tests answer for it. The app
 * calls the API on another origin in dev mode, so the answer needs CORS.
 */
async function withFirmware(page: Page, manifest: object = MANIFEST) {
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
  await page.route('**/api/tracker/firmware/manifest.json', answer(manifest));
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
    const networks = dialog.getByTestId('flash-item');
    await expect(
      networks.nth(0).getByRole('textbox', { name: /^SSID/ }),
    ).toHaveAccessibleDescription('Required');
    expect(await page.evaluate(() => 'portRequested' in window)).toBe(false);

    // the form of the settings comes from the schema of the manifest
    await expect(dialog).toContainText('WLAN networks');
    await dialog.getByRole('button', { name: 'Add Network' }).click();
    await expect(networks).toHaveCount(2);
    await networks.nth(0).getByRole('textbox', { name: /^SSID/ }).fill('Home');
    await networks
      .nth(0)
      .getByLabel(/^Password/)
      .fill('short');
    await dialog.getByRole('button', { name: 'Remove Network 2' }).click();
    await expect(networks).toHaveCount(1);
    await expect(dialog).toContainText('8 to 63 bytes');

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
      networks.nth(0).getByRole('textbox', { name: /^SSID/ }),
    ).toHaveValue('Home');
  });

  test('offers only what the firmware declares', async () => {
    await withFirmware(page, { ...MANIFEST, capabilities: ['settings'] });
    await withSerialPort(page);
    await page.goto(`/car/${carId}/edit`);

    await page
      .getByTestId('tracker')
      .filter({ hasText: 'Glovebox' })
      .getByRole('button', { name: 'Set up over USB' })
      .click();
    const dialog = page.getByRole('dialog', { name: 'Set up Glovebox' });
    await expect(
      dialog.getByRole('button', { name: 'Update settings only' }),
    ).toBeVisible();
    await expect(
      dialog.getByRole('button', { name: /^Install firmware/ }),
    ).toHaveCount(0);
    await expect(
      dialog.getByRole('button', { name: 'Read settings' }),
    ).toHaveCount(0);
  });

  test('updates only the firmware without the settings', async () => {
    await withFirmware(page, {
      ...MANIFEST,
      capabilities: ['install', 'update-firmware', 'settings', 'read-settings'],
    });
    await withSerialPort(page);
    await page.goto(`/car/${carId}/edit`);

    await page
      .getByTestId('tracker')
      .filter({ hasText: 'Glovebox' })
      .getByRole('button', { name: 'Set up over USB' })
      .click();
    const dialog = page.getByRole('dialog', { name: 'Set up Glovebox' });

    // nothing entered: the settings on the tracker stay
    await dialog
      .getByRole('button', { name: 'Update firmware only v1.0.0' })
      .click();
    expect(await page.evaluate(() => 'portRequested' in window)).toBe(true);
    await expect(dialog.getByTestId('flash-error')).toContainText(
      'No connection to the tracker',
    );
    await expect(dialog).not.toContainText('Required');
  });

  test('reads the settings of the tracker', async () => {
    await withFirmware(page, {
      ...MANIFEST,
      capabilities: ['install', 'settings', 'read-settings'],
    });
    await withSerialPort(page);
    await page.goto(`/car/${carId}/edit`);

    await page
      .getByTestId('tracker')
      .filter({ hasText: 'Glovebox' })
      .getByRole('button', { name: 'Set up over USB' })
      .click();
    const dialog = page.getByRole('dialog', { name: 'Set up Glovebox' });
    // without the capability, no update of the firmware only
    await expect(
      dialog.getByRole('button', { name: /^Update firmware only/ }),
    ).toHaveCount(0);

    await dialog.getByRole('button', { name: 'Read settings' }).click();
    expect(await page.evaluate(() => 'portRequested' in window)).toBe(true);
    await expect(dialog.getByTestId('flash-error')).toContainText(
      'No connection to the tracker',
    );
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
