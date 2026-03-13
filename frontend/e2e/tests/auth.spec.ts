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
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', 'admin');

    await page.waitForURL('**/dashboard**', { timeout: 15_000 });
    await expect(page).toHaveURL(/dashboard/);
  });

  test('invalid credentials show error message', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', 'wrongpassword');

    await expect(loginPage.errorBanner).toBeVisible({ timeout: 10_000 });
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

  test('logout clears session and redirects to login', async ({ page, loginAs }) => {
    await loginAs('admin');
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    const nav = new NavPage(page);
    await nav.logout();

    await page.waitForURL('**/login**', { timeout: 10_000 });
    await expect(page).toHaveURL(/login/);

    // Verify localStorage is cleared
    const token = await page.evaluate(() => localStorage.getItem('bc_token'));
    expect(token).toBeNull();
  });

  test('unauthenticated user is redirected to login', async ({ page }) => {
    // Clear any existing session
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.removeItem('bc_token');
      localStorage.removeItem('bc_user');
    });

    await page.goto('/dashboard');
    await page.waitForURL('**/login**', { timeout: 10_000 });
    await expect(page).toHaveURL(/login/);
  });
});
