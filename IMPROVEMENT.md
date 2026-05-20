# Periplus Selenium Automation Improvements

This document explains the production-grade improvements i made to the Periplus cart automation test.

## Goal

The original test only verified that one product could be added to the cart and that the product title appeared in the cart page.

The improved version verifies a comprehensive e-commerce cart flow:

- Login with externalized credentials.
- Select a deterministic product.
- Handle an existing cart safely.
- Add a requested quantity.
- Verify product identity, quantity, pricing, subtotal, cart total, header cart count, and checkout navigation.
- Capture screenshots on failure.
- Restore the cart item to its original state after the test.

## Main Test Flow

Main file:

```text
src/test/java/com/openway/periplus/PeriplusCartTest.java
```

Main test method:

```java
addMultipleUnitsToCartAndVerifyCheckout()
```

Important responsibilities in this test:

- Login is performed through `LoginPage`.
- Product selection is handled by `selectProduct()`.
- Existing cart state is captured before the test changes anything.
- Expected values are calculated before assertions.
- Assertions validate business behavior, not only UI visibility.
- Checkout page readiness is verified.
- Teardown restores the touched cart item.

## Page Object Model

The project now follows the Page Object Model pattern. This is to follow the best practice for Selenium.

Page objects are located in:

```text
src/test/java/com/openway/periplus/pages/
```

Current page objects:

| Page object | Purpose |
| --- | --- |
| `LoginPage` | Opens the login page and signs in. |
| `HomePage` | Opens home and performs search. |
| `SearchResultsPage` | Reads available products from search results when no pinned product is configured. |
| `ProductPage` | Opens a product detail page, reads product metadata, and adds it to cart. |
| `CartPage` | Reads cart rows, updates quantity, verifies totals, removes/restores items, and proceeds to checkout. |
| `CheckoutPage` | Verifies that checkout or a checkout step is reached. |

This keeps Selenium locators and UI interaction details out of the test class.

The product is selected in:

```text
src/test/java/com/openway/periplus/PeriplusCartTest.java
```

Method:

```java
selectProduct()
```

If `PERIPLUS_PRODUCT_ID` and `PERIPLUS_PRODUCT_ISBN` are configured, the test opens the product detail page directly:

```java
ProductSummary product = new ProductPage(driver, wait, config)
        .openByIsbn(config.productIsbn().orElseThrow())
        .summary(config.productIsbn().orElseThrow());
```

Then it validates that the configured product id matches the product detail page:

```java
Assert.assertEquals(product.productId(), config.productId().orElseThrow(),
        "Configured product id should match the product detail page.");
```

## Failure Screenshot Handling

Screenshot utility:

```text
src/test/java/com/openway/periplus/core/FailureArtifacts.java
```

Method:

```java
captureScreenshot(WebDriver driver, ITestResult result)
```

This method:

- Checks whether the current driver supports screenshots.
- Creates `build/reports/screenshots`.
- Saves a timestamped PNG file.
- Attaches the screenshot path to the TestNG result.

Core code:

```java
Path screenshotDir = Path.of("build", "reports", "screenshots");
Files.createDirectories(screenshotDir);

String testName = result.getMethod().getMethodName().replaceAll("[^A-Za-z0-9._-]", "_");
Path target = screenshotDir.resolve(testName + "-" + LocalDateTime.now().format(TIMESTAMP) + ".png");
File source = screenshotDriver.getScreenshotAs(OutputType.FILE);

Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
result.setAttribute("screenshot", target.toAbsolutePath().toString());
```

Screenshot capture is triggered in:

```text
src/test/java/com/openway/periplus/PeriplusCartTest.java
```

Inside:

```java
tearDown(ITestResult result)
```

Code:

```java
if (result.getStatus() == ITestResult.FAILURE) {
    FailureArtifacts.captureScreenshot(driver, result);
}
```

Screenshots are saved to:

```text
build/reports/screenshots/
```

Why this matters:

- Selenium failures are often visual or state-related.
- A screenshot helps identify whether the browser was on login, cart, checkout, an alert, an empty cart, or a changed page layout.
- Screenshot capture is wrapped safely so it never hides the real test failure.

