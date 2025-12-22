package com.revature.controllertests;

import com.revature.api.AuthenticationMiddleware;
import com.revature.repository.User;
import com.revature.service.AuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationMiddlewareTests {

    private AuthenticationService authService;
    private AuthenticationMiddleware middleware;
    private Context ctx;

    @BeforeEach
    void setUp() {
        authService = mock(AuthenticationService.class);
        middleware = new AuthenticationMiddleware(authService);
        ctx = mock(Context.class);
    }

    @Test
    void testValidateManager_Unauthenticated() throws Exception {
        when(ctx.cookie("jwt")).thenReturn("badtoken");
        when(authService.validateManagerAuthentication("badtoken")).thenReturn(Optional.empty());
        when(authService.validateJwtToken("badtoken")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedResponse.class, () -> middleware.validateManager().handle(ctx));
    }

    @Test
    void testValidateManager_NotManager() throws Exception {
        User user = new User();
        user.setRole("Employee");
        when(ctx.cookie("jwt")).thenReturn("token");
        when(authService.validateManagerAuthentication("token")).thenReturn(Optional.empty());
        when(authService.validateJwtToken("token")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenResponse.class, () -> middleware.validateManager().handle(ctx));
    }

    @Test
    void testValidateManager_Happy() throws Exception {
        User user = new User();
        user.setRole("Manager");
        when(ctx.cookie("jwt")).thenReturn("token");
        when(authService.validateManagerAuthentication("token")).thenReturn(Optional.of(user));

        // Should not throw
        assertDoesNotThrow(() -> middleware.validateManager().handle(ctx));
        verify(ctx).attribute("manager", user);
    }
}
