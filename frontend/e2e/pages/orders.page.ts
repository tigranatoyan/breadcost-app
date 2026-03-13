import { Page, Locator } from '@playwright/test';

export class OrdersPage {
  readonly page: Page;
  readonly newOrderButton: Locator;
  readonly statusFilter: Locator;
  readonly searchInput: Locator;
  readonly orderTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newOrderButton = page.getByRole('button', { name: /new order/i });
    this.statusFilter = page.locator('select').first();
    this.searchInput = page.getByPlaceholder(/search|customer/i);
    this.orderTable = page.locator('table').first();
  }

  async goto() {
    await this.page.goto('/orders');
  }

  /** Open the "New Order" modal */
  async openNewOrderModal() {
    await this.newOrderButton.click();
  }

  /** Fill the create-order form and submit */
  async createOrder(opts: {
    customerName: string;
    deliveryTime?: string;
    notes?: string;
    lines: Array<{ productIndex: number; qty: string; price: string }>;
  }) {
    // Fill customer name
    const modal = this.page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await modal.getByPlaceholder(/customer/i).fill(opts.customerName);

    if (opts.notes) {
      await modal.getByPlaceholder(/notes/i).fill(opts.notes);
    }

    for (let i = 0; i < opts.lines.length; i++) {
      const line = opts.lines[i];
      // Select product from dropdown in the i-th row
      const selects = modal.locator('select');
      const selectCount = await selects.count();
      if (selectCount > i) {
        await selects.nth(i).selectOption({ index: line.productIndex });
      }
      // Fill qty and price
      const qtyInputs = modal.locator('input[type="number"]');
      // Each row has qty, price inputs in sequence
      const baseIndex = i * 2;
      if (await qtyInputs.nth(baseIndex).isVisible()) {
        await qtyInputs.nth(baseIndex).fill(line.qty);
      }
      if (await qtyInputs.nth(baseIndex + 1).isVisible()) {
        await qtyInputs.nth(baseIndex + 1).fill(line.price);
      }

      // Add another line if needed
      if (i < opts.lines.length - 1) {
        const addLineBtn = modal.getByRole('button', { name: /add line/i });
        if (await addLineBtn.isVisible()) {
          await addLineBtn.click();
        }
      }
    }

    // Submit the modal
    const submitBtn = modal.getByRole('button', { name: /create|save|submit/i });
    await submitBtn.click();
  }

  /** Click an action button for a specific order row */
  async clickOrderAction(orderText: string, actionName: string) {
    const row = this.page.locator('tr, [class*="card"]').filter({ hasText: orderText });
    await row.getByRole('button', { name: new RegExp(actionName, 'i') }).click();
  }

  /** Get the count of visible order rows */
  async orderRowCount(): Promise<number> {
    return this.page.locator('tbody tr, [class*="order-row"]').count();
  }
}
