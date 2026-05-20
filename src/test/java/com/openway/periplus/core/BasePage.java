package com.openway.periplus.core;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import com.openway.periplus.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {
    private static final By CART_TOTAL = By.id("cart_total");
    private static final By PRELOADER = By.cssSelector(".preloader");

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final TestConfig config;

    protected BasePage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        this.driver = driver;
        this.wait = wait;
        this.config = config;
    }

    protected void openPath(String path) {
        driver.get(config.baseUrl() + stripLeadingSlash(path));
        waitUntilReady();
    }

    public int readCartCount() {
        try {
            String text = wait.until(ExpectedConditions.visibilityOfElementLocated(CART_TOTAL)).getText().trim();
            String digits = text.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NoSuchElementException | TimeoutException ignored) {
            return 0;
        }
    }

    public void waitForCartCount(int expectedCount) {
        wait.until(driver -> readCartCount() == expectedCount);
    }

    public void waitForCartCountGreaterThan(int previousCount) {
        wait.until(driver -> readCartCount() > previousCount);
    }

    protected void type(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(value);
    }

    protected void click(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        scrollIntoView(element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void click(By locator) {
        click(wait.until(ExpectedConditions.elementToBeClickable(locator)));
    }

    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    protected void waitUntilReady() {
        wait.until(driver -> "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState")));
        try {
            wait.withTimeout(Duration.ofSeconds(3))
                    .ignoring(NoSuchElementException.class, StaleElementReferenceException.class)
                    .until(ExpectedConditions.invisibilityOfElementLocated(PRELOADER));
        } catch (TimeoutException ignored) {
            // Some Periplus pages leave the hidden preloader node around longer than the page load.
        } finally {
            wait.withTimeout(Duration.ofSeconds(config.browserTimeoutSeconds()));
        }
    }

    protected String visiblePageText() {
        String bodyText = driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
        return bodyText.length() > 500 ? bodyText.substring(0, 500) + "..." : bodyText;
    }

    protected boolean containsIgnoringCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    protected List<WebElement> visibleElements(By locator) {
        return driver.findElements(locator).stream().filter(WebElement::isDisplayed).toList();
    }

    private String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
