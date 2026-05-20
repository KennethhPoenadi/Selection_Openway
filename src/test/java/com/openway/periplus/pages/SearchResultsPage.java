package com.openway.periplus.pages;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BasePage;
import com.openway.periplus.core.Money;
import com.openway.periplus.model.ProductSummary;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SearchResultsPage extends BasePage {
    private static final By PRODUCT_CARDS = By.cssSelector(".single-product");
    private static final By ADD_TO_CART = By.cssSelector("a.addtocart[onclick^='update_total']");
    private static final By PRODUCT_TITLE = By.cssSelector(".product-content h3 a");
    private static final By PRODUCT_PRICE = By.cssSelector(".product-price");
    private static final Pattern PRODUCT_ID = Pattern.compile("update_total\\((\\d+)");
    private static final Pattern ISBN = Pattern.compile("/p/(\\d+)/");

    public SearchResultsPage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public ProductSummary firstAvailableProduct() {
        return availableProduct(Optional.empty(), Optional.empty());
    }

    public ProductSummary availableProduct(Optional<String> targetProductId, Optional<String> targetIsbn) {
        List<WebElement> cards = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(PRODUCT_CARDS, 0));

        for (WebElement card : cards) {
            if (card.getText().toUpperCase(Locale.ROOT).contains("CURRENTLY UNAVAILABLE")) {
                continue;
            }

            List<WebElement> buttons = card.findElements(ADD_TO_CART);
            if (buttons.isEmpty()) {
                continue;
            }

            ProductSummary product = readProduct(card, buttons.get(0));
            boolean productIdMatches = targetProductId.map(product.productId()::equals).orElse(true);
            boolean isbnMatches = targetIsbn.map(product.isbn()::equals).orElse(true);
            if (!product.title().isEmpty() && productIdMatches && isbnMatches) {
                return product;
            }
        }

        throw new AssertionError("No available product matched the search result criteria. Product id: "
                + targetProductId.orElse("<any>") + ", ISBN: " + targetIsbn.orElse("<any>"));
    }

    public void addToCart(ProductSummary product) {
        WebElement addToCart = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.addtocart[onclick^='update_total(" + product.productId() + ")']")));
        click(addToCart);
    }

    private String parse(Pattern pattern, String value, String fieldName) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            throw new AssertionError("Could not parse " + fieldName + " from: " + value);
        }
        return matcher.group(1);
    }

    private ProductSummary readProduct(WebElement card, WebElement addToCartButton) {
        String title = card.findElement(PRODUCT_TITLE).getText().trim();
        String href = card.findElement(PRODUCT_TITLE).getAttribute("href");
        String onclick = addToCartButton.getAttribute("onclick");
        long unitPrice = Money.parseRupiah(card.findElement(PRODUCT_PRICE).getText());

        return new ProductSummary(parse(PRODUCT_ID, onclick, "product id"),
                parse(ISBN, href, "ISBN"),
                title,
                unitPrice);
    }
}
