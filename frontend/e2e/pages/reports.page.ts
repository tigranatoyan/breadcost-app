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
    this.ordersTab = page.getByRole('button', { name: /^orders$/i }).first();
    this.inventoryTab = page.getByRole('button', { name: /inventory/i }).first();
    this.productionTab = page.getByRole('button', { name: /production/i }).first();
    this.materialTab = page.getByRole('button', { name: /material/i }).first();
    this.costTab = page.getByRole('button', { name: /cost/i }).first();
    this.revenueTab = page.getByRole('button', { name: /revenue/i }).first();
    this.exportButton = page.getByRole('button', { name: /export/i }).first();
    this.dateFromInput = page.locator('input[type="date"]').first();
    this.dateToInput = page.locator('input[type="date"]').nth(1);
    this.departmentFilter = page.locator('select').first();
  }

  async goto() {
    await this.page.goto('/reports');
  }
}
