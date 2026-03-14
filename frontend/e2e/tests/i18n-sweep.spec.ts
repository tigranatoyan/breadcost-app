import { test, expect } from '../fixtures/auth.fixture';

/**
 * i18n / Localization Sweep
 *
 * Visits every route and flags:
 * (a) English strings missing Armenian translation
 * (b) Raw i18n keys displayed (e.g., "driver.refresh")
 * (c) Raw enum values displayed (e.g., DRAFT, IN_PRODUCTION)
 *
 * Source: qa/ARC_QA_WORKFLOW.md Phase 2
 * Reference locale: frontend/locales/hy.ts
 */

const ALL_ROUTES = [
  '/dashboard',
  '/orders',
  '/deliveries',
  '/driver',
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
];

// Known raw enum values that should never appear untranslated in the UI
const RAW_ENUMS = [
  'DRAFT', 'CONFIRMED', 'IN_PRODUCTION', 'READY', 'OUT_FOR_DELIVERY',
  'DELIVERED', 'CANCELLED', 'PENDING', 'APPROVED', 'GENERATED',
  'IN_PROGRESS', 'COMPLETED',
];

test.describe('i18n Sweep', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test.skip('no raw i18n keys visible on any page', async ({ page }) => {
    const issues: { page: string; text: string }[] = [];

    for (const route of ALL_ROUTES) {
      await page.goto(route);
      await page.waitForLoadState('load');

      // Look for patterns like "module.key" that indicate raw i18n keys
      const allText = await page.locator('body').innerText();
      const keyPattern = /\b[a-z]+\.[a-z]+[A-Za-z]*\b/g;
      const matches = allText.match(keyPattern) || [];
      for (const match of matches) {
        // Filter out false positives (URLs, versions, etc.)
        if (!match.includes('http') && !match.match(/^\d/)) {
          issues.push({ page: route, text: match });
        }
      }
    }

    expect(issues, `Raw i18n keys found: ${JSON.stringify(issues, null, 2)}`).toHaveLength(0);
  });

  test.skip('no raw enum values visible on any page', async ({ page }) => {
    const issues: { page: string; text: string; element: string }[] = [];

    for (const route of ALL_ROUTES) {
      await page.goto(route);
      await page.waitForLoadState('load');

      for (const enumVal of RAW_ENUMS) {
        const elements = page.getByText(enumVal, { exact: true });
        const count = await elements.count();
        if (count > 0) {
          issues.push({ page: route, text: enumVal, element: `${count} occurrence(s)` });
        }
      }
    }

    expect(issues, `Raw enums found: ${JSON.stringify(issues, null, 2)}`).toHaveLength(0);
  });
});
