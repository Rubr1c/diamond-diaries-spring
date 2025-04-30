package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.response.MediaResponse;
import dev.rubric.journalspring.service.EntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/v1/entry")
public class EntryController {
    private static final Logger logger = LoggerFactory.getLogger(EntryController.class);

    private final EntryService entryService;


    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }


    @GetMapping("/{id}")
    public ResponseEntity<EntryResponse> getEntryById(@AuthenticationPrincipal User user,
                                                      @PathVariable Long id) {

        logger.debug("User '{}' is requesting entry with id '{}'", user.getId(), id);

        EntryResponse entryResponse = new EntryResponse(entryService.getEntryById(user, id));
        return ResponseEntity.ok(entryResponse);
    }


    @GetMapping("/date/{date}")
    public ResponseEntity<List<EntryResponse>> getEntriesByDate(@AuthenticationPrincipal User user,
                                                                @PathVariable LocalDate date){
        logger.debug("User '{}' is requesting entries for date '{}'", user.getId(), date);

        List<EntryResponse> entryResponses = entryService.getEntriesByYearAndMonth(user, date)
                .stream()
                .map(EntryResponse::new)
                .toList();

        return ResponseEntity.ok(entryResponses);
    }

    @GetMapping
    public ResponseEntity<List<EntryResponse>> getUserEntries(@AuthenticationPrincipal User user,
                                                              @RequestParam Integer offset,
                                                              @RequestParam Integer amount) {


        logger.debug("User '{}' is requesting journal entries", user.getId());

        List<EntryResponse> entries = entryService.getUserEntries(user, offset, amount)
                .stream()
                .map(EntryResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(entries);
    }

    @GetMapping("/all")
    public ResponseEntity<List<EntryResponse>> getAllUserEntries(@AuthenticationPrincipal User user){
        logger.debug("User '{}' is requesting all journal entries", user.getId());

        List<EntryResponse> entries = entryService.getAllUserEntries(user)
                .stream()
                .map(EntryResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(entries);
    }


    @PostMapping("/tag")
    public ResponseEntity<List<EntryResponse>> getAllUserEntriesByTags(@AuthenticationPrincipal User user,
                                                                       @RequestBody List<String> tagNames,
                                                                       @RequestParam int offset,
                                                                       @RequestParam int size){

        logger.info("User {} is requesting all journal entries", user.getId());

        List<EntryResponse> entries = entryService.getUserEntriesByTags(user, tagNames, offset, size)
                .stream()
                .map(EntryResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(entries);
    }


    @PostMapping("/{entryId}/tag/new")
    public ResponseEntity<String> addTagsToEntry(@AuthenticationPrincipal User user,
                                                 @PathVariable Long entryId,
                                                 @RequestBody List<String> tagNames) {
        entryService.addTags(user, entryId, tagNames);
        return ResponseEntity.ok("Added tags to entry");
    }

    @DeleteMapping("/{entryId}/tag/{tagName}")
    public ResponseEntity<String> removeTagFromEntry(@AuthenticationPrincipal User user,
                                                 @PathVariable Long entryId,
                                                 @PathVariable String tagName) {
        entryService.removeTag(user, entryId, tagName);
        return ResponseEntity.ok("removed tag from entry");
    }


    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<EntryResponse> getEntryByUuid(@AuthenticationPrincipal User user,
                                                        @PathVariable UUID uuid) {
        logger.info("User {} is requesting journal entry with id {}", user.getId(), uuid);

        return ResponseEntity.ok(new EntryResponse(entryService.getEntryByUuid(user, uuid)));
    }

    @PostMapping("/new")
    public ResponseEntity<String> addEntry(@AuthenticationPrincipal User user,
                                           @RequestBody EntryDto entryDto){
        logger.debug("User '{}' is adding a new journal entry", user.getId());

        entryService.addEntry(user, entryDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Entry created successfully");
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<EntryResponse> updateEntry(@AuthenticationPrincipal User user,
                                                     @RequestBody EntryDto entryDto,
                                                     @PathVariable Long id){
        logger.debug("User '{}' is updating a journal entry with id '{}'", user.getId(), id);

        EntryResponse updateEntry =  new EntryResponse(entryService.updateEntry(user, entryDto, id));
        return ResponseEntity.ok(updateEntry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@AuthenticationPrincipal User user,
                                            @PathVariable Long id){
        logger.debug("User '{}' is deleting journal entry with id '{}'", user.getId(), id);

        entryService.deleteEntry(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/add-to-folder/{folderId}")
    public ResponseEntity<String> addToFolder(@AuthenticationPrincipal User user,
                                              @PathVariable Long id,
                                              @PathVariable Long folderId) {

        logger.debug("User '{}' is adding entry '{}' in folder '{}'", user.getEmail(), id, folderId);

        entryService.addEntryToFolder(user, id, folderId);
        return ResponseEntity.ok("Added entry to folder");
    }

    @DeleteMapping("/{id}/remove-from-folder")
    public ResponseEntity<String> removeFromFolder(@AuthenticationPrincipal User user,
                                                   @PathVariable Long id) {
        entryService.removeEntryFromFolder(user, id);

        return ResponseEntity.ok("Removed entry to folder");
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<EntryResponse>> getAllFromFolder(@AuthenticationPrincipal User user,
                                                                @PathVariable Long folderId) {

        logger.debug("User '{}' is getting all entries from folder '{}'", user.getEmail(), folderId);

        List<EntryResponse> entries = entryService
                .getAllEntriesFromFolder(user, folderId)
                .stream()
                .map(EntryResponse::new)
                .toList();

        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<List<MediaResponse>> getAllMediaForEntry(@AuthenticationPrincipal User user,
                                                                   @PathVariable Long id) {

        logger.debug("User {} is fetching all media for journal entry with id {}", user.getId(), id);

        List<MediaResponse> mediaResponses = entryService.getMediaByEntryId(user, id);

        return ResponseEntity.ok(mediaResponses);
    }

    @PostMapping(value = "/{id}/media/new", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadMediaToEntry(@AuthenticationPrincipal User user,
                                                     @PathVariable Long id,
                                                     @RequestParam("mediaType") MediaType mediaType,
                                                     @RequestParam("file") MultipartFile file) {

        logger.debug("User '{}' adding new media to entry '{}'", user.getEmail(), id);

        entryService.verifyUserOwnsEntry(user, id);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File must not be empty");
        }

        try {

            String fileUrl = entryService.uploadMedia(user, id, file, mediaType);
            return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded successfully: " + fileUrl);
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }


    @DeleteMapping("/{id}/media/{mediaId}")
    public ResponseEntity<String> deleteMediaForEntry(@AuthenticationPrincipal User user,
                                                      @PathVariable Long id,
                                                      @PathVariable Long mediaId) {

        logger.debug("User '{}' adding deleting media '{}' for entry '{}'", user.getEmail(), mediaId, id);

        try {
            entryService.verifyUserOwnsEntry(user, id);
            entryService.deleteMedia(mediaId, id);
            return ResponseEntity.ok("Media deleted successfully");
        } catch (ApplicationException e) {
            logger.error("Error deleting media: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error deleting media with id: {}", mediaId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete media");
        }
    }

    @GetMapping("/time-range")
    public ResponseEntity<List<EntryResponse>> getEntryIdsByTimeRange(
            @AuthenticationPrincipal User user,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        logger.info("User {} is requesting entries for date range: {} : {}", user.getId(), startDate, endDate);

        List<EntryResponse> result = entryService.getEntryIdsByTimeRange(user, startDate, endDate);
        return ResponseEntity.ok(result);
    }
}
