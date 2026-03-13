import { Page, Locator } from '@playwright/test';

export class PosPage {
  readonly page: Page;
  readonly eodButton: Locator;
  readonly productSearch: Locator;
  readonly departmentFilter: Locator;
  readonly completeSaleButton: Locator;
  readonly cartPanel: Locator;

  constructor(page: Page) {
    this.page = page;
    this.eodButton = page.getByRole('button', { name: /end of day/i });
    this.productSearch = page.getByPlaceholder(/search/i);
    this.departmentFilter = page.locator('select').first();
    this.completeSaleButton = page.getByRole('button', { name: /complete sale/i });
    this.cartPanel = page.locator('[class*="cart"], [class*="checkout"]').first();
  }

  async goto() {
    await this.page.goto('/pos');
  }

  /** Click a product card to open QuickAdd */
  async selectProduct(name: string) {
    await this.page.locator('[class*="card"], [class*="grid"] > div').filter({ hasText: name }).first().click();
  }

  /** Fill QuickAdd and add to cart */
  async quickAdd(qty: string, price?: string) {
    const overlay = this.page.locator('[class*="modal"], [role="dialog"], .fixed').last();
    const qtyInput = overlay.locator('input[type="number"]').first();
    await qtyInput.fill(qty);
    if (price) {
      const priceInput = overlay.locator('input[type="number"]').nth(1);
      if (await priceInput.isVisible()) {
        await priceInput.fill(price);
      }
    }
    await overlay.getByRole('button', { name: /add to cart/i }).click();
  }

  /** Complete the sale */
  async completeSale(opts?: {
    customerName?: string;
    paymentMethod?: 'CASH' | 'CARD';
    cashReceived?: string;
    cardReference?: string;
  }) {
    if (opts?.customerName) {
      const customerInput = this.page.getByPlaceholder(/customer|walk-in/i);
      if (await customerInput.isVisible()) {
        await customerInput.fill(opts.customerName);
      }
    }
    if (opts?.paymentMethod) {
      const paymentSelect = this.page.locator('select').filter({ hasText: /cash|card/i });
      if (await paymentSelect.isVisible()) {
        await paymentSelect.selectOption(opts.paymentMethod);
      }
    }
    if (opts?.cashReceived) {
      const cashInput = this.page.locator('input[type="number"]').last();
      await cashInput.fill(opts.cashReceived);
    }
    await this.completeSaleButton.click();
  }

  /** Cart item count */
  async cartItemCount(): Promise<number> {
    return this.page.locator('[class*="cart"] [class*="item"], [class*="line-item"]').count();
  }
}
