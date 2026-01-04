package com.revature.systemtests.selenium.utils;


import java.time.Duration;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;

public class TestContext {
    private static TestContext instance;
    private WebDriver driver;
    private static final String BASE_URL = "http://localhost:5001";
    
    public static synchronized TestContext getInstance() {
        if (instance == null) {
            instance = new TestContext();
        }
        return instance;
    }

    private TestContext() {
        String browser = System.getProperty("browser", "chrome").toLowerCase();

        switch (browser) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(getChromeOptions());
            }
        }

        this.driver.manage().window().maximize();
        this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    public WebDriver getDriver() {
        return driver;
    }
    
    public String getBaseUrl() {
        return BASE_URL;
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless"); // Uncomment to run in headless mode
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--guest");
        return options;
    }
    

    public void tearDown() {
        if (driver != null) {
            // Take screenshot on failure
            try {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.getLifecycle().addAttachment(
                        "Screenshot", "image/png", "png", screenshot);
            } catch (Exception e) {
                // Ignore screenshot errors
            }
            driver.quit();
        }
    }
}
