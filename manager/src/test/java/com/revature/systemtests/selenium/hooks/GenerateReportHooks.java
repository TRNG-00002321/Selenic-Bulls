package com.revature.systemtests.selenium.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

import org.openqa.selenium.remote.RemoteWebDriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

import com.revature.systemtests.selenium.utils.TestContext;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class GenerateReportHooks {

    public static WebDriver driver;

    // ✅ Allow CI override: -DbaseUrl=http://expense-manager-test:5001
    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:5001");

    public static String CURRENT_BROWSER;

    // Cross-platform, absolute download directory
    public static final Path DOWNLOAD_DIR =
            Paths.get(System.getProperty("user.dir"), "Downloads");

    // Track ownership so we don't quit a shared driver created elsewhere
    private boolean createdLocalDriverHere = false;

    @Before
    public void setUp() {

        // -------------------------------------------------
        // Ensure download directory exists (local-only benefit)
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
        CURRENT_BROWSER = browser;

        boolean headless = "true".equalsIgnoreCase(System.getenv("HEADLESS"));

        // -------------------------------------------------
        // ✅ CI Mode: if remoteUrl is present, DO NOT create/quit per-scenario sessions.
        // Reuse the shared driver from TestContext instead.
        // -------------------------------------------------
        String remoteUrl = System.getProperty("selenium.remoteUrl", "").trim();

        try {
            if (!remoteUrl.isEmpty()) {
                // ✅ Reuse single shared driver (RemoteWebDriver) from TestContext
                driver = TestContext.getInstance().getDriver();
                System.out.println("[INFO] Using shared RemoteWebDriver from TestContext | Remote=" + remoteUrl);

            } else {
                // -------------------------------------------------
                // Local Mode: create local drivers (optional)
                // -------------------------------------------------
                switch (browser) {
                    case "chrome": {
                        WebDriverManager.chromedriver().setup();
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
                        WebDriverManager.edgedriver().setup();
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
                        WebDriverManager.firefoxdriver().setup();
                        FirefoxOptions options = new FirefoxOptions();
                        configureFirefoxDownloads(options);

                        if (headless) {
                            options.addArguments("-headless");
                        }

                        driver = new FirefoxDriver(options);
                        break;
                    }

                    default:
                        throw new IllegalArgumentException("Unsupported browser: " + browser);
                }

                createdLocalDriverHere = true;
                System.out.println("[INFO] Started LOCAL driver | Browser=" + browser + " | Headless=" + headless);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to start WebDriver (remoteUrl=" + remoteUrl + ", browser=" + browser + ")", e
            );
        }

        // -------------------------------------------------
        // Timeouts
        // -------------------------------------------------
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        try {
            driver.manage().window().maximize();
        } catch (Exception ignored) {
            // Some remote setups may ignore maximize
        }
    }

    @After
    public void tearDown() {
        // ✅ CI: do NOT quit shared driver here (prevents "session not found")
        // Local: quit only if we created the local driver in this hook
        if (createdLocalDriverHere && driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
            driver = null;
            createdLocalDriverHere = false;
        } else {
            // Optional: per-scenario cleanup without killing session
            if (driver != null) {
                try {
                    driver.manage().deleteAllCookies();
                } catch (Exception ignored) {}
            }
        }
    }

    // =================================================
    // Download Configuration Helpers (LOCAL only)
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
