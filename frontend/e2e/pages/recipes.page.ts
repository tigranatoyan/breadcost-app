import { Page, Locator } from '@playwright/test';

export class RecipesPage {
  readonly page: Page;
  readonly newRecipeButton: Locator;
  readonly departmentFilter: Locator;
  readonly productFilter: Locator;
  readonly templatesButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newRecipeButton = page.getByRole('button', { name: /new recipe/i });
    this.departmentFilter = page.locator('select').first();
    this.productFilter = page.locator('select').nth(1);
    this.templatesButton = page.getByRole('button', { name: /templates/i });
  }

  async goto() {
    await this.page.goto('/recipes');
  }

  recipeCard(text: string): Locator {
    return this.page.locator('div').filter({ hasText: text }).first();
  }

  activateButton(): Locator {
    return this.page.getByRole('button', { name: /activate/i }).first();
  }

  ingredientsTab(): Locator {
    return this.page.getByRole('button', { name: /ingredients/i }).first();
  }

  stepsTab(): Locator {
    return this.page.getByRole('button', { name: /steps/i }).first();
  }
}
