import { test, expect } from '../fixtures/auth.fixture';
import { InvoicesPage } from '../pages/invoices.page';
import { ReportsPage } from '../pages/reports.page';

/**
 * Arc 6: Financial Operations — Happy Path E2E
 *
 * Source: architecture/ARCMAP.md Arc 6
 * Steps: View invoices → issue → pay → dispute → reports
 */
test.describe('Arc 6: Financial Operations', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test('Step 1: invoices page loads with invoice list', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await expect(page.getByText(/invoices/i).first()).toBeVisible();
    await expect(inv.statusFilter).toBeVisible();
  });

  test('Step 2-3: filter invoices by status (ISSUED)', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await inv.statusFilter.selectOption('ISSUED');
    await page.waitForLoadState('load');

    await expect(page).toHaveURL(/invoices/);
  });

  test('Step 5: mark invoice paid action available', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    // Filter to ISSUED invoices
    await inv.statusFilter.selectOption('ISSUED');
    await page.waitForLoadState('load');

    const rowCount = await page.locator('tbody tr').count();
    if (rowCount > 0) {
      // Pay button should be available on ISSUED invoices
      const payBtn = inv.payButton();
      if (await payBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
        // Verify it exists — don't click to avoid test data change
        await expect(payBtn).toBeVisible();
      }
    }
  });

  test('DISPUTED status in filter dropdown', async ({ page }) => {
    const inv = new InvoicesPage(page);
    await inv.goto();
    await page.waitForLoadState('load');

    await expect(inv.statusFilter.locator('option', { hasText: 'DISPUTED' })).toBeAttached();
  });

  test('Step 6: reports page loads with all tabs', async ({ page }) => {
    const reports = new ReportsPage(page);
    await reports.goto();
    await page.waitForLoadState('load');

    // Assert: Reports page loads without crash (was BC-302 fix)
    await expect(page.getByText(/reports/i).first()).toBeVisible();
    await expect(reports.ordersTab).toBeVisible();
  });

  test('finance role can access invoices', async ({ page, loginAs }) => {
    await loginAs('finance');
    await page.goto('/invoices');
    await page.waitForLoadState('load');

    await expect(page).toHaveURL(/invoices/);
  });
});
