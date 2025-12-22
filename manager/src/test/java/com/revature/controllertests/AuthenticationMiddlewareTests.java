package com.revature.controllertests;

import com.revature.api.AuthenticationMiddleware;
import com.revature.repository.User;
import com.revature.service.AuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationMiddlewareTests {

    @Mock
    private AuthenticationService authService;

    @InjectMocks
    private AuthenticationMiddleware middleware;

    @Mock
    private Context ctx;

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

        assertDoesNotThrow(() -> middleware.validateManager().handle(ctx));
        verify(ctx).attribute("manager", user);
    }
}
