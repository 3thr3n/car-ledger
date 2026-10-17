import { APIRequestContext, expect, Page, request } from '@playwright/test';
import { ALICE, API_URL } from '../playwright.config';

/** Logs in on the Keycloak page of the dev services (users alice and bob) */
export async function keycloakLogin(page: Page, user = 'alice') {
  await page.locator('#username').fill(user);
  await page.locator('#password').fill(user);
  await page.locator('#kc-login').click();
}

/** API client with the session of alice, for preparing and cleaning up data */
export async function aliceApi(): Promise<APIRequestContext> {
  return request.newContext({
    baseURL: API_URL,
    storageState: ALICE,
    extraHTTPHeaders: { 'X-Requested-With': 'JavaScript' },
  });
}

/** The account can only have one car, tests start without one */
export async function deleteAllCars() {
  const api = await aliceApi();
  const cars = await api.get('/api/car/my');
  expect(cars.ok()).toBeTruthy();
  for (const car of (await cars.json()) as { id: number }[]) {
    expect((await api.delete(`/api/car/my/${car.id}`)).status()).toBe(204);
  }
  await api.dispose();
}

/** Numbers as shown in the en-US locale */
export const money = (value: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'EUR' }).format(
    value,
  );

/**
 * Date in the local time zone, as the app uses it, `offset` days from today.
 * `toISOString` would give the date in UTC, a day off around midnight.
 */
export const day = (offset = 0) => {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};
