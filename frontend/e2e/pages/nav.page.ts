import { Page, Locator } from '@playwright/test';

export class NavPage {
  readonly page: Page;
  readonly brand: Locator;
  readonly logoutButton: Locator;
  readonly mobileMenuButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.brand = page.locator('text=BreadCost').first();
    this.logoutButton = page.locator('button').filter({ has: page.locator('svg.lucide-log-out, [class*="log-out"]') });
    this.mobileMenuButton = page.locator('button').filter({ has: page.locator('svg.lucide-menu') });
  }

  /** Navigate to a page via the sidebar link */
  async navigateTo(linkText: string) {
    await this.page.getByRole('link', { name: new RegExp(linkText, 'i') }).first().click();
  }

  /** Check if a nav link is visible */
  async isNavLinkVisible(linkText: string): Promise<boolean> {
    return this.page.getByRole('link', { name: new RegExp(linkText, 'i') }).first().isVisible();
  }

  /** Logout via the sidebar button */
  async logout() {
    await this.logoutButton.click();
  }

  /** Switch locale */
  async switchLocale(locale: 'EN' | 'HY') {
    await this.page.getByRole('button', { name: locale, exact: true }).click();
  }
}
