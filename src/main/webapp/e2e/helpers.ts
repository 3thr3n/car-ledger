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

/**
 * Every spec starts with new accounts of alice and bob, as if they logged in
 * for the first time: the accounts in the app are deleted with their cars,
 * inbox, invitations, requests and settings, so nothing leaks from an earlier
 * spec. The logins at Keycloak stay, the next request of alice creates her
 * account again with the same session; bob logs in in his own specs.
 */
export async function resetAccounts() {
  const api = await aliceApi();
  // bob first: deleting another account needs alice as administrator
  const accounts = await api.get('/api/admin/accounts?search=bob');
  expect(accounts.ok()).toBeTruthy();
  const { data } = (await accounts.json()) as {
    data: { id: number; name: string }[];
  };
  for (const account of data.filter((a) => a.name === 'bob')) {
    expect(
      (await api.delete(`/api/admin/accounts/${account.id}`)).status(),
    ).toBe(204);
  }
  expect((await api.delete('/api/user/me')).status()).toBe(204);
  expect((await api.get('/api/user/me')).ok()).toBeTruthy();
  await api.dispose();
}

/** The account can only have one car, e.g. for a second one in the same spec */
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
