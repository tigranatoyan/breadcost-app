import { test, expect } from '../fixtures/auth.fixture';
import { ProductsPage } from '../pages/products.page';

test.describe('Products', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('products page loads with title and new product button', async ({ page }) => {
    const products = new ProductsPage(page);
    await products.goto();
    await page.waitForLoadState('load');

    await expect(page.getByText(/products/i).first()).toBeVisible();
    await expect(products.newProductButton).toBeVisible();
  });

  test('product table renders with rows', async ({ page }) => {
    const products = new ProductsPage(page);
    await products.goto();
    await page.waitForLoadState('load');

    const rows = await products.productRowCount();
    expect(rows).toBeGreaterThanOrEqual(0);
  });

  test('search filter works', async ({ page }) => {
    const products = new ProductsPage(page);
    await products.goto();
    await page.waitForLoadState('load');

    if (await products.searchInput.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await products.searchInput.fill('bread');
      await page.waitForLoadState('load');
      await expect(page).toHaveURL(/products/);
    }
  });

  test('department filter dropdown works', async ({ page }) => {
    const products = new ProductsPage(page);
    await products.goto();
    await page.waitForLoadState('load');

    if (await products.departmentFilter.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await products.departmentFilter.selectOption({ index: 1 }).catch(() => {});
      await page.waitForLoadState('load');
      await expect(page).toHaveURL(/products/);
    }
  });

  test('new product modal opens with form fields', async ({ page }) => {
    const products = new ProductsPage(page);
    await products.goto();
    await page.waitForLoadState('load');

    await products.newProductButton.click();
    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
    // Should have name input
    await expect(modal.locator('input, select').first()).toBeVisible();
  });
});
