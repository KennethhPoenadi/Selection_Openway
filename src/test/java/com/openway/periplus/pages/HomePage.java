package com.openway.periplus.pages;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class HomePage extends BasePage {
    private static final By DESKTOP_SEARCH = By.id("filter_name_desktop");
    private static final By PRODUCT_CARDS = By.cssSelector(".single-product");

    public HomePage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public HomePage open() {
        openPath("");
        return this;
    }

    public SearchResultsPage searchFor(String productQuery) {
        WebElement search = wait.until(ExpectedConditions.elementToBeClickable(DESKTOP_SEARCH));
        search.clear();
        search.sendKeys(productQuery);
        search.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.urlContains("/product/Search"));
        wait.until(ExpectedConditions.presenceOfElementLocated(PRODUCT_CARDS));
        return new SearchResultsPage(driver, wait, config);
    }
}
