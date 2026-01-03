package com.revature.unittests.controllertests;

import com.revature.api.AuthenticationMiddleware;
import com.revature.repository.User;
import com.revature.service.AuthenticationService;

import io.javalin.http.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management System")
@Feature("Controller API Middleware")
@Story("As the system, I want to enforce authentication and manager authorization so that protected endpoints cannot be accessed by unauthorized users")
class AuthenticationMiddlewareTests {

    @Mock
    private AuthenticationService authService;

    @InjectMocks
    private AuthenticationMiddleware middleware;

    @Mock
    private Context ctx;

    @BeforeAll
    static void setUpClass() {
        Allure.addAttachment(
                "Test Suite Information",
                "Unit tests for AuthenticationMiddleware (manager auth guard + authenticated manager retrieval)"
        );
        System.out.println("Starting AuthenticationMiddlewareTests suite");
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("Completed AuthenticationMiddlewareTests suite");
    }

    @Test
    @DisplayName("validateManager: unauthenticated user => UnauthorizedResponse")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies validateManager blocks access when JWT is missing/invalid and no manager can be resolved")
    @Issue("AUTH-MW-001")
    void testValidateManager_Unauthenticated() throws Exception {
        Allure.step("Arrange: ctx has bad token and authService returns empty");
        when(ctx.cookie("jwt")).thenReturn("badtoken");
        when(authService.validateManagerAuthentication("badtoken")).thenReturn(Optional.empty());
        when(authService.validateJwtToken("badtoken")).thenReturn(Optional.empty());
        Allure.addAttachment("JWT Cookie", "badtoken");

        Allure.step("Act & Assert: middleware should throw UnauthorizedResponse");
        assertThrows(UnauthorizedResponse.class, () -> middleware.validateManager().handle(ctx));

        Allure.step("Assert: manager attribute is not set");
        verify(ctx, never()).attribute(eq("manager"), any());
    }

    @Test
    @DisplayName("validateManager: authenticated but not a manager => ForbiddenResponse")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies validateManager blocks access when user is authenticated but role is not Manager")
    @Issue("AUTH-MW-002")
    void testValidateManager_NotManager() throws Exception {
        Allure.step("Arrange: ctx has token; validateManagerAuthentication empty; validateJwtToken returns a non-manager user");
        User user = new User();
        user.setRole("Employee");

        when(ctx.cookie("jwt")).thenReturn("token");
        when(authService.validateManagerAuthentication("token")).thenReturn(Optional.empty());
        when(authService.validateJwtToken("token")).thenReturn(Optional.of(user));
        Allure.addAttachment("JWT Cookie", "token");
        Allure.addAttachment("Resolved Role", "Employee");

        Allure.step("Act & Assert: middleware should throw ForbiddenResponse");
        assertThrows(ForbiddenResponse.class, () -> middleware.validateManager().handle(ctx));

        Allure.step("Assert: manager attribute is not set");
        verify(ctx, never()).attribute(eq("manager"), any());
    }

    @Test
    @DisplayName("validateManager: manager user => request proceeds and manager stored in context")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifies validateManager allows access and stores the manager in context when token is valid and role is Manager")
    @Issue("AUTH-MW-003")
    void testValidateManager_Happy() throws Exception {
        Allure.step("Arrange: ctx has token; validateManagerAuthentication returns a manager user");
        User user = new User();
        user.setRole("Manager");

        when(ctx.cookie("jwt")).thenReturn("token");
        when(authService.validateManagerAuthentication("token")).thenReturn(Optional.of(user));
        Allure.addAttachment("JWT Cookie", "token");
        Allure.addAttachment("Resolved Role", "Manager");

        Allure.step("Act & Assert: middleware should not throw");
        assertDoesNotThrow(() -> middleware.validateManager().handle(ctx));

        Allure.step("Assert: manager attribute is set");
        verify(ctx).attribute("manager", user);
    }

    @Test
    @DisplayName("getAuthenticatedManager: returns manager from context")
    @Severity(SeverityLevel.MINOR)
    @Description("Verifies getAuthenticatedManager returns the manager stored on the context")
    @Issue("AUTH-MW-004")
    void testGetAuthenticatedManager_ReturnsManagerFromContext() {
        Allure.step("Arrange: ctx.attribute('manager') returns a manager instance");
        User manager = new User();
        manager.setRole("Manager");
        when(ctx.attribute("manager")).thenReturn(manager);

        Allure.step("Act: call getAuthenticatedManager");
        User result = AuthenticationMiddleware.getAuthenticatedManager(ctx);

        Allure.step("Assert: same instance is returned");
        assertSame(manager, result);
        verify(ctx).attribute("manager");
    }

    @Test
    @DisplayName("getAuthenticatedManager: returns null when not set")
    @Severity(SeverityLevel.MINOR)
    @Description("Verifies getAuthenticatedManager returns null when the context has no 'manager' attribute")
    @Issue("AUTH-MW-005")
    void testGetAuthenticatedManager_ReturnsNullWhenNotSet() {
        Allure.step("Arrange: ctx.attribute('manager') returns null");
        when(ctx.attribute("manager")).thenReturn(null);

        Allure.step("Act: call getAuthenticatedManager");
        User result = AuthenticationMiddleware.getAuthenticatedManager(ctx);

        Allure.step("Assert: null is returned");
        assertNull(result);
        verify(ctx).attribute("manager");
    }
}
