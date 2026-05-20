package com.openway.periplus.pages;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.model.ProductSummary;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ProductPage extends com.openway.periplus.core.BasePage {
    private static final By PRODUCT_TITLE = By.cssSelector(".quickview-content h2");
    private static final By PRODUCT_ID_META = By.cssSelector("meta[property='product:retailer_item_id']");
    private static final By PRICE_META = By.cssSelector("meta[property='product:price:amount']");
    private static final By ADD_TO_CART_BUTTON = By.cssSelector("button.btn-add-to-cart[onclick^='willAddtoCart']");

    public ProductPage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public ProductPage open(ProductSummary product) {
        return openByIsbn(product.isbn());
    }

    public ProductPage openByIsbn(String isbn) {
        openPath("p/" + isbn);
        wait.until(ExpectedConditions.visibilityOfElementLocated(PRODUCT_TITLE));
        wait.until(ExpectedConditions.presenceOfElementLocated(PRODUCT_ID_META));
        return this;
    }

    public ProductSummary summary(String expectedIsbn) {
        String productId = driver.findElement(PRODUCT_ID_META).getAttribute("content").trim();
        String title = driver.findElement(PRODUCT_TITLE).getText().trim();
        long unitPrice = Math.round(Double.parseDouble(driver.findElement(PRICE_META).getAttribute("content")));

        return new ProductSummary(productId, expectedIsbn, title, unitPrice);
    }

    public void addToCart(ProductSummary product) {
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BUTTON));
        String onclick = button.getAttribute("onclick");
        if (!onclick.contains(product.productId())) {
            throw new AssertionError("Product page add-to-cart button does not match selected product id "
                    + product.productId() + ". Button onclick: " + onclick);
        }
        click(button);
    }
}
