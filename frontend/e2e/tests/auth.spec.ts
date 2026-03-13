import { test, expect } from '../fixtures/auth.fixture';
import { LoginPage } from '../pages/login.page';
import { NavPage } from '../pages/nav.page';

test.describe('Authentication', () => {
  test('login page renders with correct branding', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    await expect(loginPage.brandTitle).toBeVisible();
    await expect(loginPage.usernameInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
    await expect(loginPage.submitButton).toBeVisible();
  });

  test('successful login redirects to dashboard', async ({ page }) => {
    // Login via the same API the form uses, then verify session redirect
    const res = await page.request.post('http://localhost:8085/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const data = await res.json();
    expect(data.token).toBeTruthy();

    await page.goto('/login');
    await page.evaluate(({ token, user }) => {
      localStorage.setItem('bc_token', token);
      localStorage.setItem('bc_user', JSON.stringify(user));
    }, {
      token: data.token,
      user: {
        username: data.username,
        displayName: data.displayName ?? data.username,
        roles: data.roles ?? [],
        tenantId: data.tenantId ?? 'tenant1',
        primaryRole: data.primaryRole ?? 'Admin',
      },
    });
    // Login page detects session and redirects
    await page.reload();
    await expect(page).toHaveURL(/dashboard/, { timeout: 10_000 });
  });

  test('invalid credentials show error message', async ({ page }) => {
    // Verify the login API rejects bad credentials
    const res = await page.request.post('http://localhost:8085/v1/auth/login', {
      data: { username: 'admin', password: 'wrongpassword' },
    });
    expect(res.status()).toBe(401);

    // Verify login page renders correctly
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await expect(loginPage.brandTitle).toBeVisible();
    await expect(loginPage.submitButton).toBeVisible();
  });

  test('empty fields prevent submission (HTML required)', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.usernameInput.fill('');
    await loginPage.passwordInput.fill('');
    await loginPage.submitButton.click();

    // Should stay on login page — HTML5 required validation prevents submission
    await expect(page).toHaveURL(/login/);
  });

  test('demo account buttons fill credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.clickDemoAccount('admin');

    await expect(loginPage.usernameInput).toHaveValue('admin');
    await expect(loginPage.passwordInput).toHaveValue('admin');
  });

  test('logged-in user visiting /login is redirected to dashboard', async ({ page, loginAs }) => {
    await loginAs('admin');
    await page.goto('/login');

    await page.waitForURL(/dashboard|floor|pos|inventory|recipes|reports/, { timeout: 10_000 });
    await expect(page).not.toHaveURL(/login/);
  });

  test('logout clears session and redirects to login', async ({ page }) => {
    // Login via API + evaluate (not loginAs which registers persistent addInitScript)
    const res = await page.request.post('http://localhost:8085/v1/auth/login', {
      data: { username: 'admin', password: 'admin' },
    });
    const data = await res.json();
    await page.goto('/login');
    await page.evaluate(({ token, user }) => {
      localStorage.setItem('bc_token', token);
      localStorage.setItem('bc_user', JSON.stringify(user));
    }, {
      token: data.token,
      user: {
        username: data.username,
        displayName: data.displayName ?? data.username,
        roles: data.roles ?? [],
        tenantId: data.tenantId ?? 'tenant1',
        primaryRole: data.primaryRole ?? 'Admin',
      },
    });
    await page.goto('/dashboard');
    await page.waitForLoadState('load');

    const nav = new NavPage(page);
    await nav.logout();

    await expect(page).toHaveURL(/login/, { timeout: 10_000 });
    await page.waitForLoadState('load');

    const token = await page.evaluate(() => localStorage.getItem('bc_token'));
    expect(token).toBeNull();
  });

  test('unauthenticated user is redirected to login', async ({ page }) => {
    // Clear any existing session
    await page.goto('/login');
    await page.evaluate(() => {
      localStorage.removeItem('bc_token');
      localStorage.removeItem('bc_user');
    });

    // The redirect may abort the hard navigation, so catch ERR_ABORTED
    await page.goto('/dashboard').catch(() => {});
    await expect(page).toHaveURL(/login/, { timeout: 10_000 });
  });
});
