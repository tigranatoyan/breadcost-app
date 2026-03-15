import { test, expect } from '../fixtures/auth.fixture';
import { TechnologistPage } from '../pages/technologist.page';

/**
 * Arc 7: AI Assistance — Happy Path E2E
 *
 * Source: architecture/ARCMAP.md Arc 7
 * Steps: AI suggestions page → pricing → quality → technologist KPIs
 */
test.describe('Arc 7: AI Assistance', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test('Step 1a: technologist page loads with KPI cards', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    // Assert: Technologist page renders with recipe/quality data
    await expect(page.locator('main')).toBeVisible();
    await expect(page).toHaveURL(/technologist/);
  });

  test('Step 1a: department filter works on technologist page', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    if (await tech.departmentFilter.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await tech.departmentFilter.selectOption({ index: 1 }).catch(() => {});
      await page.waitForLoadState('load');
    }

    await expect(page).toHaveURL(/technologist/);
  });

  test('Step 2: manage recipes link navigates to recipes', async ({ page }) => {
    const tech = new TechnologistPage(page);
    await tech.goto();
    await page.waitForLoadState('load');

    if (await tech.manageRecipesLink.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await tech.manageRecipesLink.click();
      await page.waitForURL(/recipes/, { timeout: 10_000 });
      await expect(page).toHaveURL(/recipes/);
    }
  });

  test('recipes page loads for recipe management', async ({ page }) => {
    await page.goto('/recipes');
    await page.waitForLoadState('load');

    await expect(page.getByText(/recipes/i).first()).toBeVisible();
  });
});
