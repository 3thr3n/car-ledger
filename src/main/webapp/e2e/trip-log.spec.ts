import { expect, test } from '@playwright/test';
import { aliceApi, resetAccounts } from './helpers';

test.beforeAll(resetAccounts);

test('shows the kilometers between two trips that no trip covers', async ({
  page,
}) => {
  const api = await aliceApi();
  await api.put('/api/car/my', {
    data: { name: 'E2E Log', year: 2020, odometer: 1000, tripLog: true },
  });
  const carId = (
    (await (await api.get('/api/car/my')).json()) as { id: number }[]
  )[0].id;
  const at = (hoursAgo: number) =>
    new Date(Date.now() - hoursAgo * 3600_000).toISOString();
  for (const [start, end, startedAt, endedAt, destination] of [
    [1000, 1018, at(6), at(5.5), 'Office'],
    // 42 km later than the finish of the first one
    [1060, 1078, at(3), at(2.5), 'Home'],
  ] as const) {
    const added = await api.put(
      `/api/trip/${carId}/${crypto.randomUUID()}/past`,
      {
        data: {
          type: 'COMMUTE',
          startOdometer: start,
          endOdometer: end,
          startedAt,
          endedAt,
          destination,
        },
      },
    );
    expect(added.ok()).toBeTruthy();
  }
  await api.dispose();

  await page.goto(`/car/${carId}/trips`);
  const log = page.getByTestId('trip-list');
  await expect(log.getByTestId('trip')).toHaveCount(2);
  await expect(log.getByTestId('trip-gap')).toHaveCount(1);
  await expect(log.getByTestId('trip-gap')).toHaveText('42 km not in the log');
  // the gap comes before the later trip
  await expect(
    log.getByTestId('trip').filter({ hasText: 'Home' }),
  ).toContainText('42 km not in the log');
});
