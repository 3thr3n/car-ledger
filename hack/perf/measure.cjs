// Measures the paths of the GPS tracks that grow with the points, against the dev mode (./mvnw quarkus:dev and yarn dev
// in src/main/webapp): a car of alice gets a year of points (seed.sql), then the map, the suggested trips, the backup and
// the deletions are timed. Each transaction of the app has 60 s (the default of Quarkus).
//
//   node hack/perf/measure.cjs [days] [always-on days]     (365 and 7 by default)
//
// The restore is an upload, limited by quarkus.http.limits.max-body-size (12M); to measure a larger backup, start the dev
// mode with e.g. -Dquarkus.http.limits.max-body-size=200M.
//
// DELETES ALL CARS OF ALICE in the dev database. Uses playwright of the webapp for the login (PLAYWRIGHT_CHROMIUM_EXECUTABLE
// for a system chromium) and psql of the postgres image with docker, the database of the dev services on port 5678.
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { chromium } = require(require.resolve('playwright', { paths: [path.join(__dirname, '../../src/main/webapp')] }));

const [days = '365', alwaysOnDays = '7'] = process.argv.slice(2);
const UI = 'http://localhost:5173';
const API = 'http://localhost:8080';
const TIMEOUT_MS = 60_000;
const results = [];

/** psql of the postgres image against the database of the dev services */
function psql(args, input) {
  return execFileSync(
    'docker',
    ['run', '--rm', '-i', '--network', 'host', '-e', 'PGPASSWORD=quarkus', 'postgres:18',
      'psql', '-h', 'localhost', '-p', '5678', '-U', 'quarkus', '-d', 'quarkus', '-X', '-q', ...args],
    { input, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 },
  );
}

async function timed(name, action, note = (r) => '') {
  const start = performance.now();
  let result;
  let error;
  try {
    result = await action();
  } catch (e) {
    error = e;
  }
  const ms = performance.now() - start;
  const status = error ? `failed: ${error.message.split('\n')[0]}` : note(result);
  results.push({ name, ms, status });
  console.log(`${name.padEnd(44)} ${(ms / 1000).toFixed(2).padStart(8)} s  ${status}`);
  return result;
}

/** throws for a status that isn't ok, with the start of the body */
async function ok(response) {
  if (!response.ok()) {
    throw new Error(`${response.status()} ${(await response.text()).slice(0, 200)}`);
  }
  return response;
}

(async () => {
  const browser = await chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined });
  const page = await browser.newPage({ locale: 'en-US' });
  try {
    await page.goto(UI);
    await page.getByRole('link', { name: 'Log in to start' }).click();
    await page.locator('#username').fill('alice');
    await page.locator('#password').fill('alice');
    await page.locator('#kc-login').click();
    await page.waitForURL(/\/dashboard/);
    const api = page.request;
    const headers = { 'X-Requested-With': 'JavaScript' };
    const get = (url) => api.get(`${API}${url}`, { headers, timeout: 10 * 60_000 });

    for (const car of await (await ok(await get('/api/car/my'))).json()) {
      await ok(await api.delete(`${API}/api/car/my/${car.id}`, { headers, timeout: 10 * 60_000 }));
    }
    await ok(await api.put(`${API}/api/car/my`, {
      headers, data: { name: 'Perf Golf', year: 2020, odometer: 50000, tripLog: true },
    }));
    const carId = (await (await ok(await get('/api/car/my'))).json())[0].id;
    const tracker = await (await ok(await api.put(`${API}/api/tracker/${carId}`, {
      headers, data: { name: 'ESP32' },
    }))).json();

    console.log(`seeding ${days} days (${alwaysOnDays} of them sending all day) for car ${carId} …`);
    const seedStart = performance.now();
    const seeded = psql(['-v', `car=${carId}`, '-v', `device=${tracker.device.id}`, '-v', `days=${days}`,
      '-v', `always_on_days=${alwaysOnDays}`, '-f', '-'], require('node:fs').readFileSync(path.join(__dirname, 'seed.sql')));
    console.log(`${seeded.trim()}\nseeded in ${((performance.now() - seedStart) / 1000).toFixed(1)} s\n`);

    const day = 24 * 3600_000;
    // the days of seed.sql start at midnight UTC, the time zone of the database
    const midnight = new Date();
    midnight.setUTCHours(0, 0, 0, 0);
    const range = (from, to) => `from=${encodeURIComponent(new Date(from).toISOString())}`
      + `&to=${encodeURIComponent(new Date(to).toISOString())}`;
    const track = (n) => timed(`map, ${n} day(s)`, async () => (await ok(await get(
      `/api/track/${carId}?${range(midnight.getTime() - n * day, midnight.getTime())}`))).json(),
    (t) => `${t.points} points, ${t.segments.length} segments, ${t.segments.flat().length} shown`);

    for (let i = 0; i < 2; i++) {
      // the second run with warm caches
      await track(1);
      await track(7);
      await track(31);
      await timed('suggested trips', async () => (await ok(await get(`/api/trip/${carId}/suggestions`))).json(),
        (s) => `${s.length} drives`);
    }

    const backup = await timed('backup download', async () => (await ok(await get('/api/user/me/backup'))).body(),
      (b) => `${(b.length / 1024 / 1024).toFixed(1)} MB`);

    await timed('delete the track of a day', async () => (await ok(await api.delete(
      `${API}/api/track/${carId}?${range(midnight.getTime() - 30 * day, midnight.getTime() - 29 * day)}`,
      { headers, timeout: 10 * 60_000 }))).json(), (r) => `${r.points} points`);

    // the cleanup has no endpoint: its statement, rolled back
    await timed('cleanup after "forever" → 30 days (SQL)', async () => psql(['-c',
      `BEGIN; EXPLAIN (ANALYZE, COSTS OFF, TIMING OFF) DELETE FROM track_point WHERE tk_car_id = ${carId} `
      + `AND tk_ts < now() - interval '30 days'; ROLLBACK;`]), (out) => out.match(/Execution Time: [\d.]+ ms/)?.[0] ?? '');

    await timed('delete the car (cascade of the points)', async () => ok(await api.delete(
      `${API}/api/car/my/${carId}`, { headers, timeout: 10 * 60_000 })));

    if (backup) {
      const restored = await timed('restore the backup', async () => (await ok(await api.post(`${API}/api/user/me/backup`, {
        headers, multipart: { file: { name: 'backup.zip', mimeType: 'application/zip', buffer: backup } },
        timeout: 10 * 60_000,
      }))).json(), (s) => `${s.trackPoints} points`);
      if (restored) {
        const restoredId = (await (await ok(await get('/api/car/my'))).json())[0].id;
        await timed('delete all tracks', async () => (await ok(await api.delete(`${API}/api/track/${restoredId}`,
          { headers, timeout: 10 * 60_000 }))).json(), (r) => `${r.points} points`);
        await ok(await api.delete(`${API}/api/car/my/${restoredId}`, { headers, timeout: 10 * 60_000 }));
      }
    }
  } finally {
    await browser.close();
  }

  const slow = results.filter((r) => r.ms > TIMEOUT_MS / 2 || r.status.startsWith('failed'));
  console.log(slow.length
    ? `\n${slow.length} above half of the transaction timeout of ${TIMEOUT_MS / 1000} s, or failed`
    : `\nall below half of the transaction timeout of ${TIMEOUT_MS / 1000} s`);
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
