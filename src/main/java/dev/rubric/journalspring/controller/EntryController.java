    package dev.rubric.journalspring.controller;

    import dev.rubric.journalspring.config.AuthUtil;
    import dev.rubric.journalspring.dto.EntryDto;
    import dev.rubric.journalspring.enums.MediaType;
    import dev.rubric.journalspring.exception.ApplicationException;
    import dev.rubric.journalspring.models.User;
    import dev.rubric.journalspring.response.EntryResponse;
    import dev.rubric.journalspring.response.MediaResponse;
    import dev.rubric.journalspring.service.EntryService;
    import dev.rubric.journalspring.service.UserService;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;

    import java.util.List;
    import java.util.stream.Collectors;

    @RestController
    @RequestMapping("/api/v1/entries")
    public class EntryController {
        private static final Logger logger = LoggerFactory.getLogger(EntryController.class);

        private final EntryService entryService;
        private final UserService userService;
        private final AuthUtil authUtil;


        public EntryController(EntryService entryService, UserService userService, AuthUtil authUtil) {
            this.entryService = entryService;
            this.userService = userService;
            this.authUtil = authUtil;
        }


        @GetMapping("/{id}")
        public ResponseEntity<EntryResponse> getEntryById(@PathVariable Long id){
            User user = authUtil.getAuthenticatedUser();
            logger.info("User {} is requesting entry with ID: {}", user.getId(), id);


            EntryResponse entryResponse = new EntryResponse(entryService.getEntryById(user, id));
            return ResponseEntity.ok(entryResponse);
        }

        @GetMapping
        public ResponseEntity<List<EntryResponse>> getAllUserEntries(){
            User user = authUtil.getAuthenticatedUser();
            logger.info("User {} is requesting all journal entries", user.getId());

            List<EntryResponse> entries = entryService.getAllUserEntries(user)
                    .stream()
                    .map(EntryResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entries);
        }

        @PostMapping("/add")
        public ResponseEntity<String> addEntry(@RequestBody EntryDto entryDto){
            User user = authUtil.getAuthenticatedUser();
            logger.info("User '{}' is adding a new journal entry", user.getId());

            EntryResponse entryResponse = new EntryResponse(entryService.addEntry(user, entryDto));
            return ResponseEntity.status(HttpStatus.CREATED).body("Entry created successfully");
        }

        @PutMapping("/update/{entryId}")
        public ResponseEntity<EntryResponse> updateEntry(@RequestBody EntryDto entryDto, @PathVariable Long entryId){
            User user = authUtil.getAuthenticatedUser();
            logger.info("User '{}' is updating a journal entry with id {}", user.getId(), entryId);

            EntryResponse updateEntry =  new EntryResponse(entryService.updateEntry(user, entryDto, entryId));
            return ResponseEntity.ok(updateEntry);
        }

        @DeleteMapping("/delete/{entryId}")
        public ResponseEntity<Void> deleteEntry(@PathVariable long entryId){
            User user = authUtil.getAuthenticatedUser();

            logger.info("User {} is deleting journal entry with id {}", user.getId(),entryId);
            entryService.deleteEntry(user,entryId);
            return ResponseEntity.noContent().build();
        }

        @PostMapping("/{id}/add-to-folder/{folderId}")
        public ResponseEntity<String> addToFolder(@PathVariable Long id,
                                                  @PathVariable Long folderId) {
            entryService.addEntryToFolder(authUtil.getAuthenticatedUser(), id, folderId);

            return ResponseEntity.ok("Added entry to folder");
        }

        @DeleteMapping("/{id}/remove-from-folder")
        public ResponseEntity<String> removeFromFolder(@PathVariable Long id) {
            entryService.removeEntryFromFolder(authUtil.getAuthenticatedUser(), id);

            return ResponseEntity.ok("Removed entry to folder");
        }

        @GetMapping("/folder/{folderId}")
        public ResponseEntity<List<EntryResponse>> getAllFromFolder(@PathVariable Long folderId) {
            List<EntryResponse> entries = entryService
                    .getAllEntriesFromFolder(authUtil.getAuthenticatedUser(), folderId)
                    .stream()
                    .map(EntryResponse::new)
                    .toList();

            return ResponseEntity.ok(entries);
        }
      
        @GetMapping("/{entryId}")
        public ResponseEntity<List<MediaResponse>> getAllMediaForEntry(@PathVariable long entryId) {
            logger.info("Received request for media of entry {}", entryId);

            User user = authUtil.getAuthenticatedUser();

            entryService.verifyUserOwnsEntry(user, entryId);

            logger.info("User {} is fetching all media for journal entry with id {}", user.getId(), entryId);

            List<MediaResponse> mediaResponses = entryService.getMediaByEntryId(entryId);

            return ResponseEntity.ok(mediaResponses);
        }

        @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<String> uploadMediaToEntry(
                @RequestParam("entryId") Long entryId,
                @RequestParam("file") MultipartFile file,
                @RequestParam("mediaType") MediaType mediaType) {

            User user = authUtil.getAuthenticatedUser();
            entryService.verifyUserOwnsEntry(user, entryId);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File must not be empty");
            }

            try {
                String fileUrl = entryService.uploadMedia(entryId, file, mediaType);
                return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded successfully: " + fileUrl);
            } catch (Exception e) {
                logger.error("Error uploading file: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
            }
        }


        @DeleteMapping("/delete/{mediaId}/entry/{entryId}")
        public ResponseEntity<String> deleteMediaForEntry(@PathVariable Long mediaId, @PathVariable Long entryId) {
            User user = authUtil.getAuthenticatedUser();

            try {
                // This will throw UNAUTHORIZED if the user doesn't own the entry
                entryService.verifyUserOwnsEntry(user, entryId);
                entryService.deleteMedia(mediaId, entryId);
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
