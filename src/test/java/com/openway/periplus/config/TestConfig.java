package com.openway.periplus.config;

import java.util.Optional;

import org.testng.SkipException;

public final class TestConfig {
    private static final String DEFAULT_BASE_URL = "https://www.periplus.com/";

    private final String baseUrl;
    private final String email;
    private final String password;
    private final String productQuery;
    private final Optional<String> productId;
    private final Optional<String> productIsbn;
    private final boolean headless;
    private final int browserTimeoutSeconds;
    private final int cartQuantity;

    private TestConfig() {
        this.baseUrl = normalizeBaseUrl(readConfig("periplus.baseUrl", "PERIPLUS_BASE_URL", DEFAULT_BASE_URL));
        this.email = requiredCredential("periplus.email", "PERIPLUS_EMAIL");
        this.password = requiredCredential("periplus.password", "PERIPLUS_PASSWORD");
        this.productQuery = readConfig("periplus.productQuery", "PERIPLUS_PRODUCT_QUERY", "Harry Potter");
        this.productId = optionalConfig("periplus.productId", "PERIPLUS_PRODUCT_ID");
        this.productIsbn = optionalConfig("periplus.productIsbn", "PERIPLUS_PRODUCT_ISBN");
        this.headless = Boolean.parseBoolean(readConfig("headless", "HEADLESS", "false"));
        this.browserTimeoutSeconds = readIntConfig("browser.timeout.seconds", "BROWSER_TIMEOUT_SECONDS", 25);
        this.cartQuantity = Math.max(1, readIntConfig("periplus.cartQuantity", "PERIPLUS_CART_QUANTITY", 2));
    }

    public static TestConfig load() {
        return new TestConfig();
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

    public String productQuery() {
        return productQuery;
    }

    public Optional<String> productId() {
        return productId;
    }

    public Optional<String> productIsbn() {
        return productIsbn;
    }

    public boolean headless() {
        return headless;
    }

    public int browserTimeoutSeconds() {
        return browserTimeoutSeconds;
    }

    public int cartQuantity() {
        return cartQuantity;
    }

    public Optional<String> chromeBinary() {
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

    private String requiredCredential(String propertyName, String envName) {
        String value = readConfig(propertyName, envName, "");
        if (value == null || value.isBlank()) {
            throw new SkipException("Set " + envName + " or -D" + propertyName + " before running the live Periplus test.");
        }
        return value;
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

    private Optional<String> optionalConfig(String propertyName, String envName) {
        String value = readConfig(propertyName, envName, "");
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private int readIntConfig(String propertyName, String envName, int defaultValue) {
        try {
            return Integer.parseInt(readConfig(propertyName, envName, Integer.toString(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String normalizeBaseUrl(String value) {
        return value.endsWith("/") ? value : value + "/";
    }
}
