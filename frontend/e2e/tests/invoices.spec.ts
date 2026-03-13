import { test, expect } from '../fixtures/auth.fixture';
import { InvoicesPage } from '../pages/invoices.page';

test.describe('Invoices', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('invoices page loads with tabs', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/invoices/i).first()).toBeVisible();
  });

  test('status filter dropdown is visible', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await expect(inv.statusFilter).toBeVisible();
  });

  test('DISPUTED status option available in filter', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    const options = await inv.statusFilter.locator('option').allInnerTexts();
    expect(options.some(o => o.includes('DISPUTED'))).toBe(true);
  });

  test('switch to discounts tab', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    await inv.discountsTab.click();
    await page.waitForLoadState('networkidle');

    // Should see customer ID input field
    await expect(page.getByPlaceholder(/customer/i)).toBeVisible();
  });

  test('invoice row shows action buttons when invoices exist', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    const rowCount = await page.locator('tbody tr').count();
    if (rowCount > 0) {
      // At least credit or void button should be visible
      const actionButtons = page.locator('tbody tr').first().getByRole('button');
      const btnCount = await actionButtons.count();
      expect(btnCount).toBeGreaterThan(0);
    }
  });

  test('dispute button visible for ISSUED invoices', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    // Filter to ISSUED status
    await inv.statusFilter.selectOption('ISSUED');
    await page.waitForLoadState('networkidle');

    const rowCount = await page.locator('tbody tr').count();
    if (rowCount > 0) {
      // Dispute button should be present on ISSUED invoices
      const disputeBtn = page.getByRole('button', { name: /dispute/i }).first();
      await expect(disputeBtn).toBeVisible();
    }
  });

  test('clicking invoice ID opens detail modal', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('networkidle');

    const link = page.locator('tbody tr button.text-blue-600').first();
    if (await link.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await link.click();
      const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
      await expect(modal).toBeVisible();
    }
  });
});
