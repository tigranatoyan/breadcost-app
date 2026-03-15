import { Page, Locator } from '@playwright/test';

export class SuppliersPage {
  readonly page: Page;
  readonly suppliersTab: Locator;
  readonly purchaseOrdersTab: Locator;
  readonly apiConfigTab: Locator;
  readonly addSupplierButton: Locator;
  readonly createPOButton: Locator;
  readonly suggestButton: Locator;
  readonly generateFromPlanButton: Locator;
  readonly supplierTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.suppliersTab = page.getByRole('button', { name: /^suppliers$/i }).first();
    this.purchaseOrdersTab = page.getByRole('button', { name: /purchase.?orders/i }).first();
    this.apiConfigTab = page.getByRole('button', { name: /api.?config/i }).first();
    this.addSupplierButton = page.getByRole('button', { name: /add|new supplier/i });
    this.createPOButton = page.getByRole('button', { name: /create po/i });
    this.suggestButton = page.getByRole('button', { name: /suggest/i });
    this.generateFromPlanButton = page.getByRole('button', { name: /generate.*plan/i });
    this.supplierTable = page.locator('table').first();
  }

  async goto() {
    await this.page.goto('/suppliers');
  }

  ingredientLookupInput(): Locator {
    return this.page.locator('.bg-gray-50 select').first();
  }

  findSuppliersButton(): Locator {
    return this.page.getByRole('button', { name: /lookup/i });
  }
}
