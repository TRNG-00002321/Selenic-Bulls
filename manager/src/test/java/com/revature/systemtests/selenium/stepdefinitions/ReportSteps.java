package com.revature.systemtests.selenium.stepdefinitions;

import com.revature.systemtests.selenium.hooks.GenerateReportHooks;
import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ReportSteps {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public ReportSteps() {
        this.driver = GenerateReportHooks.driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("I am on the login page")
    public void iAmOnTheLoginPage() {

        driver.get(GenerateReportHooks.BASE_URL + "/login.html");

        WebElement username = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("username"))
        );

        assertTrue(
                driver.getCurrentUrl().endsWith("/login.html"),
                "User is not on the login page"
        );

        assertTrue(username.isDisplayed(), "Username field is not visible");
    }

    @When("I login with valid credentials")
    public void iLoginWithValidCredentials() {

        WebElement username = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("username"))
        );
        WebElement password = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("password"))
        );
        WebElement loginBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))
        );

        username.clear();
        username.sendKeys("manager1");

        password.clear();
        password.sendKeys("password123");

        loginBtn.click();


        // Assert successful login by observable behavior
        wait.until(ExpectedConditions.urlContains("manager.html"));
    }

    @And("I navigate to the reports page")
    public void iNavigateToTheReportsPage() {

        WebElement reportsLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("show-reports"))
        );
        reportsLink.click();

        WebElement reportsSection = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("reports-section"))
        );

        assertTrue(
                reportsSection.isDisplayed(),
                "Reports section should be visible after clicking Show Reports"
        );
    }



    // =====================================================
    // QUICK REPORTS
    // =====================================================

    @When("I generate the {string} report")
    public void iGenerateQuickReport(String report) {
        clearCsvDownloads();

        switch (report) {
            case "All Expenses":
                click("generate-all-expenses-report");
                break;

            case "Pending Expenses":
                click("generate-pending-report");
                break;

            default:
                throw new IllegalArgumentException("Unknown quick report: " + report);
        }
    }

    // =====================================================
    // EMPLOYEE REPORT
    // =====================================================

    @When("I generate an employee report for ID {string}")
    public void iGenerateEmployeeReport(String employeeId) {
        clearCsvDownloads();

        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("employee-report-id"))
        );
        input.clear();
        input.sendKeys(employeeId);

        click("generate-employee-report");
    }

    @When("the user submits the expense by employee report without specifying the employee ID")
    public void emptyEmployeeId() {
        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("employee-report-id"))
        );
        WebElement submitBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("generate-employee-report"))
        );

        input.clear();
        submitBtn.click();
    }

    // =====================================================
    // CATEGORY REPORT
    // =====================================================

    @When("I generate a category report for {string}")
    public void iGenerateCategoryReport(String category) {
        clearCsvDownloads();

        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("category-report"))
        );
        input.clear();
        input.sendKeys(category);

        click("generate-category-report");
    }

    @When("the user submits the expense by category report without specifying the category")
    public void emptyCategory() {
        WebElement input = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("category-report"))
        );
        WebElement submitBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("generate-category-report"))
        );
        input.clear();
        submitBtn.click();
    }

    // =====================================================
    // DATE RANGE REPORT
    // =====================================================

    @When("I generate a date range report from {string} to {string}")
    public void iGenerateDateRangeReport(String start, String end) {
        clearCsvDownloads();

        String browser = GenerateReportHooks.CURRENT_BROWSER;
        if (browser.equals("firefox")) {
            driver.findElement(By.id("start-date")).sendKeys(normalizeDate(start));
            driver.findElement(By.id("end-date")).sendKeys(normalizeDate(end));
        }
        else {
            driver.findElement(By.id("start-date")).sendKeys(start);
            driver.findElement(By.id("end-date")).sendKeys(end);
        }


        click("generate-date-range-report");
    }


    @When("the user submits the expense by date range report without specifying start and end dates")
    public void emptyDateRange() {
        WebElement start = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("start-date"))
        );
        WebElement end = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("end-date"))
        );
        WebElement submitBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("generate-date-range-report"))
        );
        start.clear();
        end.clear();
        submitBtn.click();
    }

    // =====================================================
    // ASSERTIONS
    // =====================================================

    @Then("a CSV file should be downloaded")
    public void csvFileShouldBeDownloaded() {
        File csv = waitForCsvDownload(30);
        assertNotNull(csv, "CSV file was not downloaded");
        assertTrue(csv.length() > 0, "CSV file is empty");
    }

    @And("the CSV file should contain the expected data")
    public void csvShouldContainExpectedData() throws Exception {
        File csv = getLatestCsv();
        assertNotNull(csv, "CSV file not found");

        String content = Files.readString(csv.toPath());

        assertFalse(content.isBlank(), "CSV is empty");
        assertTrue(
                content.contains("Date") ||
                        content.contains("Amount") ||
                        content.contains("Status"),
                "CSV missing expected headers"
        );
    }

    @Then("an employee ID required validation message is shown")
    public void requiredIdValidationMessageShown() {
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("report-message")));
        assertTrue(error.getText().contains("Please enter an employee ID"));
    }

    @Then("a category required validation message is shown")
    public void categoryRequiredValidationMessageShown() {
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("report-message")));
        assertTrue(error.getText().contains("Please enter a category"));
    }

    @Then("a date required validation message is shown")
    public void datesRequiredValidationMessageShown() {
        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("report-message")));
        assertTrue(error.getText().contains("Please select both start and end dates"));
    }

    @And("the user remains on the report page")
    public void userRemainsOnReportPage() {
        WebElement reportSection = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("reports-section")));
        assertTrue(reportSection.isDisplayed(), "Reports section should still be visible");
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private void click(String id) {
            wait.until(ExpectedConditions.elementToBeClickable(By.id(id))).click();
        }

        private void clearCsvDownloads() {
            File dir = GenerateReportHooks.DOWNLOAD_DIR.toFile();

            File[] files = dir.listFiles(f -> f.getName().endsWith(".csv"));
        if (files != null) {
            for (File f : files) f.delete();
        }
    }

    private File waitForCsvDownload(int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds; i++) {
            File csv = getLatestCsv();
            if (csv != null && csv.exists() && csv.length() > 0) {
                return csv;
            }
            sleep(1000);
        }
        return null;
    }

    private File getLatestCsv() {
        File dir = GenerateReportHooks.DOWNLOAD_DIR.toFile();

        File[] files = dir.listFiles(f -> f.getName().endsWith(".csv"));
        if (files == null || files.length == 0) return null;

        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) {
                latest = f;
            }
        }
        return latest;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String normalizeDate(String date) {
        // converts mm/dd/yyyy → yyyy-MM-dd
        String[] parts = date.split("/");
        return parts[2] + "-" + parts[0] + "-" + parts[1];
    }

}
