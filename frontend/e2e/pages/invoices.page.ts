import { Page, Locator } from '@playwright/test';

export class InvoicesPage {
  readonly page: Page;
  readonly invoicesTab: Locator;
  readonly discountsTab: Locator;
  readonly statusFilter: Locator;
  readonly invoiceTable: Locator;

  constructor(page: Page) {
    this.page = page;
    const main = page.locator('main');
    this.invoicesTab = main.getByRole('button', { name: /^invoices$/i }).first();
    this.discountsTab = main.getByRole('button', { name: /discount/i }).first();
    this.statusFilter = main.locator('select').first();
    this.invoiceTable = main.locator('table').first();
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
