package com.openway.periplus.pages;

import java.util.List;
import java.util.Optional;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BasePage;
import com.openway.periplus.core.Money;
import com.openway.periplus.model.CartItem;
import com.openway.periplus.model.ProductSummary;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CartPage extends BasePage {
    private static final By CART_ROW = By.cssSelector(".row-cart-product");
    private static final By CART_QUANTITY = By.cssSelector("input[id^='qty_']");
    private static final By REMOVE_ITEM = By.cssSelector("a.btn-cart-remove[href*='checkout/cart?remove=']");
    private static final By CHECKOUT_BUTTON = By.cssSelector(".button5 a.btn, a[href$='/checkout/checkout']");
    private static final By TOTAL_SUMMARY_ROWS = By.cssSelector(".total-amount .right li");

    public CartPage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public CartPage open() {
        openPath("checkout/cart");
        return this;
    }

    public CartPage clear() {
        open();
        while (!driver.findElements(REMOVE_ITEM).isEmpty()) {
            int previousCount = readCartCount();
            WebElement remove = wait.until(ExpectedConditions.elementToBeClickable(REMOVE_ITEM));
            click(remove);
            waitUntilReady();
            wait.until(driver -> readCartCount() < previousCount || driver.findElements(REMOVE_ITEM).isEmpty());
        }
        if (readCartCount() != 0) {
            throw new IllegalStateException("Cart should be empty after clearing it.");
        }
        return this;
    }

    public CartItem requireItem(ProductSummary product) {
        return findItem(product.productId())
                .orElseThrow(() -> new AssertionError("Cart should contain product id " + product.productId()
                        + " / ISBN " + product.isbn() + ". Page text: " + visiblePageText()));
    }

    public Optional<CartItem> findItem(String productId) {
        if (driver.findElements(CART_ROW).isEmpty()) {
            return Optional.empty();
        }

        for (WebElement row : driver.findElements(CART_ROW)) {
            List<WebElement> quantityInputs = row.findElements(CART_QUANTITY);
            if (quantityInputs.isEmpty()) {
                continue;
            }

            WebElement quantityInput = quantityInputs.get(0);
            String currentProductId = quantityInput.getAttribute("id").replace("qty_", "");
            if (productId.equals(currentProductId)) {
                return Optional.of(readItem(row, quantityInput));
            }
        }

        return Optional.empty();
    }

    public CartPage setQuantity(ProductSummary product, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Cart quantity must be at least one.");
        }
        WebElement input = quantityInput(product.productId());
        scrollIntoView(input);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input,
                Integer.toString(quantity));

        wait.until(driver -> {
            try {
                CartItem item = requireItem(product);
                return item.quantity() == quantity;
            } catch (StaleElementReferenceException ignored) {
                return false;
            }
        });

        return this;
    }

    public CartPage waitForTotals(long expectedSubtotal, long expectedTotal) {
        wait.until(driver -> subtotal() == expectedSubtotal && total() == expectedTotal);
        return this;
    }

    public CartPage restoreQuantity(ProductSummary product, int originalQuantity) {
        if (originalQuantity <= 0) {
            return removeItem(product);
        }

        return setQuantity(product, originalQuantity);
    }

    public CartPage removeItem(ProductSummary product) {
        By removeProduct = By.cssSelector("a.btn-cart-remove[href*='remove=" + product.productId() + "']");
        if (driver.findElements(removeProduct).isEmpty()) {
            return this;
        }

        WebElement remove = wait.until(ExpectedConditions.elementToBeClickable(removeProduct));
        click(remove);
        waitUntilReady();
        wait.until(driver -> findItem(product.productId()).isEmpty());
        return this;
    }

    public long subtotal() {
        return totalByLabel("Sub-Total");
    }

    public long total() {
        return totalByLabel("Total");
    }

    public CheckoutPage proceedToCheckout() {
        WebElement checkout = visibleElements(CHECKOUT_BUTTON).stream()
                .filter(element -> containsIgnoringCase(element.getText(), "checkout"))
                .findFirst()
                .orElseGet(() -> wait.until(ExpectedConditions.elementToBeClickable(CHECKOUT_BUTTON)));
        click(checkout);
        wait.until(driver -> driver.getCurrentUrl().contains("/checkout/checkout")
                || driver.getCurrentUrl().contains("route=checkout/"));
        waitUntilReady();
        return new CheckoutPage(driver, wait, config);
    }

    private CartItem readItem(WebElement row, WebElement quantityInput) {
        String productId = quantityInput.getAttribute("id").replace("qty_", "");
        String title = row.findElement(By.cssSelector(".product-name a")).getText().trim();
        String href = row.findElement(By.cssSelector(".product-name a")).getAttribute("href");
        String isbn = href.replaceFirst(".*/p/(\\d+)(?:/.*)?", "$1");
        int quantity = Integer.parseInt(quantityInput.getAttribute("value"));
        long unitPrice = Money.parseRupiah(row.getText());

        return new CartItem(productId, isbn, title, quantity, unitPrice);
    }

    private WebElement quantityInput(String productId) {
        return wait.until(ExpectedConditions.elementToBeClickable(By.id("qty_" + productId)));
    }

    private long totalByLabel(String label) {
        if (driver.findElements(CART_ROW).isEmpty()) {
            return 0;
        }

        for (WebElement row : wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(TOTAL_SUMMARY_ROWS))) {
            if (row.getText().startsWith(label)) {
                return Money.parseRupiah(row.findElement(By.cssSelector("span")).getText());
            }
        }

        throw new AssertionError("Cart total row was not found for label: " + label);
    }
}
