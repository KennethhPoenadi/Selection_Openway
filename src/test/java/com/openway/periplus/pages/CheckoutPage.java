package com.openway.periplus.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.openway.periplus.config.TestConfig;
import com.openway.periplus.core.BasePage;

public class CheckoutPage extends BasePage {
    public CheckoutPage(WebDriver driver, WebDriverWait wait, TestConfig config) {
        super(driver, wait, config);
    }

    public CheckoutPage assertLoaded() {
        wait.until(driver -> isCheckoutUrl());
        Assert.assertTrue(isCheckoutUrl(),
                "Checkout CTA should navigate to a checkout page or checkout step.");
        Assert.assertFalse(containsIgnoringCase(visiblePageText(), "Your shopping cart is empty"),
                "Checkout should not be reached with an empty cart.");
        return this;
    }

    public CheckoutPage assertReadyForNextCheckoutStep() {
        String pageText = visiblePageText();
        boolean hasCheckoutStep = containsIgnoringCase(pageText, "shipping address")
                || containsIgnoringCase(pageText, "member")
                || containsIgnoringCase(pageText, "guest checkout")
                || containsIgnoringCase(pageText, "payment")
                || containsIgnoringCase(pageText, "delivery");
        Assert.assertTrue(hasCheckoutStep,
                "Checkout should show a recognizable next step such as shipping, member/guest checkout, payment, or delivery.");
        return this;
    }

    private boolean isCheckoutUrl() {
        String currentUrl = driver.getCurrentUrl();
        return currentUrl.contains("/checkout/checkout")
                || currentUrl.contains("route=checkout/");
    }
}
