import { test, expect } from '../fixtures/auth.fixture';
import { NavPage } from '../pages/nav.page';

test.describe('Role-based Navigation', () => {
  test('admin sees all nav sections', async ({ page, loginAs }) => {
    await loginAs('admin');
    await page.goto('/dashboard');
    await page.waitForLoadState('load');

    const nav = new NavPage(page);
    expect(await nav.isNavLinkVisible('dashboard')).toBe(true);
    expect(await nav.isNavLinkVisible('orders')).toBe(true);
    expect(await nav.isNavLinkVisible('inventory')).toBe(true);
    expect(await nav.isNavLinkVisible('point of sale')).toBe(true);
  });

  test('cashier sees POS but not admin pages', async ({ page, loginAs }) => {
    await loginAs('cashier');
    // Cashier role defaults to /pos
    await page.goto('/');
    await page.waitForLoadState('load');

    const nav = new NavPage(page);
    expect(await nav.isNavLinkVisible('point of sale')).toBe(true);
    // Admin-only sections should not be visible
    expect(await nav.isNavLinkVisible('admin')).toBe(false);
    expect(await nav.isNavLinkVisible('departments')).toBe(false);
  });

  test('production user sees floor page', async ({ page, loginAs }) => {
    await loginAs('production');
    await page.goto('/');
    await page.waitForLoadState('load');

    const nav = new NavPage(page);
    expect(await nav.isNavLinkVisible('floor')).toBe(true);
  });

  test('finance user sees reports', async ({ page, loginAs }) => {
    await loginAs('finance');
    await page.goto('/');
    await page.waitForLoadState('load');

    const nav = new NavPage(page);
    expect(await nav.isNavLinkVisible('reports')).toBe(true);
  });

  test('warehouse user sees inventory', async ({ page, loginAs }) => {
    await loginAs('warehouse');
    await page.goto('/');
    await page.waitForLoadState('load');

    const nav = new NavPage(page);
    expect(await nav.isNavLinkVisible('inventory')).toBe(true);
  });
});
