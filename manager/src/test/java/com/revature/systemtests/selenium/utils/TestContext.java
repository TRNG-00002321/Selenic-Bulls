package com.revature.systemtests.selenium.utils;

import java.net.URL;
import java.time.Duration;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import org.openqa.selenium.remote.RemoteWebDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;

public class TestContext {
    private static TestContext instance;
    private WebDriver driver;

    // ✅ Allow overriding base URL from Jenkins/mvn: -DbaseUrl=...
    private static final String DEFAULT_BASE_URL = "http://localhost:5001";

    public static synchronized TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }

    private TestContext() {
        String browser = System.getProperty("browser", "chrome").toLowerCase();
        String remoteUrl = System.getProperty("selenium.remoteUrl", "").trim();

        // ✅ Also allow Jenkins to pass baseUrl as sysprop
        // In your Jenkinsfile you set BASE_URL, but your Hooks uses context.getBaseUrl()
        // so we'll read -DbaseUrl first if present.
        // (Hooks can stay unchanged)
        // Note: This method just returns the value; we won't store it here.

        try {
            if (!remoteUrl.isEmpty()) {
                // ✅ Remote execution (CI): Selenium container provides browser + driver
                MutableCapabilities options = buildRemoteOptions(browser);
                driver = new RemoteWebDriver(new URL(remoteUrl), options);

            } else {
                // ✅ Local execution (dev machine): use WebDriverManager + local browsers
                switch (browser) {
                    case "firefox" -> {
                        WebDriverManager.firefoxdriver().setup();
                        driver = new FirefoxDriver(getFirefoxOptions());
                    }
                    case "edge" -> {
                        WebDriverManager.edgedriver().setup();
                        driver = new EdgeDriver(getEdgeOptions());
                    }
                    default -> {
                        WebDriverManager.chromedriver().setup();
                        driver = new ChromeDriver(getChromeOptions());
                    }
                }
            }

            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize WebDriver (remoteUrl=" + remoteUrl + ", browser=" + browser + ")", e);
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    // ✅ Use -DbaseUrl if provided; otherwise fallback to DEFAULT_BASE_URL
    public String getBaseUrl() {
        return System.getProperty("baseUrl", DEFAULT_BASE_URL);
    }

    // ===== Local options =====
    private ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--guest");
        return options;
    }

    private FirefoxOptions getFirefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        // options.addArguments("-headless");
        return options;
    }

    private EdgeOptions getEdgeOptions() {
        EdgeOptions options = new EdgeOptions();
        return options;
    }

    // ===== Remote options (Selenium container) =====
    private MutableCapabilities buildRemoteOptions(String browser) {
        // You said "Chrome only" now, but keeping switch allows local runs with others.
        return switch (browser) {
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                // Helpful in containers; remote image usually supports it
                o.addArguments("-headless");
                yield o;
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                // Edge headless varies by image; keeping minimal
                yield o;
            }
            default -> {
                ChromeOptions o = new ChromeOptions();
                // These are commonly recommended for container stability
                o.addArguments("--no-sandbox");
                o.addArguments("--disable-dev-shm-usage");
                // Remote container can run headed; headless is typically more stable in CI
                o.addArguments("--headless=new");
                o.addArguments("--window-size=1920,1080");
                yield o;
            }
        };
    }

    public void tearDown() {
        if (driver != null) {
            try {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.getLifecycle().addAttachment("Screenshot", "image/png", "png", screenshot);
            } catch (Exception ignored) {
                // Ignore screenshot errors
            }
            try {
                driver.quit();
            } catch (Exception ignored) {}
            driver = null;
        }
        instance = null;
    }
}
