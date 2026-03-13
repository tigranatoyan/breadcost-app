import { Page, Locator } from '@playwright/test';

export class SubscriptionsPage {
  readonly page: Page;
  readonly tiersTab: Locator;
  readonly assignmentTab: Locator;
  readonly changeButton: Locator;
  readonly featureKeyInput: Locator;
  readonly checkButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.tiersTab = page.getByRole('button', { name: /available.?plans/i }).first();
    this.assignmentTab = page.getByRole('button', { name: /current.?subscription/i }).first();
    this.changeButton = page.getByRole('button', { name: /change|assign/i }).first();
    this.featureKeyInput = page.getByPlaceholder(/advanced_reports/i);
    this.checkButton = page.getByRole('button', { name: /check/i });
  }

  async goto() {
    await this.page.goto('/subscriptions');
  }

  tierCard(name: string): Locator {
    return this.page.locator('div').filter({ hasText: name }).first();
  }

  deactivateExpiredButton(): Locator {
    return this.page.getByRole('button', { name: /deactivate/i });
  }
}
