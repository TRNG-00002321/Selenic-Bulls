package com.revature.systemtests.selenium.stepdefinitions;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.revature.systemtests.selenium.utils.TestContext;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.revature.systemtests.selenium.utils.TestContext;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LogoutSteps {

    private final TestContext context;
    private final WebDriver driver;
    private final WebDriverWait wait;

    public LogoutSteps() {
        this.context = TestContext.getInstance();
        this.driver = context.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("the user is logged in")
    public void theUserIsLoggedIn() {
        // Go to a protected page first (will render dashboard if authenticated)
        driver.get(context.getBaseUrl() + "/manager.html");

        if (!isVisible(By.id("logout-btn"))) {
            // Not logged in. Go straight to the REAL login page and log in.
            driver.get(context.getBaseUrl() + "/login.html");

            WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
            usernameInput.clear();
            usernameInput.sendKeys("manager1");

            WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
            passwordInput.clear();
            passwordInput.sendKeys("password123");

            WebElement loginButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))
            );
            loginButton.click();

            // Ensure we end up on dashboard
            driver.get(context.getBaseUrl() + "/manager.html");
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout-btn")));
    }

    @When("the user clicks the logout button")
    public void theUserClicksTheLogoutButton() {
        WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("logout-btn")));
        assertTrue(logoutButton.isDisplayed(), "Could not find a logout button/link on the page.");
        logoutButton.click();
    }

    @When("the user navigates to the manager dashboard")
    public void theUserNavigatesToTheManagerDashboard() {
        driver.get(context.getBaseUrl() + "/manager.html");
    }

    @Then("the user should be redirected to the login page")
    public void theUserShouldBeRedirectedToTheLoginPage() {
        // Login page should show the login form
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

        String url = driver.getCurrentUrl().toLowerCase();
        assertTrue(url.contains("login.html"),
                () -> "Expected to be on login.html. Current URL: " + driver.getCurrentUrl());
    }

    private boolean isVisible(By locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
