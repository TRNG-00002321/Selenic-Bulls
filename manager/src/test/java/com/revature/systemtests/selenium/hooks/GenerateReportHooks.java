package com.revature.systemtests.selenium.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class GenerateReportHooks {

    public static WebDriver driver;

    public static final String BASE_URL = "http://localhost:5001";

    public static String CURRENT_BROWSER;

    // Cross-platform, absolute download directory
    public static final Path DOWNLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "Downloads");

    @Before
    public void setUp() {

        // -------------------------------------------------
        // Ensure download directory exists
        // -------------------------------------------------
        try {
            Files.createDirectories(DOWNLOAD_DIR);
            System.out.println("[INFO] Download directory: " + DOWNLOAD_DIR.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create download directory", e);
        }

        // -------------------------------------------------
        // Browser selection
        // -------------------------------------------------
        String browser = System.getProperty("browser", System.getenv("BROWSER"));
        if (browser == null) {
            browser = "chrome";
        }
        browser = browser.toLowerCase();

        boolean headless = "true".equalsIgnoreCase(System.getenv("HEADLESS"));

        // -------------------------------------------------
        // Driver setup
        // -------------------------------------------------
        switch (browser) {

            case "chrome": {
                ChromeOptions options = new ChromeOptions();
                configureChromeEdgeDownloads(options);

                options.addArguments("--window-size=1920,1080");

                if (headless) {
                    options.addArguments("--headless=new");
                }

                driver = new ChromeDriver(options);
                break;
            }

            case "edge": {
                EdgeOptions options = new EdgeOptions();
                configureChromeEdgeDownloads(options);

                options.addArguments("--window-size=1920,1080");

                if (headless) {
                    options.addArguments("--headless=new");
                }

                driver = new EdgeDriver(options);
                break;
            }

            case "firefox": {
                FirefoxOptions options = new FirefoxOptions();
                configureFirefoxDownloads(options);

                options.addArguments("--window-size=1920,1080");

                if (headless) {
                    options.addArguments("-headless");
                }

                driver = new FirefoxDriver(options);
                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        // -------------------------------------------------
        // Timeouts
        // -------------------------------------------------
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().window().maximize();

        System.out.println("[INFO] Browser started: " + browser + " | Headless=" + headless);

        CURRENT_BROWSER = browser;
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    // =================================================
    // Download Configuration Helpers
    // =================================================

    private void configureChromeEdgeDownloads(Object options) {

        Map<String, Object> prefs = new HashMap<>();

        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);

        prefs.put(
                "download.default_directory",
                DOWNLOAD_DIR.toAbsolutePath().toString()
        );
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);

        if (options instanceof ChromeOptions chrome) {
            chrome.setExperimentalOption("prefs", prefs);
        }

        if (options instanceof EdgeOptions edge) {
            edge.setExperimentalOption("prefs", prefs);
        }
    }

    private void configureFirefoxDownloads(FirefoxOptions options) {

        options.addPreference("browser.download.folderList", 2);
        options.addPreference(
                "browser.download.dir",
                DOWNLOAD_DIR.toAbsolutePath().toString()
        );

        options.addPreference(
                "browser.helperApps.neverAsk.saveToDisk",
                "text/csv,application/csv,application/octet-stream"
        );

        options.addPreference("pdfjs.disabled", true);
    }
}