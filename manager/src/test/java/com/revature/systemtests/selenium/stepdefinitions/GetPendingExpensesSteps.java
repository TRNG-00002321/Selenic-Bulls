package com.revature.systemtests.selenium.stepdefinitions;
import io.cucumber.java.en.*;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.revature.systemtests.selenium.utils.TestContext;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GetPendingExpensesSteps {
    private TestContext context;
    private WebDriver driver;
    private WebDriverWait wait;

    public GetPendingExpensesSteps() {
        this.context = TestContext.getInstance();
        this.driver = context.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @When("I click the pending expenses button")
    public void iClickThePendingExpensesButton() {
        WebElement pendingExpensesButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("show-pending")));
        pendingExpensesButton.click();
    }

    @Then("I should see a table of pending expenses with attributes titled {string}, {string}, {string}, {string}, {string}")
    public void iShouldSeeATableOfPendingExpensesWithAttributesTitled(String col1, String col2, String col3, String col4, String col5) {
        WebElement tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#pending-expenses-list table")));
        assertNotNull(tableElement, "Pending expenses table should be present");

        List<WebElement> headers = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("#pending-expenses-list th")
            )
        );

        assertEquals(5, headers.size());
        assertEquals(col1, headers.get(0).getText().trim(), "First column header should match");
        assertEquals(col2, headers.get(1).getText().trim(), "Second column header should match");
        assertEquals(col3, headers.get(2).getText().trim(), "Third column header should match");
        assertEquals(col4, headers.get(3).getText().trim(), "Fourth column header should match");
        assertEquals(col5, headers.get(4).getText().trim(), "Fifth column header should match");
    }

    @Then("the table should show at least one pending expense")
    public void theTableShouldShowAtLeastOnePendingExpense() {
        List<WebElement> dataCells = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("#pending-expenses-list td")
            )
        );

        assertFalse(dataCells.isEmpty(), "There should be at least one pending expense in the table, but table is empty");
    }

    @Then("I should see a pending expenses header titled {string}")
    public void iShouldSeeAPendingExpensesHeader(String expectedHeader) {
        WebElement headerElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='pending-expenses-section'] h3")));
        assertEquals(expectedHeader, headerElement.getText(), "Header text should match expected");
    }

    @Then("I should see a pending expenses error message {string}")
    public void iShouldSeeAPendingExpenseErrorMessage(String expectedMessage) {
        WebElement emptyMessage = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[id='pending-expenses-list'] p")
            )
        );

        assertEquals(expectedMessage, emptyMessage.getText().trim());
    }
}