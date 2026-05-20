package com.openway.periplus.pages;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

public class LoginPage extends BasePage {
    private static final By LOGIN_EMAIL = By.cssSelector("form#login input[name='email']");
    private static final By LOGIN_PASSWORD = By.cssSelector("form#login input[name='password']");
    private static final By LOGIN_BUTTON = By.cssSelector("form#login input[type='submit']");
    private static final By PAGE_MESSAGE = By.cssSelector(".warning, .alert, .error");

    public LoginPage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public LoginPage open() {
        openPath("account/Login");
        return this;
    }

    public AccountPage loginAs(String email, String password) {
        type(LOGIN_EMAIL, email);
        type(LOGIN_PASSWORD, password);
        click(LOGIN_BUTTON);

        wait.until(driver -> !driver.getCurrentUrl().contains("/account/Login")
                || !driver.findElements(PAGE_MESSAGE).isEmpty());

        Assert.assertFalse(driver.getCurrentUrl().contains("/account/Login"),
                "Login did not complete. Periplus stayed on the login page. Page message: " + visiblePageText());

        return new AccountPage(driver, wait, config);
    }
}
