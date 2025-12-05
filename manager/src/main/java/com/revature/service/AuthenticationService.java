package com.revature.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Service for handling authentication and authorization logic.
 * Uses JWT tokens stored in HTTP-only cookies for secure authentication.
 */
public class AuthenticationService {
    private final UserRepository userRepository;
    private final Algorithm jwtAlgorithm;
    private final JWTVerifier jwtVerifier;
    private static final String JWT_SECRET = "your-secret-key-change-in-production";
    private static final String JWT_ISSUER = "expense-manager";
    
    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.jwtAlgorithm = Algorithm.HMAC256(JWT_SECRET);
        this.jwtVerifier = JWT.require(jwtAlgorithm)
                .withIssuer(JWT_ISSUER)
                .build();
    }
    
    /**
     * Create a JWT token for a user.
     * @param user the user to create a token for
     * @return JWT token string
     */
    public String createJwtToken(User user) {
        return JWT.create()
                .withIssuer(JWT_ISSUER)
                .withSubject(String.valueOf(user.getId()))
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole())
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)))
                .sign(jwtAlgorithm);
    }
    
    /**
     * Validate JWT token from cookies and return the user if valid.
     * @param jwtToken the JWT token from HTTP-only cookie
     * @return Optional containing the authenticated user if valid, empty otherwise
     */
    public Optional<User> validateJwtToken(String jwtToken) {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            DecodedJWT decodedJWT = jwtVerifier.verify(jwtToken);
            String userIdStr = decodedJWT.getSubject();
            int userId = Integer.parseInt(userIdStr);
            return userRepository.findById(userId);
        } catch (JWTVerificationException | NumberFormatException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Validate authentication from Authorization header and return the user if valid.
     * @param authorizationHeader the Authorization header value (Bearer {user_id})
     * @return Optional containing the authenticated user if valid, empty otherwise
     * @deprecated Use validateJwtToken with HTTP-only cookies instead
     */
    @Deprecated
    public Optional<User> validateAuthentication(String authorizationHeader) {
        // Check if Authorization header is present and properly formatted
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        
        try {
            String userIdStr = authorizationHeader.substring("Bearer ".length());
            int userId = Integer.parseInt(userIdStr);
            return userRepository.findById(userId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Check if the user has manager role.
     * @param user the user to check
     * @return true if user is a manager, false otherwise
     */
    public boolean isManager(User user) {
        return user != null && user.isManager();
    }
    
    /**
     * Validate that the authenticated user is a manager using JWT token.
     * @param jwtToken the JWT token from HTTP-only cookie
     * @return Optional containing the manager user if valid, empty otherwise
     */
    public Optional<User> validateManagerAuthentication(String jwtToken) {
        Optional<User> userOpt = validateJwtToken(jwtToken);
        
        if (userOpt.isPresent() && isManager(userOpt.get())) {
            return userOpt;
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate that the authenticated user is a manager.
     * @param authorizationHeader the Authorization header value (Bearer {user_id})
     * @return Optional containing the manager user if valid, empty otherwise
     * @deprecated Use validateManagerAuthentication with JWT token instead
     */
    @Deprecated
    public Optional<User> validateManagerAuthenticationLegacy(String authorizationHeader) {
        Optional<User> userOpt = validateAuthentication(authorizationHeader);
        
        if (userOpt.isPresent() && isManager(userOpt.get())) {
            return userOpt;
        }
        
        return Optional.empty();
    }
    
    /**
     * Get user by ID.
     * @param userId the user ID
     * @return Optional containing the user if found
     */
    public Optional<User> getUserById(int userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Authenticate user login with username and password.
     * @param username the username
     * @param password the password
     * @return Optional containing the user if authentication successful, empty otherwise
     */
    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Simple password comparison - in production, passwords should be hashed
            if (password.equals(user.getPassword())) {
                return userOpt;
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Authenticate manager login with username and password.
     * Only allows login if user is a manager.
     * @param username the username
     * @param password the password
     * @return Optional containing the manager user if authentication successful and user is a manager, empty otherwise
     */
    public Optional<User> authenticateManager(String username, String password) {
        Optional<User> userOpt = authenticateUser(username, password);
        
        if (userOpt.isPresent() && isManager(userOpt.get())) {
            return userOpt;
        }
        
        return Optional.empty();
    }
}