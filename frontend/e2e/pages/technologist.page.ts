import { Page, Locator } from '@playwright/test';

export class TechnologistPage {
  readonly page: Page;
  readonly departmentFilter: Locator;
  readonly manageRecipesLink: Locator;
  readonly addProductsLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.departmentFilter = page.locator('select').first();
    this.manageRecipesLink = page.getByRole('link', { name: /manage recipes/i });
    this.addProductsLink = page.getByRole('link', { name: /add products/i });
  }

  async goto() {
    await this.page.goto('/technologist');
  }

  kpiCard(text: string): Locator {
    return this.page.locator('div').filter({ hasText: text }).first();
  }
}
