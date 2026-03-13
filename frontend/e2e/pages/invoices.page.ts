import { Page, Locator } from '@playwright/test';

export class InvoicesPage {
  readonly page: Page;
  readonly invoicesTab: Locator;
  readonly discountsTab: Locator;
  readonly statusFilter: Locator;
  readonly invoiceTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.invoicesTab = page.getByRole('button', { name: /^invoices$/i }).first();
    this.discountsTab = page.getByRole('button', { name: /discounts/i }).first();
    this.statusFilter = page.locator('select').first();
    this.invoiceTable = page.locator('table').first();
  }

  async goto() {
    await this.page.goto('/invoices');
  }

  payButton(): Locator {
    return this.page.getByRole('button', { name: /^pay$/i }).first();
  }

  disputeButton(): Locator {
    return this.page.getByRole('button', { name: /dispute/i }).first();
  }

  resolveButton(): Locator {
    return this.page.getByRole('button', { name: /resolve/i }).first();
  }

  creditButton(): Locator {
    return this.page.getByRole('button', { name: /credit/i }).first();
  }

  voidButton(): Locator {
    return this.page.getByRole('button', { name: /void/i }).first();
  }
}
