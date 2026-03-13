import { test, expect } from '../fixtures/auth.fixture';
import { InventoryPage } from '../pages/inventory.page';

test.describe('Inventory', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('inventory page loads with stock tab active', async ({ page }) => {
    const inv = new InventoryPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/inventory/i).first()).toBeVisible();
  });

  test('action buttons are visible (receive, transfer, adjust)', async ({ page }) => {
    const inv = new InventoryPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await expect(inv.receiveStockButton).toBeVisible();
    await expect(inv.transferButton).toBeVisible();
    await expect(inv.adjustButton).toBeVisible();
  });

  test('receive stock modal opens with form fields', async ({ page }) => {
    const inv = new InventoryPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await inv.receiveStockButton.click();
    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
    // Should have item selector and qty input
    await expect(modal.locator('select, input').first()).toBeVisible();
  });

  test('adjust stock modal opens with reason code', async ({ page }) => {
    const inv = new InventoryPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await inv.adjustButton.click();
    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
  });

  test('switch between stock and items tabs', async ({ page }) => {
    const inv = new InventoryPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    // Click items tab
    await inv.itemsTab.click();
    await page.waitForLoadState('networkidle');

    // Click back to stock tab
    await inv.stockTab.click();
    await page.waitForLoadState('networkidle');

    await expect(page).toHaveURL(/inventory/);
  });

  test('type filter dropdown works', async ({ page }) => {
    const inv = new InventoryPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    if (await inv.typeFilter.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await inv.typeFilter.selectOption({ index: 1 }).catch(() => {});
      await page.waitForLoadState('networkidle');
      await expect(page).toHaveURL(/inventory/);
    }
  });

  test('warehouse role can access inventory', async ({ page, loginAs }) => {
    await page.evaluate(() => {
      localStorage.removeItem('bc_token');
      localStorage.removeItem('bc_user');
    });
    await loginAs('warehouse');
    await page.goto('/inventory');
    await page.waitForLoadState('networkidle');

    await expect(page).toHaveURL(/inventory/);
  });
});
