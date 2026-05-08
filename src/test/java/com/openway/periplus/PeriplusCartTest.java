package com.openway.periplus;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PeriplusCartTest {
    private static final String BASE_URL = "https://www.periplus.com/";
    private static final By LOGIN_EMAIL = By.cssSelector("form#login input[name='email']");
    private static final By LOGIN_PASSWORD = By.cssSelector("form#login input[name='password']");
    private static final By LOGIN_BUTTON = By.cssSelector("form#login input[type='submit']");
    private static final By DESKTOP_SEARCH = By.id("filter_name_desktop");
    private static final By CART_TOTAL = By.id("cart_total");
    private static final By PRODUCT_CARDS = By.cssSelector(".single-product");
    private static final By ADD_TO_CART = By.cssSelector("a.addtocart[onclick^='update_total']");
    private static final By PRODUCT_TITLE = By.cssSelector(".product-content h3 a");

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        skipWhenCredentialIsMissing("periplus.email", "PERIPLUS_EMAIL");
        skipWhenCredentialIsMissing("periplus.password", "PERIPLUS_PASSWORD");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,1000");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--remote-allow-origins=*");

        findChromeBinary().ifPresent(options::setBinary);

        if (Boolean.parseBoolean(readConfig("headless", "HEADLESS", "false"))) {
            options.addArguments("--headless=new");
        }

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(readIntConfig("browser.timeout.seconds", "BROWSER_TIMEOUT_SECONDS", 25)));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test(description = "Login, find a product, add it to cart, and verify it is present")
    public void addOneProductToCart() {
        String email = requiredCredential("periplus.email", "PERIPLUS_EMAIL");
        String password = requiredCredential("periplus.password", "PERIPLUS_PASSWORD");
        String productQuery = readConfig("periplus.productQuery", "PERIPLUS_PRODUCT_QUERY", "Meditations");

        login(email, password);

        driver.get(BASE_URL);
        waitUntilReady();
        int initialCartCount = readCartCount();

        searchFor(productQuery);
        SelectedProduct product = selectFirstAvailableProduct();
        click(product.addToCartButton());

        wait.until(driver -> readCartCount() > initialCartCount);
        int updatedCartCount = readCartCount();
        Assert.assertEquals(updatedCartCount, initialCartCount + 1, "The cart count should increase by one after adding a product.");

        driver.get(BASE_URL + "checkout/cart");
        waitUntilReady();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("body"), product.title()));

        Assert.assertTrue(driver.getPageSource().toLowerCase(Locale.ROOT).contains(product.title().toLowerCase(Locale.ROOT)),
                "The shopping cart should contain the selected product title: " + product.title());
    }

    private void login(String email, String password) {
        driver.get(BASE_URL + "account/Login");
        waitUntilReady();

        type(LOGIN_EMAIL, email);
        type(LOGIN_PASSWORD, password);
        click(wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON)));

        wait.until(driver -> !driver.getCurrentUrl().contains("/account/Login")
                || !driver.findElements(By.cssSelector(".warning, .alert, .error")).isEmpty());

        Assert.assertFalse(driver.getCurrentUrl().contains("/account/Login"),
                "Login did not complete. Periplus stayed on the login page. Page message: " + visiblePageText());
    }

    private void searchFor(String productQuery) {
        WebElement search = wait.until(ExpectedConditions.elementToBeClickable(DESKTOP_SEARCH));
        search.clear();
        search.sendKeys(productQuery);
        search.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.urlContains("/product/Search"));
        wait.until(ExpectedConditions.presenceOfElementLocated(PRODUCT_CARDS));
    }

    private SelectedProduct selectFirstAvailableProduct() {
        List<WebElement> cards = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(PRODUCT_CARDS, 0));

        for (WebElement card : cards) {
            if (card.getText().toUpperCase(Locale.ROOT).contains("CURRENTLY UNAVAILABLE")) {
                continue;
            }

            List<WebElement> buttons = card.findElements(ADD_TO_CART);
            if (buttons.isEmpty()) {
                continue;
            }

            String title = card.findElement(PRODUCT_TITLE).getText().trim();
            if (!title.isEmpty()) {
                return new SelectedProduct(title, buttons.get(0));
            }
        }

        throw new AssertionError("No available product with an Add to cart button was found in the search results.");
    }

    private int readCartCount() {
        try {
            String text = wait.until(ExpectedConditions.visibilityOfElementLocated(CART_TOTAL)).getText().trim();
            String digits = text.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NoSuchElementException ignored) {
            return 0;
        }
    }

    private void type(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(value);
    }

    private void click(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private void waitUntilReady() {
        wait.until(driver -> "complete".equals(((JavascriptExecutor) driver).executeScript("return document.readyState")));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".preloader")));
    }

    private String visiblePageText() {
        String bodyText = driver.findElement(By.tagName("body")).getText().replaceAll("\\s+", " ").trim();
        return bodyText.length() > 500 ? bodyText.substring(0, 500) + "..." : bodyText;
    }

    private String requiredCredential(String propertyName, String envName) {
        String value = readConfig(propertyName, envName, "");
        if (value == null || value.isBlank()) {
            throw new SkipException("Set " + envName + " or -D" + propertyName + " before running the live Periplus test.");
        }
        return value;
    }

    private void skipWhenCredentialIsMissing(String propertyName, String envName) {
        String value = readConfig(propertyName, envName, "");
        if (value == null || value.isBlank()) {
            throw new SkipException("Set " + envName + " or -D" + propertyName + " before running the live Periplus test.");
        }
    }

    private String readConfig(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }

    private int readIntConfig(String propertyName, String envName, int defaultValue) {
        try {
            return Integer.parseInt(readConfig(propertyName, envName, Integer.toString(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private Optional<String> findChromeBinary() {
        String configured = readConfig("chrome.binary", "CHROME_BINARY", "");
        if (!configured.isBlank()) {
            return Optional.of(configured);
        }

        String macChrome = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
        if (new java.io.File(macChrome).exists()) {
            return Optional.of(macChrome);
        }

        return Optional.empty();
    }

    private record SelectedProduct(String title, WebElement addToCartButton) {
    }
}
