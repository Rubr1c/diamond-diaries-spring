package dev.rubric.journalspring.controller;


import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.service.SharedEntryService;
import dev.rubric.journalspring.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shared-entry")
public class SharedEntryController {

    private final UserService userService;
    private final SharedEntryService sharedEntryService;

    public SharedEntryController(UserService userService, SharedEntryService sharedEntryService) {
        this.userService = userService;
        this.sharedEntryService = sharedEntryService;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username);
    }

    @PostMapping
    public ResponseEntity<UUID> share(@RequestBody SharedEntryDto input) {
        UUID entryId = sharedEntryService.createSharedEntry(getAuthenticatedUser(), input);

        return ResponseEntity.ok(entryId);
    }
}
