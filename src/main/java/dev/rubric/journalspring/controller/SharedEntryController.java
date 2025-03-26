package dev.rubric.journalspring.controller;


import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.service.SharedEntryService;
import dev.rubric.journalspring.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shared-entry")
public class SharedEntryController {

    private final SharedEntryService sharedEntryService;
    private final AuthUtil authUtil;

    public SharedEntryController(UserService userService, SharedEntryService sharedEntryService, AuthUtil authUtil) {
        this.sharedEntryService = sharedEntryService;
        this.authUtil = authUtil;
    }


    @PostMapping("/new")
    public ResponseEntity<UUID> share(@RequestBody SharedEntryDto input) {
        UUID entryId = sharedEntryService.createSharedEntry(authUtil.getAuthenticatedUser(), input);

        return ResponseEntity.ok(entryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntryResponse> get(@PathVariable UUID id) {
        Entry entry = sharedEntryService.accessSharedEntry(authUtil.getAuthenticatedUser(), id);

        return ResponseEntity.ok(new EntryResponse(entry));
    }

    @PostMapping("/{id}/add-user")
    public ResponseEntity<String> addUser(@PathVariable UUID id,
                                          @RequestBody Map<String, String> input) {
        sharedEntryService
                .addUserToSharedEntry(authUtil.getAuthenticatedUser(), id, input.get("userEmail"));

        return ResponseEntity.ok("User added to entry");
    }

    @PostMapping("/{id}/remove-user")
    public ResponseEntity<String> removeUser(@PathVariable UUID id,
                                             @RequestBody Map<String, String> input) {
        sharedEntryService
                .removeUserToSharedEntry(authUtil.getAuthenticatedUser(), id, input.get("userEmail"));

        return ResponseEntity.ok("User removed to entry");
    }
}
