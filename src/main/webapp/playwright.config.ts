import { defineConfig, devices } from '@playwright/test';

const CI = !!process.env.CI;
export const API_URL = 'http://localhost:8080';
export const ALICE = 'e2e/.auth/alice.json';

/**
 * End-to-end tests against the real app: Quarkus dev mode (dev services start
 * PostgreSQL, Keycloak and Infinispan in Docker) and the Vite dev server.
 * Running servers are reused locally.
 */
export default defineConfig({
  testDir: './e2e',
  // the specs share the accounts alice and bob, each one resets them first
  workers: 1,
  fullyParallel: false,
  retries: CI ? 1 : 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'en-US',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // e.g. a system chromium, when the playwright browsers are not installed
    launchOptions: {
      executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined,
    },
  },
  projects: [
    { name: 'login', testMatch: /login\.setup\.ts/ },
    {
      name: 'app',
      dependencies: ['login'],
      use: { ...devices['Desktop Chrome'], storageState: ALICE },
    },
  ],
  webServer: [
    {
      command:
        './mvnw -q quarkus:dev -Dquarkus.console.enabled=false -Dquarkus.test.continuous-testing=disabled',
      cwd: '../../..',
      url: `${API_URL}/q/health/live`,
      timeout: 10 * 60_000,
      reuseExistingServer: !CI,
    },
    {
      command: 'yarn dev --port 5173 --strictPort',
      url: 'http://localhost:5173',
      timeout: 60_000,
      reuseExistingServer: !CI,
    },
  ],
});
