package com.openway.periplus.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

public final class FailureArtifacts {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private FailureArtifacts() {
    }

    public static void captureScreenshot(WebDriver driver, ITestResult result) {
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return;
        }

        try {
            Path screenshotDir = Path.of("build", "reports", "screenshots");
            Files.createDirectories(screenshotDir);

            String testName = result.getMethod().getMethodName().replaceAll("[^A-Za-z0-9._-]", "_");
            Path target = screenshotDir.resolve(testName + "-" + LocalDateTime.now().format(TIMESTAMP) + ".png");
            File source = screenshotDriver.getScreenshotAs(OutputType.FILE);

            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            result.setAttribute("screenshot", target.toAbsolutePath().toString());
        } catch (RuntimeException | java.io.IOException ignored) {
            // Screenshot capture must never hide the real test failure.
        }
    }
}
