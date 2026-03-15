import { test as base, Page } from '@playwright/test';

const API_BASE = process.env.API_BASE ?? 'http://localhost:8080';
const TENANT_ID = 'tenant1';

export interface UserCredentials {
  username: string;
  password: string;
  role: string;
}

export const users: Record<string, UserCredentials> = {
  admin:      { username: 'admin',      password: 'admin',      role: 'admin' },
  production: { username: 'production', password: 'production', role: 'floor' },
  finance:    { username: 'finance',    password: 'finance',    role: 'finance' },
  cashier:    { username: 'cashier',    password: 'cashier',    role: 'cashier' },
  manager:    { username: 'manager',    password: 'manager',    role: 'management' },
  warehouse:  { username: 'warehouse',  password: 'warehouse',  role: 'warehouse' },
  viewer:     { username: 'viewer',     password: 'viewer',     role: 'viewer' },
};

/** Log in via the API and inject session into localStorage via addInitScript.
 *  This sets auth tokens BEFORE any page JS runs, preventing race
 *  conditions with the login page's auto-redirect or AuthShell checks. */
async function loginViaAPI(page: Page, creds: UserCredentials): Promise<void> {
  const res = await page.request.post(`${API_BASE}/v1/auth/login`, {
    data: { username: creds.username, password: creds.password },
  });

  if (!res.ok()) {
    throw new Error(`Login failed for ${creds.username}: ${res.status()} ${await res.text()}`);
  }

  const data = await res.json();
  const userInfo = {
    username: data.username,
    displayName: data.displayName ?? data.username,
    roles: data.roles,
    tenantId: data.tenantId ?? TENANT_ID,
    primaryRole: data.primaryRole,
  };

  // Set the cookie so Next.js middleware (server-side) allows the request through.
  await page.context().addCookies([{
    name: 'bc_token',
    value: data.token,
    domain: 'localhost',
    path: '/',
    sameSite: 'Lax',
  }]);

  // Use addInitScript so localStorage is set before the page's React code runs.
  // This avoids race conditions where the login page detects isLoggedIn() and
  // triggers router.replace('/dashboard'), conflicting with the test's navigation.
  await page.addInitScript(
    ({ token, user }) => {
      localStorage.setItem('bc_token', token);
      localStorage.setItem('bc_user', JSON.stringify(user));
    },
    { token: data.token, user: userInfo },
  );
}

/** Log in via the login page UI. */
async function loginViaUI(page: Page, creds: UserCredentials): Promise<void> {
  await page.goto('/login');
  await page.locator('input[autocomplete="username"]').fill(creds.username);
  await page.locator('input[autocomplete="current-password"]').fill(creds.password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL('**/dashboard**', { timeout: 15_000 });
}

type AuthFixtures = {
  loginAs: (_role: keyof typeof users) => Promise<void>;
  loginAsAdmin: () => Promise<void>;
  loginAsViaUI: (_role: keyof typeof users) => Promise<void>;
};

export const test = base.extend<AuthFixtures>({
  loginAs: async ({ page }, applyFixture) => {
    await applyFixture(async (role: keyof typeof users) => {
      await loginViaAPI(page, users[role]);
    });
  },

  loginAsAdmin: async ({ page }, applyFixture) => {
    await loginViaAPI(page, users.admin);
    await applyFixture(async () => {});
  },

  loginAsViaUI: async ({ page }, applyFixture) => {
    await applyFixture(async (role: keyof typeof users) => {
      await loginViaUI(page, users[role]);
    });
  },
});

export { expect } from '@playwright/test';
