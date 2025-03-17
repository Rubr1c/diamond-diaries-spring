package dev.rubric.journalspring.controller;


import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.models.Entry;
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
@RequestMapping("api/v1/media")
public class MediaController {
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private final MediaService mediaService;
    private final UserService userService;
    private final EntryService entryService;


    public MediaController(MediaService mediaService, UserService userService, EntryService entryService) {
        this.mediaService = mediaService;
        this.userService = userService;
        this.entryService = entryService;
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }

        String username = authentication.getName();
        return userService.findByUsername(username);
    }

    @GetMapping("/media/{entryId}")
    public ResponseEntity<List<MediaResponse>> getAllMediaForEntry(@PathVariable long entryId) {
        User user = getAuthenticatedUser();

        logger.info("User {} is fetching all media for journal entry with id {}", user.getId(), entryId);
        List<MediaResponse> mediaResponses = mediaService.getMediaByEntryId(entryId);

        return ResponseEntity.ok(mediaResponses);
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMedia(
            @RequestParam("entryId") Long entryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") MediaType mediaType) {

        User user = getAuthenticatedUser();
        Entry entry = entryService.getEntryEntityById(user, entryId); // This will throw if the entry isn't found

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File must not be empty");
        }

        try {
            String fileUrl = mediaService.uploadMedia(entry.getId(), file, mediaType);
            return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded successfully: " + fileUrl);
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }

    @DeleteMapping("/delete/{mediaId}")
    public ResponseEntity<?> deleteMedia(@PathVariable Long mediaId) {
        try {
            mediaService.deleteMedia(mediaId);
            return ResponseEntity.ok("Media deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting media with id: {}", mediaId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete media");
        }
    }
}
