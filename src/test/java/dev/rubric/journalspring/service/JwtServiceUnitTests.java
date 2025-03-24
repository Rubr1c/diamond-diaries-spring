package dev.rubric.journalspring.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceUnitTests {

    private final String secretKey = "YWJjZGVmZ2hpamtsbW5vcawdwadawdafWAWFAWFAFaffHFyc3R1dnd4eXo=";
    private final Long expiration = 10000L;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secretKey, expiration);
    }

    // A simple dummy UserDetails implementation for testing
    private static class DummyUserDetails implements UserDetails {
        private final String username;

        DummyUserDetails(String username) {
            this.username = username;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.emptyList();
        }

        @Override
        public String getPassword() {
            return "dummyPassword";
        }

        @Override
        public String getUsername() {
            return username;
        }

    }

    @Test
    void generateTokenAndExtractUsername() {
        UserDetails userDetails = new DummyUserDetails("testuser");
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("testuser", extractedUsername, "Extracted username should match the original one");
    }

    @Test
    void generateTokenWithExtraClaims() {
        UserDetails userDetails = new DummyUserDetails("testuser");
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");
        String token = jwtService.generateToken(extraClaims, userDetails);

        // Use extractClaim to get the extra claim
        String role = jwtService.extractClaim(token, claims -> (String) claims.get("role"));
        assertEquals("ADMIN", role, "The role claim should be 'ADMIN'");
    }

    @Test
    void isTokenValid_ReturnsTrueForValidToken() {
        UserDetails userDetails = new DummyUserDetails("validUser");
        String token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails), "Token should be valid for the correct user");
    }

    @Test
    void isTokenValid_ReturnsFalseForWrongUser() {
        UserDetails userDetails = new DummyUserDetails("user1");
        String token = jwtService.generateToken(userDetails);
        // Use a different user details with a different username
        UserDetails differentUser = new DummyUserDetails("user2");
        assertFalse(jwtService.isTokenValid(token, differentUser), "Token should not be valid for a different user");
    }

    @Test
    void isTokenValid_ReturnsFalseForExpiredToken() throws InterruptedException {
        JwtService shortExpiryService = new JwtService(secretKey, 100L);
        UserDetails userDetails = new DummyUserDetails("testuser");
        String token = shortExpiryService.generateToken(userDetails);
        Thread.sleep(150);
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            shortExpiryService.isTokenValid(token, userDetails);
        });
    }

    @Test
    void extractClaim_InvalidTokenThrowsException() {
        String invalidToken = "this.is.not.a.valid.token";
        Exception exception = assertThrows(Exception.class, () -> {
            jwtService.extractUsername(invalidToken);
        });
        String expectedMessagePart = "JWT";
        assertTrue(exception.getMessage().contains(expectedMessagePart),
                "Exception message should contain '" + expectedMessagePart + "'");
    }
}

