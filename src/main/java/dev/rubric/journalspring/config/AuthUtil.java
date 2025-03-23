package dev.rubric.journalspring.config;

import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    private final UserService userService;

    public AuthUtil(UserService userService) {
        this.userService = userService;
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username);
    }
}
