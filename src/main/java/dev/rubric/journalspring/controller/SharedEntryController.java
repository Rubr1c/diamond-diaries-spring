package dev.rubric.journalspring.controller;


import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.service.SharedEntryService;
import dev.rubric.journalspring.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shared-entry")
public class SharedEntryController {

    private final SharedEntryService sharedEntryService;

    public SharedEntryController(SharedEntryService sharedEntryService) {
        this.sharedEntryService = sharedEntryService;
    }


    @PostMapping("/new")
    public ResponseEntity<UUID> share(@AuthenticationPrincipal User user,
                                      @RequestBody SharedEntryDto input) {
        UUID entryId = sharedEntryService.createSharedEntry(user, input);

        return ResponseEntity.ok(entryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntryResponse> get(@AuthenticationPrincipal User user,
                                             @PathVariable UUID id) {

        Entry entry = sharedEntryService.accessSharedEntry(user, id);

        return ResponseEntity.ok(new EntryResponse(entry));
    }

    @PostMapping("/{id}/add-user")
    public ResponseEntity<String> addUser(@AuthenticationPrincipal User user,
                                          @PathVariable UUID id,
                                          @RequestBody Map<String, String> input) {
        sharedEntryService
                .addUserToSharedEntry(user, id, input.get("userEmail"));

        return ResponseEntity.ok("User added to entry");
    }

    @PostMapping("/{id}/remove-user")
    public ResponseEntity<String> removeUser(@AuthenticationPrincipal User user,
                                             @PathVariable UUID id,
                                             @RequestBody Map<String, String> input) {
        sharedEntryService
                .removeUserToSharedEntry(user, id, input.get("userEmail"));

        return ResponseEntity.ok("User removed to entry");
    }
}
