package com.revature;
import org.junit.platform.suite.api.*;

// Marks the class as a test suite
@Suite
// Sets a display name for the suite in reports
@SuiteDisplayName("Manager Test Suite")
// Selects the specific classes to include in this suite
@SelectPackages({"com.revature.controllertests", 
"com.revature.servicetests", "com.revature.repotests"})
public class ManagerTestSuite
{
        
}

