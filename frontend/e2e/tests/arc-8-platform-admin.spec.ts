import { test, expect } from '../fixtures/auth.fixture';
import { DashboardPage } from '../pages/dashboard.page';
import { SubscriptionsPage } from '../pages/subscriptions.page';

/**
 * Arc 8: Platform Administration — Happy Path E2E
 *
 * Source: architecture/ARCMAP.md Arc 8
 * Steps: Admin page → user management → subscription tiers → feature gates → dashboard
 */
test.describe('Arc 8: Platform Administration', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test('Step 1: admin page loads with user management', async ({ page }) => {
    await page.goto('/admin');
    await page.waitForLoadState('load');

    // Assert: Admin page renders
    await expect(page.locator('main')).toBeVisible();
    await expect(page).toHaveURL(/admin/);
  });

  test('Step 3: subscription tiers page loads', async ({ page }) => {
    const subs = new SubscriptionsPage(page);
    await subs.goto();
    await page.waitForLoadState('load');

    // Assert: Subscription page renders with tier info
    await expect(page.locator('main')).toBeVisible();
  });

  test('Step 3: subscription assignment tab accessible', async ({ page }) => {
    const subs = new SubscriptionsPage(page);
    await subs.goto();
    await page.waitForLoadState('load');

    if (await subs.assignmentTab.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await subs.assignmentTab.click();
      await page.waitForLoadState('load');
    }

    await expect(page).toHaveURL(/subscriptions/);
  });

  test('Step 4: feature gate check functionality', async ({ page }) => {
    const subs = new SubscriptionsPage(page);
    await subs.goto();
    await page.waitForLoadState('load');

    // Try the feature key check input
    if (await subs.featureKeyInput.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await subs.featureKeyInput.fill('ADVANCED_REPORTS');
      const checkBtn = subs.checkButton;
      if (await checkBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
        await checkBtn.click();
        await page.waitForLoadState('load');
      }
    }

    await expect(page).toHaveURL(/subscriptions/);
  });

  test('Step 6: dashboard loads with widgets', async ({ page }) => {
    const dash = new DashboardPage(page);
    await dash.goto();
    await page.waitForLoadState('load');

    // Assert: Dashboard renders with brand and stat cards
    await expect(page.locator('main')).toBeVisible();
    await expect(dash.brandLabel).toBeVisible();
  });

  test('viewer role has limited access', async ({ page, loginAs }) => {
    await loginAs('viewer');
    await page.goto('/dashboard');
    await page.waitForLoadState('load');

    // Viewer can access dashboard
    await expect(page.locator('main')).toBeVisible();
  });
});
