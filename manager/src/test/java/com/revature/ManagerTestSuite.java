package com.revature;
import org.junit.platform.suite.api.*;

@Suite
// Sets a display name for the suite
@SuiteDisplayName("Manager Tests")
// Selects the specific packages to use in the Suite
@SelectPackages({
        "com.revature.controllertests",
        "com.revature.repotests",
        "com.revature.servicetests",
})
public class ManagerTestSuite
{
        
}

