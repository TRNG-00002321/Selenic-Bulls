package com.revature;
import org.junit.platform.suite.api.*;

import com.revature.controllertests.GetPendingExpenseControllerTest;

// Marks the class as a test suite
@Suite
// Sets a display name for the suite in reports
@SuiteDisplayName("")
// Selects the specific classes to include in this suite
@SelectClasses({GetPendingExpenseControllerTest.class})
public class ManagerTestSuite
{
        
}

