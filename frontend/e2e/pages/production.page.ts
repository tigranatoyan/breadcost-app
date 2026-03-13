import { Page, Locator } from '@playwright/test';

export class ProductionPage {
  readonly page: Page;
  readonly newPlanButton: Locator;
  readonly statusFilter: Locator;
  readonly dateFilter: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newPlanButton = page.getByRole('button', { name: /new plan/i });
    this.statusFilter = page.locator('select').first();
    this.dateFilter = page.locator('input[type="date"]');
  }

  async goto() {
    await this.page.goto('/production-plans');
  }

  /** Create a new production plan */
  async createPlan(opts: { date: string; shift?: string; notes?: string }) {
    await this.newPlanButton.click();
    const modal = this.page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    const dateInput = modal.locator('input[type="date"]');
    await dateInput.fill(opts.date);
    if (opts.shift) {
      const shiftSelect = modal.locator('select');
      if (await shiftSelect.isVisible()) {
        await shiftSelect.selectOption(opts.shift);
      }
    }
    if (opts.notes) {
      await modal.getByPlaceholder(/optional|instructions/i).fill(opts.notes);
    }
    await modal.getByRole('button', { name: /create|save|submit/i }).click();
  }

  /** Click a plan action button (e.g., Generate, Approve, Start) */
  async clickPlanAction(planText: string, action: string) {
    const card = this.page.locator('tr, [class*="card"], [class*="plan"]').filter({ hasText: planText });
    await card.getByRole('button', { name: new RegExp(action, 'i') }).click();
  }

  /** Get plan count */
  async planCount(): Promise<number> {
    return this.page.locator('tbody tr, [class*="plan-card"], [class*="plan-row"]').count();
  }
}
