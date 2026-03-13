import { Page, Locator } from '@playwright/test';

export class FloorPage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async goto() {
    await this.page.goto('/floor');
  }

  planCard(text: string): Locator {
    return this.page.locator('div').filter({ hasText: text }).first();
  }

  workOrderCard(productName: string): Locator {
    return this.page.locator('div').filter({ hasText: productName }).first();
  }

  startButton(): Locator {
    return this.page.getByRole('button', { name: /start/i }).first();
  }

  completeButton(): Locator {
    return this.page.getByRole('button', { name: /complete/i }).first();
  }

  closeButton(): Locator {
    return this.page.getByRole('button', { name: /close/i }).first();
  }
}
