import { Page, Locator } from '@playwright/test';

export class ProductsPage {
  readonly page: Page;
  readonly newProductButton: Locator;
  readonly searchInput: Locator;
  readonly departmentFilter: Locator;
  readonly productTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newProductButton = page.getByRole('button', { name: /new product/i });
    this.searchInput = page.getByPlaceholder(/search/i);
    this.departmentFilter = page.locator('select').first();
    this.productTable = page.locator('table').first();
  }

  async goto() {
    await this.page.goto('/products');
  }

  editButton(): Locator {
    return this.page.getByRole('button', { name: /edit/i }).first();
  }

  async productRowCount(): Promise<number> {
    return this.page.locator('tbody tr').count();
  }
}
