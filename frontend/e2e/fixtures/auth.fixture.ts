import { test as base, Page } from '@playwright/test';

const API_BASE = 'http://localhost:8085';
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

/** Log in via the API and inject session into localStorage, then navigate to baseURL. */
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
    roles: data.roles,
    tenantId: data.tenantId ?? TENANT_ID,
    primaryRole: data.primaryRole,
  };

  await page.goto('/');
  await page.evaluate(
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
  loginAs: (role: keyof typeof users) => Promise<void>;
  loginAsAdmin: () => Promise<void>;
  loginAsViaUI: (role: keyof typeof users) => Promise<void>;
};

export const test = base.extend<AuthFixtures>({
  loginAs: async ({ page }, use) => {
    await use(async (role: keyof typeof users) => {
      await loginViaAPI(page, users[role]);
    });
  },

  loginAsAdmin: async ({ page }, use) => {
    await loginViaAPI(page, users.admin);
    await use(async () => {});
  },

  loginAsViaUI: async ({ page }, use) => {
    await use(async (role: keyof typeof users) => {
      await loginViaUI(page, users[role]);
    });
  },
});

export { expect } from '@playwright/test';
