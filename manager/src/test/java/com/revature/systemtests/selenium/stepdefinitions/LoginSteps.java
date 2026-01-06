package com.revature.systemtests.selenium.stepdefinitions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.revature.systemtests.selenium.utils.TestContext;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LoginSteps {

    private final TestContext context;
    private final WebDriver driver;
    private final WebDriverWait wait;

    // Track whether the browser blocked submit due to HTML5 constraint validation (required fields, etc.)
    private boolean blockedByHtml5Validation = false;
    private final List<By> invalidFieldLocators = new ArrayList<>();

    public LoginSteps() {
        this.context = TestContext.getInstance();
        this.driver = context.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        driver.get(context.getBaseUrl() + "/login.html");

        if(driver.getCurrentUrl().contains("manager.html")) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout-btn"))).click();
        }
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

        blockedByHtml5Validation = false;
        invalidFieldLocators.clear();
    }

    @When("the user logs in with username {string} and password {string}")
    public void theUserLogsInWithUsernameAndPassword(String username, String password) {
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        usernameInput.clear();
        if (username != null) {
            usernameInput.sendKeys(username);
        }

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        passwordInput.clear();
        if (password != null) {
            passwordInput.sendKeys(password);
        }

        WebElement loginButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))
        );
        loginButton.click();

        captureHtml5ValidationState();
    }

    @When("the user enters username {string}")
    public void theUserEntersUsername(String username) {
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        usernameInput.clear();
        if (username != null) {
            usernameInput.sendKeys(username);
        }
    }

    @When("the user enters password {string}")
    public void theUserEntersPassword(String password) {
        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        passwordInput.clear();
        if (password != null) {
            passwordInput.sendKeys(password);
        }
    }

    @When("the user clicks the login button")
    public void theUserClicksTheLoginButton() {
        WebElement loginButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))
        );
        loginButton.click();

        captureHtml5ValidationState();
    }

    @Then("the user should see a success message containing {string}")
    public void theUserShouldSeeASuccessMessageContaining(String expectedText) {
        // Prefer common “message/alert” elements; fall back to page source.
        WebElement messageElement = driver.findElement(By.id("login-message"));

        if (messageElement.isDisplayed()) {
            String actual = messageElement.getText().trim().toLowerCase();
            assertTrue(actual.contains(expectedText.toLowerCase()),
                    () -> "Expected success message to contain: " + expectedText + ", but was: " + actual);
        } else {
            String page = driver.getPageSource().toLowerCase();
            assertTrue(page.contains(expectedText.toLowerCase()),
                    () -> "Expected page to contain success text: " + expectedText);
        }
    }

    @Then("the user should be redirected to the manager dashboard")
    public void theUserShouldBeRedirectedToTheManagerDashboard() {
        // If the app redirects, URL typically contains "manager". If not, the dashboard should expose a stable element.
        wait.until(d -> d.getCurrentUrl().toLowerCase().contains("manager"));

        String url = driver.getCurrentUrl().toLowerCase();
        String page = driver.getPageSource().toLowerCase();

        assertTrue(url.contains("manager") || page.contains("manager") || page.contains("dashboard"),
                () -> "Expected redirect to manager dashboard. Current URL: " + driver.getCurrentUrl());
    }

    @Then("the user should remain on the login page")
    public void theUserShouldRemainOnTheLoginPage() {
        // Wait for either login form to still be present or for URL to remain on non-manager page.
        wait.until(d -> !d.getCurrentUrl().toLowerCase().contains("manager"));

        boolean hasUsername = !driver.findElements(By.id("username")).isEmpty();
        boolean hasPassword = !driver.findElements(By.id("password")).isEmpty();
        assertTrue(hasUsername && hasPassword,
                () -> "Expected to remain on login page with username/password inputs present.");
    }

    @Then("the user should see an error message containing {string}")
    public void theUserShouldSeeAnErrorMessageContaining(String expectedText) {
        // If submit was blocked by native HTML5 validation (e.g., required fields),
        // there may be NO app error message. Assert the validation state instead.
        if (blockedByHtml5Validation) {
            for (By locator : invalidFieldLocators) {
                Html5ValidationAsserts.assertRequiredViolationTriggered(driver, locator);
            }
            return;
        }

        // Otherwise, assert the application-level error message (e.g., invalid credentials from backend).
        WebElement errorElement = driver.findElement(By.id("login-message"));

        if (errorElement.isDisplayed()) {
            String actual = errorElement.getText().trim().toLowerCase();
            assertTrue(actual.contains(expectedText.toLowerCase()),
                    () -> "Expected error message to contain: " + expectedText + ", but was: " + actual);
        } else {
            String page = driver.getPageSource().toLowerCase();
            assertTrue(page.contains(expectedText.toLowerCase()),
                    () -> "Expected page to contain error text: " + expectedText);
        }
    }

    // ---------------- helpers ----------------

    private void captureHtml5ValidationState() {
        blockedByHtml5Validation = false;
        invalidFieldLocators.clear();

        JavascriptExecutor js = (JavascriptExecutor) driver;

        By username = By.id("username");
        By password = By.id("password");

        WebElement usernameEl = wait.until(ExpectedConditions.visibilityOfElementLocated(username));
        WebElement passwordEl = wait.until(ExpectedConditions.visibilityOfElementLocated(password));

        boolean usernameInvalid = Boolean.TRUE.equals((Boolean) js.executeScript(
                "return arguments[0].matches(':invalid');", usernameEl));
        boolean passwordInvalid = Boolean.TRUE.equals((Boolean) js.executeScript(
                "return arguments[0].matches(':invalid');", passwordEl));

        if (usernameInvalid || passwordInvalid) {
            blockedByHtml5Validation = true;
            if (usernameInvalid) invalidFieldLocators.add(username);
            if (passwordInvalid) invalidFieldLocators.add(password);
        }
    }
}
