package dev.rubric.journalspring.controller;


import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.service.SharedEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shared-entry")
public class SharedEntryController {

    private final SharedEntryService sharedEntryService;
    private final Logger logger = LoggerFactory.getLogger(SharedEntryController.class);

    public SharedEntryController(SharedEntryService sharedEntryService) {
        this.sharedEntryService = sharedEntryService;
    }


    @PostMapping("/new")
    public ResponseEntity<UUID> share(@AuthenticationPrincipal User user,
                                      @RequestBody SharedEntryDto input) {

        logger.debug("User '{}' is sharing entry '{}'", user.getEmail(), input.entryId());

        UUID entryId = sharedEntryService.createSharedEntry(user, input);

        return ResponseEntity.ok(entryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntryResponse> get(@AuthenticationPrincipal User user,
                                             @PathVariable UUID id) {

        logger.debug("User '{}' is requesting to access shared entry '{}'", user.getEmail(), id);

        Entry entry = sharedEntryService.accessSharedEntry(user, id);

        return ResponseEntity.ok(new EntryResponse(entry));
    }

    @PostMapping("/{id}/add-user")
    public ResponseEntity<String> addUser(@AuthenticationPrincipal User user,
                                          @PathVariable UUID id,
                                          @RequestBody Map<String, String> input) {
        String email = input.get("userEmail");

        logger.debug("User '{}' is adding user '{}' to shared entry '{}'", user.getEmail(), email, id);

        sharedEntryService
                .addUserToSharedEntry(user, id, email);

        return ResponseEntity.ok("User added to entry");
    }

    @DeleteMapping("/{id}/remove-user")
    public ResponseEntity<String> removeUser(@AuthenticationPrincipal User user,
                                             @PathVariable UUID id,
                                             @RequestBody Map<String, String> input) {

        String email = input.get("userEmail");

        logger.debug("User '{}' is removing user '{}' to shared entry '{}'", user.getEmail(), email, id);

        sharedEntryService
                .removeUserFromSharedEntry(user, id, email);

        return ResponseEntity.ok("User removed to entry");
    }
}
