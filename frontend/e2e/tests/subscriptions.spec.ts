import { test, expect } from '../fixtures/auth.fixture';
import { SubscriptionsPage } from '../pages/subscriptions.page';

test.describe('Subscriptions', () => {
  test.beforeEach(async ({ page, loginAs }) => {
    await loginAs('admin');
  });

  test('subscriptions page loads with tiers tab', async ({ page }) => {
    const sub = new SubscriptionsPage(page);
    await sub.goto();
    await page.waitForLoadState('networkidle');

    await expect(page.getByText(/subscriptions/i).first()).toBeVisible();
  });

  test('tier cards are rendered', async ({ page }) => {
    const sub = new SubscriptionsPage(page);
    await sub.goto();
    await page.waitForLoadState('networkidle');

    // Should show tier cards or a "no tiers" message
    const content = page.locator('.grid, table, p');
    await expect(content.first()).toBeVisible();
  });

  test('switch to assignment tab shows current subscription', async ({ page }) => {
    const sub = new SubscriptionsPage(page);
    await sub.goto();
    await page.waitForLoadState('networkidle');

    await sub.assignmentTab.click();
    await page.waitForLoadState('networkidle');

    // Should see either current subscription info or assign button
    const assignOrChange = page.getByRole('button', { name: /assign|change/i }).first();
    await expect(assignOrChange).toBeVisible();
  });

  test('feature check input is visible on assignment tab', async ({ page }) => {
    const sub = new SubscriptionsPage(page);
    await sub.goto();
    await page.waitForLoadState('networkidle');

    await sub.assignmentTab.click();
    await page.waitForLoadState('networkidle');

    await expect(sub.featureKeyInput).toBeVisible();
    await expect(sub.checkButton).toBeVisible();
  });

  test('feature check returns allowed/denied', async ({ page }) => {
    const sub = new SubscriptionsPage(page);
    await sub.goto();
    await page.waitForLoadState('networkidle');

    await sub.assignmentTab.click();
    await page.waitForLoadState('networkidle');

    await sub.featureKeyInput.fill('advanced_reports');
    await sub.checkButton.click();
    await page.waitForLoadState('networkidle');

    // Should show allowed or denied message
    const result = page.locator('p').filter({ hasText: /allowed|denied/i }).first();
    await expect(result).toBeVisible({ timeout: 10_000 });
  });

  test('assign modal opens when clicking assign/change', async ({ page }) => {
    const sub = new SubscriptionsPage(page);
    await sub.goto();
    await page.waitForLoadState('networkidle');

    await sub.assignmentTab.click();
    await page.waitForLoadState('networkidle');

    const assignBtn = page.getByRole('button', { name: /assign|change/i }).first();
    await assignBtn.click();

    const modal = page.locator('[class*="modal"], [role="dialog"], .fixed.inset-0').last();
    await expect(modal).toBeVisible();
    // Should have tier selector
    await expect(modal.locator('select')).toBeVisible();
  });
});
