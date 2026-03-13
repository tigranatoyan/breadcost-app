import { Page, Locator } from '@playwright/test';

export class DashboardPage {
  readonly page: Page;
  readonly brandLabel: Locator;
  readonly refreshButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.brandLabel = page.locator('text=BreadCost');
    this.refreshButton = page.locator('button:has(svg.lucide-refresh-cw), button:has(svg.lucide-rotate-cw)');
  }

  async goto() {
    await this.page.goto('/dashboard');
  }

  statCard(text: string): Locator {
    return this.page.locator(`a, div`).filter({ hasText: text }).first();
  }

  sectionHeading(text: string): Locator {
    return this.page.getByRole('heading', { name: text });
  }
}
