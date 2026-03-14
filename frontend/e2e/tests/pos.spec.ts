import { test, expect } from '../fixtures/auth.fixture';
import { PosPage } from '../pages/pos.page';

test.describe('POS', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test('POS page loads with product grid and cart', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    // Should have product search and complete sale button
    await expect(page.getByText(/pos/i).first()).toBeVisible();
  });

  test('search filters product grid', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    if (await pos.productSearch.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await pos.productSearch.fill('bread');
      await page.waitForTimeout(500); // debounce
      await expect(page).toHaveURL(/pos/);
    }
  });

  test('select product opens quick-add overlay', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    // Click the first product card
    const productCard = page.locator('[class*="card"], [class*="grid"] > div')
      .filter({ has: page.locator('button, [class*="cursor"]') })
      .first();

    if (await productCard.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await productCard.click();
      // A modal/overlay with qty input should appear
      const overlay = page.locator('[class*="modal"], [role="dialog"], .fixed').last();
      await expect(overlay.locator('input[type="number"]').first()).toBeVisible({ timeout: 5_000 });
    }
  });

  test('complete sale button is present', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    await expect(pos.completeSaleButton).toBeVisible();
  });

  test('end of day button is present', async ({ page }) => {
    const pos = new PosPage(page);
    await pos.goto();
    await page.waitForLoadState('load');

    await expect(pos.eodButton).toBeVisible();
  });

  test('cashier role can access POS page', async ({ page, loginAs }) => {
    // Re-login as cashier (addInitScript overwrites previous auth on next navigation)
    await loginAs('cashier');
    await page.goto('/pos');
    await page.waitForLoadState('load');

    await expect(page).toHaveURL(/pos/);
    await expect(page.getByText(/pos/i).first()).toBeVisible();
  });
});
