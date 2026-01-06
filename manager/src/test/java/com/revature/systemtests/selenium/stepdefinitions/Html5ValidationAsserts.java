package com.revature.systemtests.selenium.stepdefinitions;
import static org.junit.jupiter.api.Assertions.*;

import org.openqa.selenium.*;

public final class Html5ValidationAsserts {

    private Html5ValidationAsserts() {}

    public static void assertRequiredViolationTriggered(WebDriver driver, By fieldLocator) {
        WebElement field = driver.findElement(fieldLocator);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        Boolean invalid = (Boolean) js.executeScript("return arguments[0].matches(':invalid');", field);
        assertTrue(Boolean.TRUE.equals(invalid), "Expected field to be invalid (required) after submit.");

        Boolean valueMissing = (Boolean) js.executeScript("return arguments[0].validity.valueMissing;", field);
        assertTrue(Boolean.TRUE.equals(valueMissing), "Expected validity.valueMissing == true for required field.");

        String message = (String) js.executeScript("return arguments[0].validationMessage;", field);
        assertNotNull(message, "Expected validationMessage not null.");
        assertFalse(message.trim().isEmpty(), "Expected validationMessage not empty (native bubble would show).");

        // Optional: most browsers focus the first invalid element on submit
        WebElement active = driver.switchTo().activeElement();
        assertEquals(field, active, "Expected browser to focus the invalid required field.");
    }
}
