import AxeBuilder from '@axe-core/playwright';
import { expect, Page, request, test } from '@playwright/test';
import { ALICE, API_URL } from '../playwright.config';
import { aliceApi, day, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

/**
 * WCAG 2.1 AA of the main pages (#543): axe finds what can be checked by a
 * tool, e.g. contrast, names and structure; the keyboard is tested by hand
 * below. A car with entries, trips and a GPS track, so the pages are filled.
 */
test.describe.serial('accessibility', () => {
  let carId: number;

  test.beforeAll(async () => {
    const api = await aliceApi();
    await api.put('/api/car/my', {
      data: { name: 'E2E A11y', year: 2020, odometer: 1000, tripLog: true },
    });
    carId = (
      (await (await api.get('/api/car/my')).json()) as { id: number }[]
    )[0].id;
    for (let i = 0; i < 3; i++) {
      await api.put(`/api/bill/${carId}/fuel`, {
        data: {
          date: day(-10 + i),
          vatRate: 19,
          unit: 40,
          pricePerUnit: 170,
          distance: 600,
        },
      });
    }
    await api.put(`/api/bill/${carId}/maintenance`, {
      data: { date: day(-3), vatRate: 19, total: 120, odometer: 2800 },
    });
    await api.put(`/api/reminder/${carId}`, {
      data: { title: 'Service', dueDate: day(20) },
    });
    const tracker = await api.put(`/api/tracker/${carId}`, {
      data: { name: 'ESP32' },
    });
    const { token } = (await tracker.json()) as { token: string };
    await api.dispose();

    // a drive an hour ago, 90 s and 1 km: more than the minimum of a drive
    const start = Date.now() - 3600_000;
    const device = await request.newContext({ baseURL: API_URL });
    await device.post('/api/device/points', {
      headers: { Authorization: `Bearer ${token}` },
      data: Array.from({ length: 90 }, (_, i) => ({
        ts: new Date(start + i * 1000).toISOString(),
        lat: 50 + i * 0.0001,
        lon: 10,
        speed: 40,
      })),
    });
    await device.dispose();
  });

  /** the violations of WCAG 2.1 A and AA, the map itself is a picture */
  const violations = async (page: Page) =>
    (
      await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .exclude('.maplibregl-canvas')
        .analyze()
    ).violations.map(
      (v) =>
        `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join(', ')})`,
    );

  for (const scheme of ['light', 'dark'] as const) {
    test(`the main pages in ${scheme} mode`, async ({ browser }) => {
      test.setTimeout(180_000);
      const context = await browser.newContext({
        storageState: ALICE,
        colorScheme: scheme,
        locale: 'en-US',
      });
      const page = await context.newPage();
      const pages: [string, (() => Promise<void>)?][] = [
        ['/dashboard'],
        ['/car'],
        [`/car/${carId}`],
        [`/car/${carId}/bill/fuel`],
        [`/car/${carId}/bill/add?type=FUEL`],
        [`/car/${carId}/trips`],
        [
          `/car/${carId}/trips`,
          () => page.getByRole('button', { name: 'Add past trip' }).click(),
        ],
        [`/car/${carId}/track`],
        ['/notifications'],
        ['/settings'],
        ['/admin'],
      ];
      for (const [url, open] of pages) {
        await page.goto(url);
        await page.waitForLoadState('networkidle');
        if (open) {
          await open();
          await expect(page.getByRole('dialog')).toBeVisible();
          // after the fade in of 225 ms, its colors are only final then
          await page.waitForTimeout(400);
        }
        expect(await violations(page), url).toEqual([]);
      }
      await context.close();
    });
  }

  test('every accent color in both modes', async ({ browser }) => {
    test.setTimeout(120_000);
    const api = await aliceApi();
    for (const accent of ['BLUE', 'VIOLET', 'ROSE', 'AMBER']) {
      await api.put('/api/user/me/settings', { data: { accent } });
      for (const scheme of ['light', 'dark'] as const) {
        const context = await browser.newContext({
          storageState: ALICE,
          colorScheme: scheme,
          locale: 'en-US',
        });
        const page = await context.newPage();
        await page.goto(`/car/${carId}`);
        await page.waitForLoadState('networkidle');
        expect(await violations(page), `${accent} ${scheme}`).toEqual([]);
        await context.close();
      }
    }
    await api.put('/api/user/me/settings', { data: { accent: 'TEAL' } });
    await api.dispose();
  });

  test('fits 320 px without scrolling sideways', async ({ browser }) => {
    test.setTimeout(120_000);
    // like a zoom of 400 % (WCAG 1.4.10), with the longer German texts
    const context = await browser.newContext({
      storageState: ALICE,
      viewport: { width: 320, height: 640 },
      locale: 'de-DE',
    });
    const page = await context.newPage();
    for (const url of [
      '/dashboard',
      '/car',
      `/car/${carId}`,
      `/car/${carId}/bill/fuel`,
      `/car/${carId}/bill/add?type=FUEL`,
      `/car/${carId}/trips`,
      `/car/${carId}/track`,
      '/notifications',
      '/settings',
      '/admin',
    ]) {
      await page.goto(url);
      await page.waitForLoadState('networkidle');
      const scrolled = await page.evaluate(() => {
        window.scrollTo(1000, 0);
        return window.scrollX;
      });
      expect(scrolled, url).toBe(0);
    }
    await context.close();
  });

  test('skips the menu with the first tab', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(
      page.getByRole('heading', { name: 'Dashboard' }),
    ).toBeVisible();

    await page.keyboard.press('Tab');
    const skip = page.getByRole('link', { name: 'Skip to content' });
    await expect(skip).toBeFocused();
    await expect(skip).toBeInViewport();
    await page.keyboard.press('Enter');
    await expect(page.locator('main')).toBeFocused();
  });

  test('keeps the focus in a dialog and returns it', async ({ page }) => {
    await page.goto(`/car/${carId}/trips`);
    const open = page.getByRole('button', { name: 'Add past trip' });
    await open.focus();
    await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog', { name: 'Add a past trip' });
    await expect(dialog).toBeVisible();

    for (let i = 0; i < 30; i++) {
      await page.keyboard.press('Tab');
      expect(
        await dialog.evaluate((element) =>
          element.contains(document.activeElement),
        ),
      ).toBe(true);
    }
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
    await expect(open).toBeFocused();
  });

  test('leaves the map and the drives with the keyboard', async ({ page }) => {
    await page.goto(`/car/${carId}/track`);
    await expect(page.getByTestId('track-drives')).toContainText('1 drive');

    const zoomIn = page.getByRole('button', { name: 'Zoom in' });
    await zoomIn.focus();
    // through the controls of the map and the list of the drives, to the end
    // of the page and around: never stuck
    const seen = new Set<string>();
    for (let i = 0; i < 40; i++) {
      await page.keyboard.press('Tab');
      seen.add(
        await page.evaluate(
          () =>
            document.activeElement?.getAttribute('aria-label') ??
            document.activeElement?.textContent ??
            '',
        ),
      );
    }
    expect(seen.size).toBeGreaterThan(5);
    expect([...seen].some((name) => /^Skip to content$/.test(name))).toBe(true);
  });
});
