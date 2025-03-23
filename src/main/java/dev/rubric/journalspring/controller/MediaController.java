package dev.rubric.journalspring.controller;


import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.MediaResponse;
import dev.rubric.journalspring.service.EntryService;
import dev.rubric.journalspring.service.MediaService;
import dev.rubric.journalspring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private final MediaService mediaService;
    private final UserService userService;
    private final EntryService entryService;
    private final AuthUtil authUtil;


    public MediaController(MediaService mediaService, UserService userService, EntryService entryService, AuthUtil authUtil) {
        this.mediaService = mediaService;
        this.userService = userService;
        this.entryService = entryService;
        this.authUtil = authUtil;
    }


    @GetMapping("/{entryId}")
    public ResponseEntity<List<MediaResponse>> getAllMediaForEntry(@PathVariable long entryId) {
        logger.info("Received request for media of entry {}", entryId);

        User user = authUtil.getAuthenticatedUser();

        entryService.verifyUserOwnsEntry(user, entryId);

        logger.info("User {} is fetching all media for journal entry with id {}", user.getId(), entryId);

        List<MediaResponse> mediaResponses = mediaService.getMediaByEntryId(entryId);

        return ResponseEntity.ok(mediaResponses);
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadMedia(
            @RequestParam("entryId") Long entryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType) {

        User user = authUtil.getAuthenticatedUser();
        entryService.verifyUserOwnsEntry(user, entryId);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File must not be empty");
        }

        try {
            String fileUrl = mediaService.uploadMedia(entryId, file, mediaType);
            return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded successfully: " + fileUrl);
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }


    @DeleteMapping("/delete/{mediaId}/entry/{entryId}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long mediaId, @PathVariable Long entryId) {
        User user = authUtil.getAuthenticatedUser();

        try {
            // This will throw UNAUTHORIZED if the user doesn't own the entry
            entryService.verifyUserOwnsEntry(user, entryId);
            mediaService.deleteMedia(mediaId, entryId);
            return ResponseEntity.ok("Media deleted successfully");
        } catch (ApplicationException e) {
            logger.error("Error deleting media: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error deleting media with id: {}", mediaId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete media");
        }
    }

}
