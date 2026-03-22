import { test, expect } from '@playwright/test';

test.describe('Class Pet House E2E Smoke Tests', () => {

  test('Frontend should load and display correct title', async ({ page }) => {
    // We expect the Vite server to be running on 5173
    try {
      await page.goto('/');
      await expect(page).toHaveTitle(/宠物屋|Class Pet House|Vite/i);
    } catch (e) {
      console.log('Ensure the frontend server is running on localhost:5173');
      throw e;
    }
  });

  test('Teacher Login Page should have necessary fields', async ({ page }) => {
    await page.goto('/login');
    // Check if there is a username field
    const usernameInput = page.locator('input[type="text"], input[placeholder*="用户名"]');
    await expect(usernameInput.first()).toBeVisible();
    
    // Check if there is a password field
    const passwordInput = page.locator('input[type="password"]');
    await expect(passwordInput.first()).toBeVisible();
  });

  test('Student Portal should be accessible', async ({ page }) => {
    await page.goto('/student/login');
    // Ensure student login is distinct or at least loads
    const bodyText = await page.locator('body').innerText();
    expect(bodyText.length).toBeGreaterThan(0);
  });
});