## Assertion Strategy

The assertions are in:

```text
src/test/java/com/openway/periplus/PeriplusCartTest.java
```

The test does not only assert that elements are visible. It asserts business correctness.

### Product ID Assertion

Code:

```java
Assert.assertEquals(cartItem.productId(), selectedProduct.productId(),
        "Cart item should keep the same Periplus product id as the selected product.");
```

Purpose:

- Confirms that the exact selected product was added to the cart.
- Prevents false positives where a product title is similar but the cart item is actually different.
- Validates the internal Periplus identifier used by the website for cart actions.

Why it matters:

- Product titles can be duplicated.
- Product titles can be truncated.
- Product ids are stronger identifiers than text.
- The product id is the value Periplus uses behind the UI for add-to-cart and quantity updates.
- If the cart contains a different product id, then the cart does not contain the same item that the test selected.

### ISBN Assertion

Code:

```java
Assert.assertEquals(cartItem.isbn(), selectedProduct.isbn(),
        "Cart item should keep the same ISBN as the selected product.");
```

Purpose:

- Confirms the book identity at catalog level.
- Adds a second identity check beyond the Periplus internal product id.
- Confirms that the selected book edition is the same one shown in the cart.

Why it matters:

- ISBN is a stable book identifier.
- The same title can exist in different formats or editions.
- A wrong edition could have the same title but different ISBN.
- Product id confirms the internal website item.
- ISBN confirms the actual catalog/book identity.
- Together, product id and ISBN make the assertion stricter and reduce false positives.

### Title Assertion

Code:

```java
Assert.assertTrue(titleMatches(cartItem.title(), selectedProduct.title()),
        "Cart item title should match the selected product.");
```

Purpose:

- Confirms the user-facing product title still matches.

Why it matters:

- Product id and ISBN validate system identity.
- Title validates what the user actually sees.

The helper method handles Periplus title truncation:

```java
private boolean titleMatches(String cartTitle, String selectedTitle)
```

This prevents false failures when search results display a shortened title ending with `...`.

### Unit Price Assertion

Code:

```java
Assert.assertEquals(cartItem.unitPrice(), selectedProduct.unitPrice(),
        "Cart unit price should match the selected product price.");
```

Purpose:

- Confirms the cart is using the same unit price shown on the product detail page.

Why it matters:

- Price mismatches are critical in e-commerce.
- Discounts, stale pages, or cart calculation issues can cause price differences.

### Quantity Assertion

Code:

```java
Assert.assertEquals(cartItem.quantity(), expectedFinalProductQuantity,
        "Cart quantity should equal previous quantity plus the requested quantity.");
```

Purpose:

- Confirms quantity behavior is based on the requested amount.
- Handles existing cart state correctly.

Why it matters:

- If the product already exists in cart, the test should not assume the cart starts empty.
- Example: existing quantity `5`, requested quantity `2`, expected final quantity `7`.

### Line Subtotal Assertion

Code:

```java
Assert.assertEquals(cartItem.lineSubtotal(), expectedLineSubtotal,
        "Selected product line subtotal should equal unit price multiplied by final quantity.");
```

Purpose:

- Validates the selected product's row calculation.

Formula:

```text
line subtotal = unit price * final product quantity
```

Why it matters:

- This catches calculation errors on the cart row.
- It proves the selected product line is internally consistent.

### Cart Subtotal Assertion

Code:

```java
Assert.assertEquals(cart.subtotal(), expectedCartSubtotal,
        "Cart subtotal should increase by unit price multiplied by requested quantity.");
```

Purpose:

- Validates the whole cart subtotal after adding the requested quantity.

Formula:

```text
expected cart subtotal = initial subtotal + (unit price * requested quantity)
```

Why it matters:

- The cart may already contain other products.
- The assertion checks the correct delta instead of assuming a clean cart.

### Cart Total Assertion

Code:

```java
Assert.assertEquals(cart.total(), expectedCartSubtotal,
        "Cart total should match subtotal before shipping/payment charges are selected.");
```

Purpose:

- Verifies total amount before shipping or payment fees are applied.

