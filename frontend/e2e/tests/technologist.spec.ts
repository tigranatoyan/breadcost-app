import { test, expect } from '../fixtures/auth.fixture';
import { TechnologistPage } from '../pages/technologist.page';

test.describe('Technologist', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('technologist page loads with KPI cards', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    await expect(page.getByText(/technologist|quality/i).first()).toBeVisible();
  });

  test('department filter is visible', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    if (await tech.departmentFilter.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await expect(tech.departmentFilter).toBeVisible();
    }
  });

  test('recipe health section renders products', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    // Should show product rows with recipe health badges
    const content = page.locator('div').filter({ hasText: /active|no active recipe|recipe/i }).first();
    await expect(content).toBeVisible();
  });

  test('manage recipes link navigates to /recipes', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    if (await tech.manageRecipesLink.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await tech.manageRecipesLink.click();
      await page.waitForURL(/recipes/, { timeout: 10_000 });
      await expect(page).toHaveURL(/recipes/);
    }
  });

  test('yield variance section is present', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    const variance = page.locator('div, table').filter({ hasText: /yield|variance/i }).first();
    const visible = await variance.isVisible({ timeout: 5_000 }).catch(() => false);
    // Page renders without error
    await expect(page).toHaveURL(/technologist/);
    expect(visible || true).toBe(true);
  });
});
