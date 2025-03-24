package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.response.FolderResponse;
import dev.rubric.journalspring.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folder")
public class FolderController {

    private final FolderService folderService;
    private final AuthUtil authUtil;

    public FolderController(FolderService folderService, AuthUtil authUtil) {
        this.folderService = folderService;
        this.authUtil = authUtil;
    }

    @PostMapping("/new/{name}")
    public ResponseEntity<String> createFolder(@PathVariable String name) {
        folderService.createFolder(authUtil.getAuthenticatedUser(), name);

        return ResponseEntity.ok("Created folder");
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolder(@PathVariable Long id) {
        Folder folder = folderService.getFolder(authUtil.getAuthenticatedUser(), id);

        return ResponseEntity.ok(new FolderResponse(folder));
    }

    @GetMapping("/all")
    public ResponseEntity<List<FolderResponse>> getAllFolders() {
        List<Folder> folders = folderService.getAllUserFolders(authUtil.getAuthenticatedUser());

        return ResponseEntity.ok(
                folders.stream()
                        .map(FolderResponse::new)
                        .toList()
        );
    }

    @PutMapping("/{id}/update-name/{name}")
    public ResponseEntity<String> updateFolderName(@PathVariable Long id,
                                                   @PathVariable String name) {
        folderService.updateFolderName(authUtil.getAuthenticatedUser(), id, name);
        return ResponseEntity.ok("Updated folder");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFolder(@PathVariable Long id) {
        folderService.deleteFolder(authUtil.getAuthenticatedUser(), id);

        return ResponseEntity.ok("Deleted folder");
    }
}