Why it matters:

- At cart stage, Periplus has not selected shipping/payment charges yet.
- Total should match subtotal at this point.

### Header Cart Count Assertion

Code:

```java
Assert.assertEquals(cart.readCartCount(), expectedFinalCartCount,
        "Header cart count should increase by the requested quantity.");
```

Purpose:

- Confirms the header cart badge matches the actual requested cart quantity change.

Why it matters:

- Header count is a user-facing summary.
- It must stay synchronized with cart content.

### Checkout Assertions

Code:

```java
cart.proceedToCheckout()
        .assertLoaded()
        .assertReadyForNextCheckoutStep();
```

Located in:

```text
src/test/java/com/openway/periplus/pages/CheckoutPage.java
```

`assertLoaded()` validates that the user reaches checkout or a checkout sub-step:

```java
private boolean isCheckoutUrl() {
    String currentUrl = driver.getCurrentUrl();
    return currentUrl.contains("/checkout/checkout")
            || currentUrl.contains("route=checkout/");
}
```

Why it accepts both URL shapes:

- Periplus may route users to `/checkout/checkout`.
- Logged-in users may be redirected directly to a checkout step such as `route=checkout/shipping_address`.

`assertReadyForNextCheckoutStep()` validates that a recognizable checkout step is displayed:

```java
boolean hasCheckoutStep = containsIgnoringCase(pageText, "shipping address")
        || containsIgnoringCase(pageText, "member")
        || containsIgnoringCase(pageText, "guest checkout")
        || containsIgnoringCase(pageText, "payment")
        || containsIgnoringCase(pageText, "delivery");
```

Why this matters:

- The test should prove the cart can proceed into checkout.
- It should not depend on only one exact checkout URL.
- It should not proceed with an empty cart.

## Existing Cart Handling

The test captures the current cart state before adding the product:

```java
int initialCartCount = cart.readCartCount();
int existingProductQuantity = cart.findItem(selectedProduct.productId())
        .map(CartItem::quantity)
        .orElse(0);
long initialSubtotal = cart.subtotal();
```

Then it calculates expected values:

```java
int expectedFinalProductQuantity = existingProductQuantity + requestedQuantity;
int expectedFinalCartCount = initialCartCount + requestedQuantity;
long expectedLineSubtotal = selectedProduct.unitPrice() * expectedFinalProductQuantity;
long expectedCartSubtotal = initialSubtotal + (selectedProduct.unitPrice() * requestedQuantity);
```

Why this matters:

- The test is safe even when the cart is not empty.
- It does not delete unrelated cart items.
- It verifies the business impact of the requested quantity.

## Cart Cleanup / State Restoration

Cleanup is handled in:

```text
src/test/java/com/openway/periplus/PeriplusCartTest.java
```

Method:

```java
restoreCartState()
```

Code:

```java
new CartPage(driver, wait, config)
        .open()
        .restoreQuantity(touchedProduct, originalTouchedProductQuantity);
```

Cart restoration logic is in:

```text
src/test/java/com/openway/periplus/pages/CartPage.java
```

Methods:

```java
restoreQuantity(ProductSummary product, int originalQuantity)
removeItem(ProductSummary product)
```

Behavior:

- If the product existed before the test, its original quantity is restored.
- If the product did not exist before the test, it is removed after the test.
- Unrelated cart items are not touched.

Why this matters:

- Live E2E tests should avoid polluting shared test accounts.
- Cleanup makes repeated test runs safer.

## Selenium Best Practices Applied

In this project, I applied Selenium practices that make the automation easier to maintain, less flaky, and closer to how a real production UI test should be structured. I did not want the test to only "click things until it passes"; I wanted each step to wait for the right browser state and each assertion to prove an actual business rule.

### Explicit Waits

I used `WebDriverWait` and condition-based waits instead of `Thread.sleep()`.

The reason is simple: a live website does not always respond at the same speed. If I use fixed sleeps, the test can become both slow and flaky. Instead, I wait for the exact condition that the next step needs.

Examples:

```java
wait.until(ExpectedConditions.elementToBeClickable(locator));
wait.until(driver -> subtotal() == expectedSubtotal && total() == expectedTotal);
```

