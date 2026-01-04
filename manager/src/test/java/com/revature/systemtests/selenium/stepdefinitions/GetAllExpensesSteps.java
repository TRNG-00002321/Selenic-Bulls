package com.revature.systemtests.selenium.stepdefinitions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.List;


import org.openqa.selenium.By;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.revature.systemtests.selenium.utils.TestContext;


import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;


public class GetAllExpensesSteps {
    private TestContext context;
    private WebDriver driver;
    private WebDriverWait wait;

    public GetAllExpensesSteps() {
        this.context = TestContext.getInstance();
        this.driver = context.getDriver();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @When("I click the all expenses button")
    public void iClickTheAllExpensesButton() {
        WebElement allExpensesButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("show-all-expenses")));
        allExpensesButton.click();
    }

    @When("I filter the expenses by employee ID {string}")
    public void iFilterTheExpensesByEmployeeID(String employeeId) {
        WebElement employeeIdInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("employee-filter")));
        WebElement filterButton = driver.findElement(By.id("filter-by-employee"));

        employeeIdInput.clear();
        employeeIdInput.sendKeys(employeeId);
        filterButton.click();
    }

    @Then("I should see a table title {string}")
    public void iShouldSeeATableTitle(String expectedTitle) {
        WebElement titleElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h4")));
        assertEquals(expectedTitle, titleElement.getText(), "Table title should match expected");
    }

    @Then("I should see a table of all expenses with attributes titled {string}, {string}, {string}, {string}, {string}, {string}, {string}")
    public void iShouldSeeATableOfAllExpensesWithAttributesTitled(String col1, String col2, String col3, String col4, String col5, String col6, String col7) {
        WebElement tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#all-expenses-list table")));
        assertNotNull(tableElement, "All expenses table should be present");

        List<WebElement> headers = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("#all-expenses-list th")
            )
        );

        assertEquals(7, headers.size(), "There should be 7 columns in the expenses table");
        assertEquals(col1, headers.get(0).getText().trim(), "First column header should match");
        assertEquals(col2, headers.get(1).getText().trim(), "Second column header should match");
        assertEquals(col3, headers.get(2).getText().trim(), "Third column header should match");
        assertEquals(col4, headers.get(3).getText().trim(), "Fourth column header should match");
        assertEquals(col5, headers.get(4).getText().trim(), "Fifth column header should match");
        assertEquals(col6, headers.get(5).getText().trim(), "Sixth column header should match");
        assertEquals(col7, headers.get(6).getText().trim(), "Seventh column header should match");
    }

    @Then("the table should show at least one expense")
    public void theTableShouldShowAtLeastOneExpense() {
        List<WebElement> dataCells = wait.until(
            ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("#all-expenses-list td")
            )
        );
        assertFalse(dataCells.isEmpty(), "There should be at least one expense in the table");
    }

    @Then("I should see an all expenses header titled {string}")
    public void iShouldSeeAnAllExpensesHeader(String expectedHeader) {
        WebElement headerElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='all-expenses-section'] h3")));
        assertEquals(expectedHeader, headerElement.getText(), "Header text should match expected");
    }

    @Then("I should see an expenses error message {string}")
    public void iShouldSeeAnExpensesErrorMessage(String expectedMessage) {
        WebElement emptyMessage = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[id='all-expenses-list'] p")
            )
        );

        assertEquals(expectedMessage, emptyMessage.getText().trim());
    }
}