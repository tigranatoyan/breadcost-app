import { test, expect } from '../fixtures/auth.fixture';

/**
 * STRICT Observation Validator
 *
 * Zero-tolerance checks for each QA observation from the original report.
 * Every test takes a screenshot as evidence. FAILS on any finding.
 */

test.describe('OBS Validation (STRICT)', () => {

  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  // ── OBS-1: Login auto-redirect for authenticated users ─────────────────────
  test('OBS-1: authenticated user visiting /login should redirect to /dashboard', async ({ page }) => {
    // We are already logged in via loginAs('admin')
    await page.goto('/login');
    await page.waitForTimeout(3_000); // give time for any redirect
    const url = page.url();
    await page.screenshot({ path: 'test-results/obs-1-login-redirect.png', fullPage: true });
    expect(url, 'Authenticated user should be redirected away from /login').not.toContain('/login');
  });

  // ── OBS-2: Missing recipes.subtitle i18n key ──────────────────────────────
  test('OBS-2: /recipes should not show raw key "recipes.subtitle"', async ({ page }) => {
    await page.goto('/recipes');
    await page.waitForLoadState('load');
    await page.waitForTimeout(2_000);
    const bodyText = await page.locator('body').innerText();
    await page.screenshot({ path: 'test-results/obs-2-recipes-subtitle.png', fullPage: true });
    expect(bodyText, 'Raw key "recipes.subtitle" should not appear').not.toContain('recipes.subtitle');
  });

  // ── OBS-3: Raw enums on /dashboard ─────────────────────────────────────────
  test('OBS-3: /dashboard should not show raw enum values in badges', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('load');
    await page.waitForTimeout(2_000);
    await page.screenshot({ path: 'test-results/obs-3-dashboard-enums.png', fullPage: true });

    const rawEnums = ['DRAFT', 'CONFIRMED', 'APPROVED', 'GENERATED', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED', 'PENDING'];
    const found: string[] = [];
    for (const e of rawEnums) {
      const badges = page.locator('span, td').filter({ hasText: new RegExp(`^${e}$`) });
      const count = await badges.count();
      if (count > 0) found.push(`${e}(×${count})`);
    }
    expect(found, `Raw enums found on /dashboard: ${found.join(', ')}`).toHaveLength(0);
  });

  // ── OBS-4: Raw enums on /production-plans ──────────────────────────────────
  test('OBS-4: /production-plans should not show raw enum values in badges', async ({ page }) => {
    await page.goto('/production-plans');
    await page.waitForLoadState('load');
    await page.waitForTimeout(2_000);
    await page.screenshot({ path: 'test-results/obs-4-production-plans-enums.png', fullPage: true });

    const rawEnums = ['DRAFT', 'CONFIRMED', 'APPROVED', 'GENERATED', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED', 'PENDING'];
    const found: string[] = [];
    for (const e of rawEnums) {
      const badges = page.locator('span, td').filter({ hasText: new RegExp(`^${e}$`) });
      const count = await badges.count();
      if (count > 0) found.push(`${e}(×${count})`);
    }
    expect(found, `Raw enums found on /production-plans: ${found.join(', ')}`).toHaveLength(0);
  });

  // ── OBS-5: Raw enums on /floor ─────────────────────────────────────────────
  test('OBS-5: /floor should not show raw enum values in badges', async ({ page }) => {
    await page.goto('/floor');
    await page.waitForLoadState('load');
    await page.waitForTimeout(2_000);
    await page.screenshot({ path: 'test-results/obs-5-floor-enums.png', fullPage: true });

    const rawEnums = ['DRAFT', 'CONFIRMED', 'APPROVED', 'GENERATED', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED', 'PENDING'];
    const found: string[] = [];
    for (const e of rawEnums) {
      const badges = page.locator('span, td').filter({ hasText: new RegExp(`^${e}$`) });
      const count = await badges.count();
      if (count > 0) found.push(`${e}(×${count})`);
    }
    expect(found, `Raw enums found on /floor: ${found.join(', ')}`).toHaveLength(0);
  });

  // ── OBS-6: Raw enums on /technologist ──────────────────────────────────────
  test('OBS-6: /technologist should not show raw enum values in badges', async ({ page }) => {
    await page.goto('/technologist');
    await page.waitForLoadState('load');
    await page.waitForTimeout(2_000);
    await page.screenshot({ path: 'test-results/obs-6-technologist-enums.png', fullPage: true });

    const rawEnums = ['DRAFT', 'CONFIRMED', 'APPROVED', 'GENERATED', 'IN_PRODUCTION', 'COMPLETED', 'CANCELLED', 'PENDING'];
    const found: string[] = [];
    for (const e of rawEnums) {
      const badges = page.locator('span, td').filter({ hasText: new RegExp(`^${e}$`) });
      const count = await badges.count();
      if (count > 0) found.push(`${e}(×${count})`);
    }
    expect(found, `Raw enums found on /technologist: ${found.join(', ')}`).toHaveLength(0);
  });

  // ── FULL PAGE SCREENSHOT AUDIT ─────────────────────────────────────────────
  // Takes a screenshot of every major page for visual inspection
  const ALL_PAGES = [
    '/dashboard', '/orders', '/production-plans', '/floor',
    '/inventory', '/products', '/recipes', '/departments',
    '/pos', '/reports', '/invoices', '/technologist',
    '/suppliers', '/subscriptions', '/admin',
  ];

  test('Screenshot audit: capture every page for manual review', async ({ page }) => {
    test.setTimeout(120_000);
    for (const route of ALL_PAGES) {
      await page.goto(route);
      await page.waitForLoadState('load');
      await page.waitForTimeout(2_000);
      const name = route.replaceAll('/', '_').replace(/^_/, '');
      await page.screenshot({ path: `test-results/page-${name}.png`, fullPage: true });
    }
    // Always passes — this is for evidence gathering
    expect(true).toBe(true);
  });
});
