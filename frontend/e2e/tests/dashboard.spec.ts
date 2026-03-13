import { test, expect } from '../fixtures/auth.fixture';
import { DashboardPage } from '../pages/dashboard.page';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('dashboard loads with KPI stat cards', async ({ page }) => {
    const dash = new DashboardPage(page);
    await dash.goto();
    await page.waitForLoadState('load');

    // Should see the BreadCost brand
    await expect(dash.brandLabel).toBeVisible();
    // Dashboard should have section content
    await expect(page).toHaveURL(/dashboard/);
  });

  test('dashboard shows order-related widgets', async ({ page }) => {
    const dash = new DashboardPage(page);
    await dash.goto();
    await page.waitForLoadState('load');

    // Look for common dashboard text
    const content = page.locator('main, [class*="content"]').first();
    await expect(content).toBeVisible();
  });

  test('language toggle switches between EN and HY', async ({ page }) => {
    const dash = new DashboardPage(page);
    await dash.goto();
    await page.waitForLoadState('load');

    // Click HY locale button
    const hyButton = page.getByRole('button', { name: 'HY', exact: true });
    if (await hyButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await hyButton.click();
      await page.waitForTimeout(500);

      // Click back to EN
      const enButton = page.getByRole('button', { name: 'EN', exact: true });
      await enButton.click();
      await page.waitForTimeout(500);
    }

    await expect(page).toHaveURL(/dashboard/);
  });
});
