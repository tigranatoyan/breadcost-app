import { test, expect } from '../fixtures/auth.fixture';
import { SuppliersPage } from '../pages/suppliers.page';

test.describe('Suppliers', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('suppliers page loads with tabs', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/suppliers/i).first()).toBeVisible();
  });

  test('add supplier button is visible on suppliers tab', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await expect(sup.addSupplierButton).toBeVisible();
  });

  test('switch to purchase orders tab', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await sup.purchaseOrdersTab.click();
    await page.waitForLoadState('networkidle');

    await expect(sup.createPOButton).toBeVisible();
  });

  test('generate POs from plan button visible on PO tab', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await sup.purchaseOrdersTab.click();
    await page.waitForLoadState('networkidle');

    await expect(sup.generateFromPlanButton).toBeVisible();
  });

  test('ingredient → supplier lookup panel on PO tab', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await sup.purchaseOrdersTab.click();
    await page.waitForLoadState('networkidle');

    await expect(sup.ingredientLookupInput()).toBeVisible();
    await expect(sup.findSuppliersButton()).toBeVisible();
  });

  test('switch to API config tab', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await sup.apiConfigTab.click();
    await page.waitForLoadState('networkidle');

    await expect(page).toHaveURL(/suppliers/);
  });

  test('add supplier modal opens with form', async ({ page }) => {
    const sup = new SuppliersPage(page);
    await sup.goto();
    await page.waitForLoadState('networkidle');

    await sup.addSupplierButton.click();
    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
  });
});
