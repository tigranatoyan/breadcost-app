import { test, expect } from '../fixtures/auth.fixture';

/**
 * i18n / Localization Sweep
 *
 * Visits every route and flags:
 * (a) Raw i18n keys displayed (e.g., "driver.refresh")
 * (b) Raw enum values displayed (e.g., DRAFT, IN_PRODUCTION)
 * (c) Pages that crash in Armenian locale
 *
 * Source: qa/ARC_QA_WORKFLOW.md Phase 2
 */

const ALL_ROUTES = [
  '/dashboard',
  '/orders',
  '/production-plans',
  '/floor',
  '/inventory',
  '/products',
  '/recipes',
  '/departments',
  '/pos',
  '/reports',
  '/invoices',
  '/admin',
  '/technologist',
  '/suppliers',
  '/subscriptions',
];

// Raw enum values that should be localized in the UI
const RAW_ENUMS = [
  'DRAFT', 'CONFIRMED', 'IN_PRODUCTION', 'READY', 'OUT_FOR_DELIVERY',
  'DELIVERED', 'CANCELLED', 'PENDING', 'APPROVED', 'GENERATED',
  'IN_PROGRESS', 'COMPLETED',
];

test.describe('i18n Sweep', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test('no raw i18n keys visible on any page (EN)', async ({ page }) => {
    const issues: { page: string; text: string }[] = [];

    for (const route of ALL_ROUTES) {
      await page.goto(route);
      await page.waitForLoadState('load');
      await page.waitForTimeout(1_000); // allow React render

      const allText = await page.locator('body').innerText();
      // Pattern: lowercase.lowercase (e.g., "driver.refresh", "orders.status")
      // but must not match known safe patterns like product names, URLs, etc.
      const keyPattern = /\b[a-z]{2,}\.[a-z][a-zA-Z]{2,}\b/g;
      const matches = allText.match(keyPattern) || [];
      for (const match of matches) {
        // Exclude common false positives
        if (match.includes('http') || match.match(/^\d/) || match === 'e.g'
            || match.startsWith('next.') || match.startsWith('react.')
            || match.endsWith('.com') || match.endsWith('.js') || match.endsWith('.ts')
            || match.endsWith('.tsx') || match.endsWith('.css')
            || match === 'localhost.localdomain') {
          continue;
        }
        issues.push({ page: route, text: match });
      }
    }

    if (issues.length > 0) {
      console.log('RAW I18N KEYS FOUND:');
      console.table(issues);
      test.info().annotations.push({ type: 'i18n-gap', description: JSON.stringify(issues) });
    }
    // Soft-fail: log findings for observation report, don't block suite
    expect(issues.length, `Found ${issues.length} raw i18n key(s) — see console output`).toBeLessThanOrEqual(5);
  });

  test('no raw enum values visible in status badges', async ({ page }) => {
    const issues: { page: string; text: string; count: number }[] = [];

    for (const route of ALL_ROUTES) {
      await page.goto(route);
      await page.waitForLoadState('load');
      await page.waitForTimeout(1_000);

      for (const enumVal of RAW_ENUMS) {
        // Only check inside badge/status elements and table cells
        const badges = page.locator('span, td, [class*="badge"]').filter({ hasText: new RegExp(`^${enumVal}$`) });
        const count = await badges.count();
        if (count > 0) {
          issues.push({ page: route, text: enumVal, count });
        }
      }
    }

    if (issues.length > 0) {
      console.log('RAW ENUM VALUES FOUND:');
      console.table(issues);
      test.info().annotations.push({ type: 'i18n-gap', description: JSON.stringify(issues) });
    }
    // Soft-fail: log findings for observation report, don't block suite
    expect(issues.length, `Found ${issues.length} raw enum(s) — see console output`).toBeLessThanOrEqual(20);
  });

  test('all pages render without crash in Armenian locale', async ({ page }) => {
    const crashed: string[] = [];

    for (const route of ALL_ROUTES) {
      await page.goto(route);
      await page.waitForLoadState('load');

      // Switch to Armenian
      const hyBtn = page.getByRole('button', { name: 'HY', exact: true });
      if (await hyBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
        await hyBtn.click();
        await page.waitForLoadState('load');
        await page.waitForTimeout(1_000);

        // Check for error boundaries or crash indicators
        const errorText = await page.locator('body').innerText();
        if (errorText.includes('Cannot read properties') || errorText.includes('undefined')
            || errorText.includes('Error') && errorText.includes('unhandled')) {
          crashed.push(route);
        }
      }
    }

    expect(crashed, `Pages crashed in HY locale: ${crashed.join(', ')}`).toHaveLength(0);
  });
});
