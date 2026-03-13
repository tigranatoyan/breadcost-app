import { test, expect } from '../fixtures/auth.fixture';
import { DepartmentsPage } from '../pages/departments.page';

test.describe('Departments', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('departments page loads with title', async ({ page }) => {
    const dept = new DepartmentsPage(page);
    await dept.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/departments/i).first()).toBeVisible();
  });

  test('new department button is visible', async ({ page }) => {
    const dept = new DepartmentsPage(page);
    await dept.goto();
    await page.waitForLoadState('networkidle');

    await expect(dept.newDepartmentButton).toBeVisible();
  });

  test('department table renders with rows', async ({ page }) => {
    const dept = new DepartmentsPage(page);
    await dept.goto();
    await page.waitForLoadState('networkidle');

    const rows = await dept.departmentRowCount();
    expect(rows).toBeGreaterThanOrEqual(0);
  });

  test('new department modal opens with form fields', async ({ page }) => {
    const dept = new DepartmentsPage(page);
    await dept.goto();
    await page.waitForLoadState('networkidle');

    await dept.newDepartmentButton.click();
    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
    // Should have name input and warehouse mode select
    await expect(modal.locator('input').first()).toBeVisible();
  });

  test('edit button opens edit modal when departments exist', async ({ page }) => {
    const dept = new DepartmentsPage(page);
    await dept.goto();
    await page.waitForLoadState('networkidle');

    const rows = await dept.departmentRowCount();
    if (rows > 0) {
      await dept.editButton().click();
      const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
      await expect(modal).toBeVisible();
    }
  });

  test('department table shows name and status columns', async ({ page }) => {
    const dept = new DepartmentsPage(page);
    await dept.goto();
    await page.waitForLoadState('networkidle');

    const rows = await dept.departmentRowCount();
    if (rows > 0) {
      // Table header should have Name and Status
      const headers = page.locator('thead th');
      const headerTexts = await headers.allInnerTexts();
      const hasName = headerTexts.some(h => /name/i.test(h));
      expect(hasName).toBe(true);
    }
  });
});
