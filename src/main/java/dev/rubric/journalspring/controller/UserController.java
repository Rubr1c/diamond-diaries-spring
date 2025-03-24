package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.UserResponse;
import dev.rubric.journalspring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/api/v1/user")
@RestController
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> authenticatedUser() {
        logger.info("Getting authenticated user");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            logger.error("Authentication object is null");
            return ResponseEntity.notFound().build();
        }

        logger.info("Authentication found. Is authenticated: {}", authentication.isAuthenticated());

        if (!authentication.isAuthenticated()) {
            logger.error("User is not authenticated");
            return ResponseEntity.notFound().build();
        }

        try {
            User currentUser = (User) authentication.getPrincipal();
            logger.info("Found authenticated user: - ID: {}, Username: {}, Email: {}, Activated: {}",
                    currentUser.getId(),
                    currentUser.getDisplayUsername(),
                    currentUser.getEmail(),
                    currentUser.isEnabled());

            UserResponse response = new UserResponse(currentUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating user response", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
