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
    private boolean createdDriverHere = false;

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

        boolean headless = "true".equalsIgnoreCase(System.getenv("HEADLESS"));

        // -------------------------------------------------
        // Remote (CI) vs Local
        // -------------------------------------------------
        String remoteUrl = System.getProperty("selenium.remoteUrl", "").trim();

        try {
            if (!remoteUrl.isEmpty()) {
                // ✅ CI mode: use Selenium container
                MutableCapabilities options = buildRemoteOptions(browser, headless);
                driver = new RemoteWebDriver(new URL(remoteUrl), options);
                createdDriverHere = true;

            } else {
                // ✅ Local mode: use local browsers + WebDriverManager
                switch (browser) {
                    case "chrome": {
                        WebDriverManager.chromedriver().setup();
                        ChromeOptions options = new ChromeOptions();
                        configureChromeEdgeDownloads(options);

                        options.addArguments("--window-size=1920,1080");
                        options.addArguments("--no-sandbox");
                        options.addArguments("--disable-dev-shm-usage");

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

                        // Firefox doesn't support --window-size arg like Chromium;
                        // maximize() will handle it.
                        if (headless) {
                            options.addArguments("-headless");
                        }

                        driver = new FirefoxDriver(options);
                        break;
                    }

                    default:
                        throw new IllegalArgumentException("Unsupported browser: " + browser);
                }

                createdDriverHere = true;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to start WebDriver (remoteUrl=" + remoteUrl + ", browser=" + browser + ")", e);
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

        System.out.println("[INFO] Browser started: " + browser + " | Headless=" + headless + " | Remote=" + (!remoteUrl.isEmpty()));
        CURRENT_BROWSER = browser;
    }

    @After
    public void tearDown() {
        // ✅ Only quit if this hook created the driver
        if (createdDriverHere && driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
            driver = null;
            createdDriverHere = false;
        }
    }

    // =================================================
    // Remote capabilities
    // =================================================
    private MutableCapabilities buildRemoteOptions(String browser, boolean headless) {

        switch (browser) {
            case "firefox": {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                return o;
            }
            case "edge": {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new");
                return o;
            }
            default: {
                ChromeOptions o = new ChromeOptions();
                // Recommended for containers
                o.addArguments("--no-sandbox");
                o.addArguments("--disable-dev-shm-usage");
                o.addArguments("--window-size=1920,1080");
                if (headless) o.addArguments("--headless=new");
                return o;
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
