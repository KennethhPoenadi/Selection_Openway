package com.openway.periplus.pages;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AccountPage extends BasePage {
    public AccountPage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public HomePage goHome() {
        openPath("");
        return new HomePage(driver, wait, config);
    }
}
