// Logs in to CarLedger of hack/kind/up.sh through the identity provider with a browser and checks that the dashboard
// shows up, i.e. redirects, certificates, client secret and the token check of the app work together.
//
//   node hack/kind/login.cjs keycloak|authentik <user> <password>
//
// Uses playwright of the webapp; PLAYWRIGHT_CHROMIUM_EXECUTABLE for a system chromium.
const path = require('node:path');
const { chromium } = require(require.resolve('playwright', { paths: [path.join(__dirname, '../../src/main/webapp')] }));

const [idp, user, password] = process.argv.slice(2);
const app = 'https://car-ledger.localhost';

(async () => {
  const browser = await chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined });
  // the CA of the cluster is not trusted by the browser
  const page = await browser.newPage({ ignoreHTTPSErrors: true, locale: 'en-US' });
  const failed = [];
  page.on('response', (r) => r.status() >= 400 && failed.push(`${r.status()} ${r.request().method()} ${r.url()}`));
  try {
    await page.goto(app);
    await page.getByRole('link', { name: 'Log in to start' }).click();
    if (idp === 'keycloak') {
      await page.locator('#username').fill(user);
      await page.locator('#password').fill(password);
      await page.locator('#kc-login').click();
    } else {
      // the flow of authentik asks for the user and the password on two pages
      await page.locator('input[name="uidField"]').fill(user);
      await page.keyboard.press('Enter');
      await page.locator('input[name="password"]').fill(password);
      await page.keyboard.press('Enter');
      // consent of the application, if asked
      const consent = page.getByRole('button', { name: 'Continue' });
      await Promise.race([consent.click().catch(() => {}), page.waitForURL(`${app}/**`)]);
    }
    await page.waitForURL(/\/dashboard/, { timeout: 60_000 });
    await page.getByRole('heading', { name: 'Dashboard' }).waitFor();
    console.log(`logged in through ${idp}: ${page.url()}`);
  } catch (e) {
    await page.screenshot({ path: path.join(__dirname, '../../target/kind/login-failed.png') });
    console.error(`login failed at ${page.url()}, screenshot target/kind/login-failed.png`);
    failed.forEach((f) => console.error(`  ${f}`));
    throw e;
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
