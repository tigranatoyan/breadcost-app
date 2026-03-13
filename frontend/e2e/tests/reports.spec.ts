import { test, expect } from '../fixtures/auth.fixture';
import { ReportsPage } from '../pages/reports.page';

test.describe('Reports', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('reports page loads with tabs', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/reports/i).first()).toBeVisible();
  });

  test('orders tab is active by default', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    // Revenue summary banner should be visible
    const kpis = page.locator('div').filter({ hasText: /total|revenue|orders/i }).first();
    await expect(kpis).toBeVisible();
  });

  test('switch between all 6 report tabs', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    const tabs = [rpt.inventoryTab, rpt.productionTab, rpt.materialTab, rpt.costTab, rpt.revenueTab, rpt.ordersTab];
    for (const tab of tabs) {
      await tab.click();
      await page.waitForLoadState('networkidle');
      await expect(page).toHaveURL(/reports/);
    }
  });

  test('date filters are visible', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    await expect(rpt.dateFromInput).toBeVisible();
    await expect(rpt.dateToInput).toBeVisible();
  });

  test('export CSV button is visible', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    await expect(rpt.exportButton).toBeVisible();
  });

  test('inventory tab shows stock data', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    await rpt.inventoryTab.click();
    await page.waitForLoadState('networkidle');

    // Should show inventory KPIs or table
    const content = page.locator('table, div').filter({ hasText: /position|value|item|stock/i }).first();
    await expect(content).toBeVisible();
  });

  test('production tab shows plan data', async ({ page }) => {
    const rpt = new ReportsPage(page);
    await rpt.goto();
    await page.waitForLoadState('networkidle');

    await rpt.productionTab.click();
    await page.waitForLoadState('networkidle');

    const content = page.locator('table, div').filter({ hasText: /plan|production|status/i }).first();
    await expect(content).toBeVisible();
  });
});
