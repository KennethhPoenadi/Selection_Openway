package com.openway.periplus;

import java.time.Duration;
import java.util.Locale;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BrowserFactory;
import com.openway.periplus.core.FailureArtifacts;
import com.openway.periplus.model.CartItem;
import com.openway.periplus.model.ProductSummary;
import com.openway.periplus.pages.CartPage;
import com.openway.periplus.pages.HomePage;
import com.openway.periplus.pages.LoginPage;
import com.openway.periplus.pages.ProductPage;
import com.openway.periplus.pages.SearchResultsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PeriplusCartTest {
    private TestConfig config;
    private WebDriver driver;
    private WebDriverWait wait;
    private ProductSummary touchedProduct;
    private int originalTouchedProductQuantity;
    private boolean shouldRestoreCart;

    @BeforeMethod
    public void setUp() {
        config = TestConfig.load();
        driver = BrowserFactory.createChrome(config);
        wait = new WebDriverWait(driver, Duration.ofSeconds(config.browserTimeoutSeconds()));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver != null) {
            if (result.getStatus() == ITestResult.FAILURE) {
                FailureArtifacts.captureScreenshot(driver, result);
            }
            restoreCartState();
            driver.quit();
        }
    }

    @Test(description = "Verify a logged-in user can add multiple units of a selected product and continue to checkout")
    public void addMultipleUnitsToCartAndVerifyCheckout() {
        int requestedQuantity = config.cartQuantity();

        new LoginPage(driver, wait, config)
                .open()
                .loginAs(config.email(), config.password());

        ProductSummary selectedProduct = selectProduct();

        CartPage cart = new CartPage(driver, wait, config).open();
        int initialCartCount = cart.readCartCount();
        int existingProductQuantity = cart.findItem(selectedProduct.productId())
                .map(CartItem::quantity)
                .orElse(0);
        long initialSubtotal = cart.subtotal();
        touchedProduct = selectedProduct;
        originalTouchedProductQuantity = existingProductQuantity;
        int expectedFinalProductQuantity = existingProductQuantity + requestedQuantity;
        int expectedFinalCartCount = initialCartCount + requestedQuantity;
        long expectedLineSubtotal = selectedProduct.unitPrice() * expectedFinalProductQuantity;
        long expectedCartSubtotal = initialSubtotal + (selectedProduct.unitPrice() * requestedQuantity);

        new ProductPage(driver, wait, config)
                .open(selectedProduct)
                .addToCart(selectedProduct);
        shouldRestoreCart = true;
        cart.waitForCartCount(initialCartCount + 1);

        cart.open()
                .setQuantity(selectedProduct, expectedFinalProductQuantity)
                .waitForTotals(expectedCartSubtotal, expectedCartSubtotal);
        cart.waitForCartCount(expectedFinalCartCount);

        CartItem cartItem = cart.requireItem(selectedProduct);

        Assert.assertEquals(cartItem.productId(), selectedProduct.productId(),
                "Cart item should keep the same Periplus product id as the selected product.");
        Assert.assertEquals(cartItem.isbn(), selectedProduct.isbn(),
                "Cart item should keep the same ISBN as the selected product.");
        Assert.assertTrue(titleMatches(cartItem.title(), selectedProduct.title()),
                "Cart item title should match the selected product.");
        Assert.assertEquals(cartItem.unitPrice(), selectedProduct.unitPrice(),
                "Cart unit price should match the selected product price.");
        Assert.assertEquals(cartItem.quantity(), expectedFinalProductQuantity,
                "Cart quantity should equal previous quantity plus the requested quantity.");
        Assert.assertEquals(cartItem.lineSubtotal(), expectedLineSubtotal,
                "Selected product line subtotal should equal unit price multiplied by final quantity.");
        Assert.assertEquals(cart.subtotal(), expectedCartSubtotal,
                "Cart subtotal should increase by unit price multiplied by requested quantity.");
        Assert.assertEquals(cart.total(), expectedCartSubtotal,
                "Cart total should match subtotal before shipping/payment charges are selected.");
        Assert.assertEquals(cart.readCartCount(), expectedFinalCartCount,
                "Header cart count should increase by the requested quantity.");

        cart.proceedToCheckout()
                .assertLoaded()
                .assertReadyForNextCheckoutStep();
    }

    private void restoreCartState() {
        if (!shouldRestoreCart || touchedProduct == null) {
            return;
        }

        try {
            new CartPage(driver, wait, config)
                    .open()
                    .restoreQuantity(touchedProduct, originalTouchedProductQuantity);
        } catch (RuntimeException ignored) {
            // Cleanup should preserve the real test result, not replace it with a teardown failure.
        }
    }

    private ProductSummary selectProduct() {
        if (config.productId().isPresent() && config.productIsbn().isPresent()) {
            ProductSummary product = new ProductPage(driver, wait, config)
                    .openByIsbn(config.productIsbn().orElseThrow())
                    .summary(config.productIsbn().orElseThrow());
            Assert.assertEquals(product.productId(), config.productId().orElseThrow(),
                    "Configured product id should match the product detail page.");
            return product;
        }

        SearchResultsPage results = new HomePage(driver, wait, config)
                .open()
                .searchFor(config.productQuery());
        return results.availableProduct(config.productId(), config.productIsbn());
    }

    private boolean titleMatches(String cartTitle, String selectedTitle) {
        String normalizedCartTitle = cartTitle.toLowerCase(Locale.ROOT).trim();
        String normalizedSelectedTitle = selectedTitle.toLowerCase(Locale.ROOT).trim();
        if (normalizedSelectedTitle.endsWith("...")) {
            normalizedSelectedTitle = normalizedSelectedTitle.substring(0, normalizedSelectedTitle.length() - 3).trim();
        }
        return !normalizedCartTitle.isBlank()
                && !normalizedSelectedTitle.isBlank()
                && normalizedCartTitle.contains(normalizedSelectedTitle);
    }
}
