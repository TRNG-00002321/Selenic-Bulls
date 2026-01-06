package com.revature.systemtests.selenium.stepdefinitions;

import com.revature.systemtests.selenium.utils.TestContext;
import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;

public class ApproveExpenseSteps {
    // Selenium WebDriver and WebDriverWait instances
    private WebDriver driver = TestContext.getInstance().getDriver();
    private WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Step definitions for approving an expense:
    @Given("the manager is viewing the pending expenses list")
    public void navigateToPending() {
        // Clicks the button to show pending expenses: the <button id="show-pending">
        wait.until(ExpectedConditions.elementToBeClickable(By.id("show-pending"))).click();
    }
    // Step definitions for approving an expense:
    @When("the manager clicks \"Review\" for the {string} expense")
    public void clickReview(String description) {
        // Finds the "Review" button in the same row as the description
        String xpath = "//td[text()='" + description + "']/..//button[text()='Review']";
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath))).click();
    }

    @And("the manager clicks the \"Approve\" button in the modal")
    public void confirmApproval() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("approve-expense"))).click();
        // Wait for the modal to close
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("review-modal")));
    }

    @And("the manager clicks the \"Deny\" button in the modal")
    public void clickDenyInModal() {
        // Matches <button id="deny-expense"> from your HTML
        wait.until(ExpectedConditions.elementToBeClickable(By.id("deny-expense"))).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("review-modal")));
    }

    @Then("the expense should no longer appear in the pending list")
    public void verifyRemoved() {
        // Refresh the list to be sure
        driver.findElement(By.id("refresh-pending")).click();

        // Verify 'Client Dinner' is gone
        int count = driver.findElements(By.xpath("//td[text()='Travel Flight']")).size();
        assertEquals(0, count, "Expense was still found in the pending list!");
    }
    @And("the manager enters {string} into the comment field")
    public void enterComment(String comment) {
        // Matches <textarea id="review-comment"> from your HTML
        WebElement commentBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("review-comment")));
        commentBox.clear();
        commentBox.sendKeys(comment);
        System.out.println("Step: Entered comment: " + comment);
    }

    @And("the manager navigates to the \"All Expenses\" section")
    public void navigateToAllExpenses() {
        // Matches <button id="show-all-expenses">
        driver.findElement(By.id("show-all-expenses")).click();
        // It's good practice to wait for the section to become visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("all-expenses-section")));
    }

    @Then("the {string} expense should have a status of {string}")
    public void verifyStatusInAllTable(String description, String expectedStatus) {
        // This XPath finds the row by description and gets the text from that row
        // Your 'All Expenses' table might look slightly different, so we find the row first
        String rowXpath = "//div[@id='all-expenses-list']//tr[contains(., '" + description + "')]";
        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(rowXpath)));

        assertTrue(row.getText().toUpperCase().contains(expectedStatus.toUpperCase()),
                "Expected status " + expectedStatus + " not found in the row for " + description);
    }

    @And("the comment {string} should be visible in the record")
    public void verifyCommentVisible(String expectedComment) {
        // 1. Create a surgical XPath for the SPECIFIC row that is APPROVED and contains our description
        String rowXpath = "//div[@id='all-expenses-list']//tr[td[text()='Travel Flight'] and td[contains(., 'APPROVED')]]";

        // 2. Wait up to 10 seconds for that specific row to actually contain the expected comment text
        boolean isTextPresent = wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.xpath(rowXpath), expectedComment));

        // 3. Final verification and debug info
        WebElement row = driver.findElement(By.xpath(rowXpath));
        String actualRowText = row.getText();

        System.out.println("DEBUG: Verified Row Content: " + actualRowText);
        // Assert that the expected comment is indeed present
        assertTrue(isTextPresent, "Audit Trail Error! Expected comment [" + expectedComment +
                "] was not found in the row. Actual row text: " + actualRowText);
    }

}