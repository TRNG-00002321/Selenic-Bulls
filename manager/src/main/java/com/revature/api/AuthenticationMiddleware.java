package com.revature.api;

import com.revature.repository.User;
import com.revature.service.AuthenticationService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.ForbiddenResponse;

import java.util.Optional;

/**
 * Authentication middleware for validating manager access.
 * Ensures only authenticated managers can access protected endpoints.
 */
public class AuthenticationMiddleware {
    private final AuthenticationService authenticationService;
    
    public AuthenticationMiddleware(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    
    /**
     * Handler to validate manager authentication before processing requests.
     * Uses JWT tokens from HTTP-only cookies.
     * @return Javalin Handler for authentication middleware
     */
    public Handler validateManager() {
        return ctx -> {
            String jwtToken = ctx.cookie("jwt");
            
            Optional<User> managerOpt = authenticationService.validateManagerAuthentication(jwtToken);
            
            if (managerOpt.isEmpty()) {
                // Check if user is authenticated but not a manager
                Optional<User> userOpt = authenticationService.validateJwtToken(jwtToken);
                if (userOpt.isPresent()) {
                    throw new ForbiddenResponse("Access denied - managers only");
                } else {
                    throw new UnauthorizedResponse("Authentication required");
                }
            }
            
            // Store the authenticated manager in the context for use in handlers
            ctx.attribute("manager", managerOpt.get());
        };
    }
    
    /**
     * Get the authenticated manager from the context.
     * @param ctx Javalin context
     * @return the authenticated manager user
     */
    public static User getAuthenticatedManager(Context ctx) {
        return ctx.attribute("manager");
    }
}