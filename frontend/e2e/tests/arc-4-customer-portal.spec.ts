import { test, expect } from '../fixtures/auth.fixture';

/**
 * Arc 4: Customer Self-Service Portal — Happy Path E2E
 *
 * Source: architecture/ARCMAP.md Arc 4
 * Trigger: Customer registers
 * End State: Order tracked and delivered via portal
 * Actors: Customer, System
 *
 * TODO: Populate with step-by-step assertions from ARCMAP.md happy path.
 */
test.describe('Arc 4: Customer Portal', () => {
  test.beforeEach(async ({ loginAs }) => {
    await loginAs('admin');
  });

  test.skip('customer list loads with name and contact info', async ({ page }) => {
    // Step: Admin views customer list
    // Assert: Customers shown with full names (not truncated)
  });

  test.skip('customer detail shows order history', async ({ page }) => {
    // Step: Admin views customer detail
    // Assert: Past orders listed with status and total
  });

  test.skip('loyalty balance displays correctly', async ({ page }) => {
    // Step: View loyalty for customer
    // Assert: Balance shown without requiring raw UUID input
  });

  test.skip('product catalog tab loads without errors', async ({ page }) => {
    // Step: Navigate to product catalog
    // Assert: No JS errors, products listed
  });
});
