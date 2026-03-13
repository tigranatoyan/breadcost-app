import { Page, Locator } from '@playwright/test';

export class DepartmentsPage {
  readonly page: Page;
  readonly newDepartmentButton: Locator;
  readonly departmentTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newDepartmentButton = page.getByRole('button', { name: /new department/i });
    this.departmentTable = page.locator('table').first();
  }

  async goto() {
    await this.page.goto('/departments');
  }

  editButton(): Locator {
    return this.page.getByRole('button', { name: /edit/i }).first();
  }

  async departmentRowCount(): Promise<number> {
    return this.page.locator('tbody tr').count();
  }
}
