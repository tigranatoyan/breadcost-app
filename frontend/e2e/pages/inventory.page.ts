import { Page, Locator } from '@playwright/test';

export class InventoryPage {
  readonly page: Page;
  readonly stockTab: Locator;
  readonly itemsTab: Locator;
  readonly receiveStockButton: Locator;
  readonly transferButton: Locator;
  readonly adjustButton: Locator;
  readonly typeFilter: Locator;
  readonly searchInput: Locator;
  readonly alertsToggle: Locator;
  readonly stockTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.stockTab = page.getByRole('button', { name: /stock/i }).first();
    this.itemsTab = page.getByRole('button', { name: /items/i }).first();
    this.receiveStockButton = page.getByRole('button', { name: /receive/i });
    this.transferButton = page.getByRole('button', { name: /transfer/i });
    this.adjustButton = page.getByRole('button', { name: /adjust/i });
    this.typeFilter = page.locator('select').first();
    this.searchInput = page.getByPlaceholder(/search/i);
    this.alertsToggle = page.getByRole('button', { name: /alert/i });
    this.stockTable = page.locator('table').first();
  }

  async goto() {
    await this.page.goto('/inventory');
  }

  /** Open Receive Stock modal and fill form */
  async receiveStock(opts: {
    itemIndex: number;
    qty: string;
    unitCost: string;
  }) {
    await this.receiveStockButton.click();
    const modal = this.page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    const selects = modal.locator('select');
    await selects.first().selectOption({ index: opts.itemIndex });
    const numberInputs = modal.locator('input[type="number"]');
    await numberInputs.first().fill(opts.qty);
    await numberInputs.nth(1).fill(opts.unitCost);
    await modal.getByRole('button', { name: /save|submit|receive/i }).click();
  }

  /** Open Adjust modal and fill form */
  async adjustStock(opts: {
    itemIndex: number;
    qty: string;
    reasonCode: string;
    notes?: string;
  }) {
    await this.adjustButton.click();
    const modal = this.page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    const selects = modal.locator('select');
    await selects.first().selectOption({ index: opts.itemIndex });
    const numberInputs = modal.locator('input[type="number"]');
    await numberInputs.first().fill(opts.qty);
    // Select reason code
    if (await selects.nth(1).isVisible()) {
      await selects.nth(1).selectOption(opts.reasonCode);
    }
    if (opts.notes) {
      await modal.getByPlaceholder(/notes/i).fill(opts.notes);
    }
    await modal.getByRole('button', { name: /save|submit|adjust/i }).click();
  }

  /** Get row count in stock table */
  async stockRowCount(): Promise<number> {
    return this.page.locator('tbody tr').count();
  }
}
