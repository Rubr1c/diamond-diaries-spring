package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.FolderResponse;
import dev.rubric.journalspring.service.FolderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folder")
public class FolderController {

    private final FolderService folderService;
    private final Logger logger = LoggerFactory.getLogger(FolderController.class);

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping("/new/{name}")
    public ResponseEntity<String> createFolder(@AuthenticationPrincipal User user,
                                               @PathVariable String name) {

        logger.debug("User '{}' is creating folder named '{}'", user.getEmail(), name);

        folderService.createFolder(user, name);

        return ResponseEntity.status(HttpStatus.CREATED).body("Created folder");
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolder(@AuthenticationPrincipal User user,
                                                    @PathVariable Long id) {

        logger.debug("User '{}' is trying to get folder with id '{}'", user.getEmail(), id);

        Folder folder = folderService.getFolder(user, id);

        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders(@AuthenticationPrincipal User user) {

        logger.debug("User '{}' is fetching is all folders", user.getEmail());

        List<Folder> folders = folderService.getAllUserFolders(user);

        return ResponseEntity.ok(
                folders.stream()
                        .map(FolderResponse::new)
                        .toList()
        );
    }

    @PutMapping("/{id}/update-name/{name}")
    public ResponseEntity<String> updateFolderName(@AuthenticationPrincipal User user,
                                                   @PathVariable Long id,
                                                   @PathVariable String name) {

        logger.debug("User '{}' is updating name of folder id '{}' to {}", user.getEmail(), id, name);

        folderService.updateFolderName(user, id, name);
        return ResponseEntity.ok("Updated folder");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFolder(@AuthenticationPrincipal User user,
                                               @PathVariable Long id) {

        logger.debug("User '{}' is deleting folder with id '{}'", user.getEmail(), id);

        folderService.deleteFolder(user, id);
        return ResponseEntity.ok("Deleted folder");
    }
}
