package com.openway.periplus.core;

import com.openway.periplus.config.TestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public final class BrowserFactory {
    private BrowserFactory() {
    }

    public static WebDriver createChrome(TestConfig config) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,1000");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--remote-allow-origins=*");

        config.chromeBinary().ifPresent(options::setBinary);

        if (config.headless()) {
            options.addArguments("--headless=new");
        }

        return new ChromeDriver(options);
    }
}
