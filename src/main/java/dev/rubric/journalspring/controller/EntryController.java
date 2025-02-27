package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.service.EntryService;
import dev.rubric.journalspring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/entries")
public class EntryController {
    private static final Logger logger = LoggerFactory.getLogger(EntryController.class);

    private final EntryService entryService;
    private final UserService userService;


    public EntryController(EntryService entryService, UserService userService) {
        this.entryService = entryService;
        this.userService = userService;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntryResponse> getEntryById(@PathVariable Long id){
        User user = getAuthenticatedUser();
        logger.info("User {} is requesting entry with ID: {}", user.getId(), id);

        EntryResponse entryResponse = entryService.getEntryById(user, id);
        return ResponseEntity.ok(entryResponse);
    }

    @PostMapping
    public ResponseEntity<EntryResponse> addEntry(@RequestBody EntryDto entryDto){
        User user = getAuthenticatedUser();
        logger.info("User '{}' is adding a new journal entry", user.getId());

        EntryResponse entryResponse = entryService.addEntry(user, entryDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(entryResponse);
    }




}
