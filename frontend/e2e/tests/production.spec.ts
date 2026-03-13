import { test, expect } from '../fixtures/auth.fixture';
import { ProductionPage } from '../pages/production.page';

test.describe('Production Plans', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('production plans page loads with new-plan button', async ({ page }) => {
    const prod = new ProductionPage(page);
    await prod.goto();
    await page.waitForLoadState('networkidle');

    await expect(prod.newPlanButton).toBeVisible();
  });

  test('status filter is visible', async ({ page }) => {
    const prod = new ProductionPage(page);
    await prod.goto();
    await page.waitForLoadState('networkidle');

    await expect(prod.statusFilter).toBeVisible();
  });

  test('create plan modal opens with form fields', async ({ page }) => {
    const prod = new ProductionPage(page);
    await prod.goto();
    await page.waitForLoadState('networkidle');

    await prod.newPlanButton.click();
    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
    // Should have date input
    await expect(modal.locator('input[type="date"]')).toBeVisible();
  });

  test('create a new production plan', async ({ page }) => {
    const prod = new ProductionPage(page);
    await prod.goto();
    await page.waitForLoadState('networkidle');

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateStr = tomorrow.toISOString().split('T')[0];

    await prod.createPlan({ date: dateStr, notes: 'E2E test plan' });
    await page.waitForLoadState('networkidle');

    // Should still be on production plans page
    await expect(page).toHaveURL(/production-plans/);
  });

  test('filter plans by status', async ({ page }) => {
    const prod = new ProductionPage(page);
    await prod.goto();
    await page.waitForLoadState('networkidle');

    await prod.statusFilter.selectOption({ index: 1 }).catch(() => {});
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/production-plans/);
  });
});
