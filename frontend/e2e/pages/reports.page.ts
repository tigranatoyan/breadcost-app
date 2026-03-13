import { Page, Locator } from '@playwright/test';

export class ReportsPage {
  readonly page: Page;
  readonly ordersTab: Locator;
  readonly inventoryTab: Locator;
  readonly productionTab: Locator;
  readonly materialTab: Locator;
  readonly costTab: Locator;
  readonly revenueTab: Locator;
  readonly exportButton: Locator;
  readonly dateFromInput: Locator;
  readonly dateToInput: Locator;
  readonly departmentFilter: Locator;

  constructor(page: Page) {
    this.page = page;
    const main = page.locator('main');
    this.ordersTab = main.getByRole('button', { name: /orders/i }).first();
    this.inventoryTab = main.getByRole('button', { name: /inventory/i }).first();
    this.productionTab = main.getByRole('button', { name: /production/i }).first();
    this.materialTab = main.getByRole('button', { name: /material/i }).first();
    this.costTab = main.getByRole('button', { name: /cost/i }).first();
    this.revenueTab = main.getByRole('button', { name: /revenue/i }).first();
    this.exportButton = main.getByRole('button', { name: /export/i }).first();
    this.dateFromInput = main.locator('input[type="date"]').first();
    this.dateToInput = main.locator('input[type="date"]').nth(1);
    this.departmentFilter = main.locator('select').first();
  }

  async goto() {
    await this.page.goto('/reports');
  }
}
