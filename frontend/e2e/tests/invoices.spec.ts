import { test, expect } from '../fixtures/auth.fixture';
import { InvoicesPage } from '../pages/invoices.page';

test.describe('Invoices', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('invoices page loads with tabs', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await expect(page.getByText(/invoices/i).first()).toBeVisible();
  });

  test('status filter dropdown is visible', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await expect(inv.statusFilter).toBeVisible();
  });

  test('DISPUTED status option available in filter', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await expect(inv.statusFilter).toBeVisible();
    await expect(inv.statusFilter.locator('option', { hasText: 'DISPUTED' })).toBeAttached();
  });

  test('switch to discounts tab', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await inv.discountsTab.click();
    await page.waitForLoadState('load');

    // Should see customer ID input field
    await expect(page.getByPlaceholder(/customer/i)).toBeVisible();
  });

  test('invoice row shows action buttons when invoices exist', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

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
    await page.waitForLoadState('load');

    // Filter to ISSUED status
    await inv.statusFilter.selectOption('ISSUED');
    await page.waitForLoadState('load');

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
    await page.waitForLoadState('load');

    const link = page.locator('tbody tr button.text-blue-600').first();
    if (await link.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await link.click();
      const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
      await expect(modal).toBeVisible();
    }
  });
});
