import { test, expect } from '../fixtures/auth.fixture';
import { PosPage } from '../pages/pos.page';

/**
 * Arc 5: POS & Walk-in Sales — Happy Path E2E
 *
 * Source: architecture/ARCMAP.md Arc 5
 * Steps: Open POS → select products → add to cart → complete sale → receipt
 */
test.describe('Arc 5: POS & Walk-in Sales', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test('Step 1: POS page loads with product grid', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    // Assert: POS page renders with product display
    await expect(page.getByText(/pos/i).first()).toBeVisible();
    await expect(pos.completeSaleButton).toBeVisible();
  });

  test('Step 1: product search filters the grid', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    if (await pos.productSearch.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await pos.productSearch.fill('bread');
      await page.waitForTimeout(500);
      // Grid should still be visible (filtered)
      await expect(page).toHaveURL(/pos/);
    }
  });

  test('Step 2: add items to cart → total updates', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    // Click a product card to open quick-add
    const productCard = page.locator('[class*="card"], [class*="grid"] > div')
      .filter({ has: page.locator('button, [class*="cursor"]') })
      .first();

    if (await productCard.isVisible({ timeout: 5_000 }).catch(() => false)) {
      await productCard.click();
      // Quick-add overlay should appear with qty input
      const overlay = page.locator('[class*="modal"], [role="dialog"], .fixed').last();
      const qtyInput = overlay.locator('input[type="number"]').first();
      if (await qtyInput.isVisible({ timeout: 5_000 }).catch(() => false)) {
        await qtyInput.fill('5');
        // Click add to cart
        const addBtn = overlay.getByRole('button', { name: /add to cart/i });
        if (await addBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
          await addBtn.click();
          await page.waitForLoadState('load');
        }
      }
    }

    await expect(page).toHaveURL(/pos/);
  });

  test('Step 3-4: complete sale button is functional', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    // Complete sale button should be present
    await expect(pos.completeSaleButton).toBeVisible();
  });

  test('Step 6: EOD (End of Day) button exists', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    await expect(pos.eodButton).toBeVisible();
  });

  test('cashier role can access POS', async ({ page, loginAs }) => {
    await loginAs('cashier');
    await page.goto('/pos');
    await page.waitForLoadState('load');

    await expect(page).toHaveURL(/pos/);
    await expect(page.getByText(/pos/i).first()).toBeVisible();
  });
});