What this improves:

- The test does not continue until an element is clickable.
- The test does not assert cart totals before Periplus finishes its AJAX update.
- The test can run faster when the site is fast, but still wait when the site is slower.
- There is no blind waiting.

Applied in:

```text
src/test/java/com/openway/periplus/core/BasePage.java
src/test/java/com/openway/periplus/pages/CartPage.java
src/test/java/com/openway/periplus/pages/CheckoutPage.java
```

### Page Object Model

I separated the UI interaction logic from the test logic by using Page Object Model.

The test class focuses on the scenario:

```java
cart.open()
        .setQuantity(selectedProduct, expectedFinalProductQuantity)
        .waitForTotals(expectedCartSubtotal, expectedCartSubtotal);
```

The page class handles the Selenium details:

```text
src/test/java/com/openway/periplus/pages/CartPage.java
```

This makes the automation easier to extend later. If Periplus changes a cart locator, I only need to update `CartPage`, not every test case.

Why this matters:

- Test methods stay readable.
- Locators are not scattered everywhere.
- New automation scenarios can reuse existing pages.
- Maintenance is easier when the UI changes.

### Stable Runtime Configuration

I moved runtime data into config instead of hard-coding it inside the test.

Config is centralized in:

```text
src/test/java/com/openway/periplus/config/TestConfig.java
```

Supported values include:

- `PERIPLUS_EMAIL`
- `PERIPLUS_PASSWORD`
- `PERIPLUS_BASE_URL`
- `PERIPLUS_PRODUCT_QUERY`
- `PERIPLUS_PRODUCT_ID`
- `PERIPLUS_PRODUCT_ISBN`
- `PERIPLUS_CART_QUANTITY`
- `HEADLESS`
- `BROWSER_TIMEOUT_SECONDS`
- `CHROME_BINARY`

Why I did this:

- Secrets are not hard-coded.
- The same test can run locally, headless, or later in CI.
- Product data and quantity can be changed without editing Java code.
- A reviewer can understand which values are test data and which values are code behavior.

Example:

```bash
export PERIPLUS_PRODUCT_QUERY='Harry Potter'
export PERIPLUS_PRODUCT_ID='66846208'
export PERIPLUS_PRODUCT_ISBN='9798886633849'
export PERIPLUS_CART_QUANTITY='2'
```

### Deterministic Test Data

For the main cart scenario, I used a pinned product id and ISBN.

This is important because relying only on the first search result is risky. Search results can change order, products can become unavailable, and similar titles can appear in the same list.

I use both values because they validate different layers of product identity:

- The **Periplus product id** is the website's internal identifier. It is used by Periplus in cart operations such as add-to-cart, quantity update, and remove item.
- The **ISBN** is the book/catalog identifier. It represents the actual book or edition from a customer and catalog perspective.

Using only the product title is not strong enough. A title like `Harry Potter and the...` can appear many times in different formats, editions, bundles, or merchandise. If I only check the title, the test could pass even when the cart contains the wrong edition. By checking both product id and ISBN, I make sure the exact product selected by the system is also the exact book/edition shown in the cart.

The deterministic product flow is handled in:

```text
src/test/java/com/openway/periplus/PeriplusCartTest.java
src/test/java/com/openway/periplus/pages/ProductPage.java
```

The test opens the configured product detail page and validates that the product id matches:

```java
Assert.assertEquals(product.productId(), config.productId().orElseThrow(),
        "Configured product id should match the product detail page.");
```

Why this matters:

- The test checks the intended product.
- Product identity is not based only on text.
- Product id confirms the internal Periplus item.
- ISBN confirms the real book/catalog item.
- Similar titles or different editions are not accidentally accepted.
- The test is more stable across repeated runs.

### Business-Level Assertions

I used assertions that verify the business behavior of the cart, not only whether an element is displayed.

For example, the test asserts:

- product id
- ISBN
- title
- unit price
- quantity
- line subtotal
- cart subtotal
- cart total
- header cart count
- checkout step

This means the test checks whether the cart is actually correct from a user and business perspective.

Example:

