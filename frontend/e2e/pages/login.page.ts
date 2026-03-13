import { Page, Locator } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorBanner: Locator;
  readonly brandTitle: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('input[autocomplete="username"]');
    this.passwordInput = page.locator('input[autocomplete="current-password"]');
    this.submitButton = page.locator('button[type="submit"]');
    this.errorBanner = page.locator('.bg-red-50');
    this.brandTitle = page.locator('h1');
  }

  async goto() {
    await this.page.goto('/login');
  }

  /** Wait for React hydration — uses a brief delay since Next.js dev mode
   *  concurrent hydration makes deterministic checks unreliable */
  async waitForHydration() {
    await this.page.waitForTimeout(2000);
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }

  async clickDemoAccount(name: string) {
    await this.page.getByRole('button', { name }).click();
  }
}
