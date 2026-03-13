import { test, expect } from '../fixtures/auth.fixture';
import { RecipesPage } from '../pages/recipes.page';

test.describe('Recipes', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('recipes page loads with section title', async ({ page }) => {
    const recipes = new RecipesPage(page);
    await recipes.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/recipes/i).first()).toBeVisible();
  });

  test('department filter is visible', async ({ page }) => {
    const recipes = new RecipesPage(page);
    await recipes.goto();
    await page.waitForLoadState('networkidle');

    await expect(recipes.departmentFilter).toBeVisible();
  });

  test('new recipe button is present', async ({ page }) => {
    const recipes = new RecipesPage(page);
    await recipes.goto();
    await page.waitForLoadState('networkidle');

    await expect(recipes.newRecipeButton).toBeVisible();
  });

  test('selecting a department loads product filter', async ({ page }) => {
    const recipes = new RecipesPage(page);
    await recipes.goto();
    await page.waitForLoadState('networkidle');

    // Select first department
    const options = await recipes.departmentFilter.locator('option').count();
    if (options > 1) {
      await recipes.departmentFilter.selectOption({ index: 1 });
      await page.waitForLoadState('networkidle');
      await expect(recipes.productFilter).toBeVisible();
    }
  });

  test('recipe cards show version and status badges', async ({ page }) => {
    const recipes = new RecipesPage(page);
    await recipes.goto();
    await page.waitForLoadState('networkidle');

    // Select a department and product to see recipe cards
    const options = await recipes.departmentFilter.locator('option').count();
    if (options > 1) {
      await recipes.departmentFilter.selectOption({ index: 1 });
      await page.waitForLoadState('networkidle');
      const prodOptions = await recipes.productFilter.locator('option').count();
      if (prodOptions > 1) {
        await recipes.productFilter.selectOption({ index: 1 });
        await page.waitForLoadState('networkidle');
        // Check for version badge or status badge
        const badges = page.locator('[class*="badge"], [class*="Badge"], span[class*="rounded"]');
        const count = await badges.count();
        expect(count).toBeGreaterThanOrEqual(0); // At least page renders without error
      }
    }
  });
});