```java
Assert.assertEquals(cart.subtotal(), expectedCartSubtotal,
        "Cart subtotal should increase by unit price multiplied by requested quantity.");
```

Why this matters:

- A visible product row is not enough.
- The wrong product could still be visible.
- The wrong quantity could still be visible.
- The wrong subtotal could still let the UI look normal.
- E-commerce tests need to verify calculations.

### Existing Cart State Handling

I did not assume that the cart starts empty.

Before adding the product, the test captures:

```java
int initialCartCount = cart.readCartCount();
int existingProductQuantity = cart.findItem(selectedProduct.productId())
        .map(CartItem::quantity)
        .orElse(0);
long initialSubtotal = cart.subtotal();
```

Then the expected values are calculated from the actual starting state:

```java
int expectedFinalProductQuantity = existingProductQuantity + requestedQuantity;
int expectedFinalCartCount = initialCartCount + requestedQuantity;
long expectedCartSubtotal = initialSubtotal + (selectedProduct.unitPrice() * requestedQuantity);
```

Why this matters:

- The test can run even if the selected product is already in the cart.
- The test can run even if other products exist in the cart.
- The assertion checks the change caused by the test, not an unrealistic empty-cart assumption.

### Cleanup / Cart Restoration

I added cleanup logic so the test does not leave the test account in a modified state.

The cleanup is triggered in:

```java
tearDown(ITestResult result)
```

The restore logic is:

```java
restoreCartState()
```

If the product existed before the test, the original quantity is restored. If the product did not exist before the test, it is removed.

Why this matters:

- Repeated test runs are safer.
- The account does not keep accumulating cart items.
- The test is less likely to affect future test results.

### Safe Quantity Update

The cart quantity input on Periplus can trigger JavaScript alerts if it is cleared like a normal text input. During live testing, using `clear()` and `sendKeys()` caused a browser alert.

To make it more stable, I update the input value with JavaScript and dispatch the same `change` event that the application listens to:

```java
((JavascriptExecutor) driver).executeScript(
        "arguments[0].value = arguments[1];"
                + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
        input,
        Integer.toString(quantity));
```

Why this matters:

- It avoids unnecessary alert issues from the numeric widget.
- It still triggers the app's cart update behavior.
- The test waits until the cart reflects the new quantity.

### Failure Artifacts

I added screenshot capture on test failure.

The code is in:

```text
src/test/java/com/openway/periplus/core/FailureArtifacts.java
```

It is called from:

```java
if (result.getStatus() == ITestResult.FAILURE) {
    FailureArtifacts.captureScreenshot(driver, result);
}
```

Screenshots are saved to:

```text
build/reports/screenshots/
```

Why this matters:

- Selenium failures are easier to debug with browser context.
- If the site layout changes, the screenshot shows what the browser saw.
- If the test fails on login, cart, checkout, or an alert, the screenshot helps identify the exact state.
- Screenshots are only captured on failure, so successful runs stay clean.

### Checkout Flow Flexibility

Periplus does not always stay on one checkout URL. In live testing, logged-in users can be redirected directly to a checkout step like:

```text
route=checkout/shipping_address
```

So `CheckoutPage` accepts both:

```java
currentUrl.contains("/checkout/checkout")
        || currentUrl.contains("route=checkout/")
```

Why this matters:

- The test follows the real application behavior.
- It does not fail just because Periplus redirects to a valid checkout sub-step.
- It still verifies that the cart did not proceed to checkout empty.

## Verified Live Run

The improved test was verified with:

```bash
set -a
source .env
set +a
gradle test -Dheadless=true
```

Result:

```text
addMultipleUnitsToCartAndVerifyCheckout PASSED
BUILD SUCCESSFUL
```

## Remaining Caveat

This is still a live end-to-end test against a third-party website.

The framework is production-grade for Selenium automation, but the test can still fail if:

- Periplus changes its HTML or CSS selectors.
- The pinned product becomes unavailable.
- The account is blocked, expired, or challenged.
- Network or site performance is unstable.
- Checkout business rules change.

These are normal risks for live third-party E2E tests, not problems with the automation structure.
