package com.revature.controllertests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class GetPendingExpenseControllerTest {
    @Test
    public void testAdd() {
        int a = 2;
        int b = 3;
        int act = a + b;
        int exp = 5;

        assertEquals(exp, act);
    }
}
