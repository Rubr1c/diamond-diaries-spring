    package dev.rubric.journalspring.controller;

    import dev.rubric.journalspring.config.GetAuthenticatedUserUtility;
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

    import java.time.LocalDate;
    import java.util.List;
    import java.util.stream.Collectors;

    @RestController
    @RequestMapping("/api/v1/entries")
    public class EntryController {
        private static final Logger logger = LoggerFactory.getLogger(EntryController.class);

        private final EntryService entryService;
        private final UserService userService;
        private final GetAuthenticatedUserUtility userUtility;


        public EntryController(EntryService entryService, UserService userService, GetAuthenticatedUserUtility userUtility) {
            this.entryService = entryService;
            this.userService = userService;
            this.userUtility = userUtility;
        }


        @GetMapping("/{id}")
        public ResponseEntity<EntryResponse> getEntryById(@PathVariable Long id){
            User user = userUtility.getAuthenticatedUser();
            logger.info("User {} is requesting entry with ID: {}", user.getId(), id);


            EntryResponse entryResponse = new EntryResponse(entryService.getEntryById(user, id));
            return ResponseEntity.ok(entryResponse);
        }

        @GetMapping("/date/{date}")
        public ResponseEntity<List<EntryResponse>> getEntriesByDate(@PathVariable LocalDate date){
            User user = userUtility.getAuthenticatedUser();
            logger.info("User {} is requesting entries for date: {}", user.getId(), date);

            List<EntryResponse> entryResponses = entryService.getEntriesByYearAndMonth(user, date)
                    .stream()
                    .map(EntryResponse::new)
                    .toList();

            return ResponseEntity.ok(entryResponses);
        }

        @GetMapping
        public ResponseEntity<List<EntryResponse>> getAllUserEntries(){
            User user = userUtility.getAuthenticatedUser();
            logger.info("User {} is requesting all journal entries", user.getId());

            List<EntryResponse> entries = entryService.getAllUserEntries(user)
                    .stream()
                    .map(EntryResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entries);
        }

        @PostMapping("/add")
        public ResponseEntity<EntryResponse> addEntry(@RequestBody EntryDto entryDto){
            User user = userUtility.getAuthenticatedUser();
            logger.info("User '{}' is adding a new journal entry", user.getId());

            EntryResponse entryResponse = new EntryResponse(entryService.addEntry(user, entryDto));
            return ResponseEntity.status(HttpStatus.CREATED).body(entryResponse);
        }

        @PutMapping("/update/{entryId}")
        public ResponseEntity<EntryResponse> updateEntry(@RequestBody EntryDto entryDto, @PathVariable Long entryId){
            User user = userUtility.getAuthenticatedUser();
            logger.info("User '{}' is updating a journal entry with id {}", user.getId(), entryId);

            EntryResponse updateEntry =  new EntryResponse(entryService.updateEntry(user, entryDto, entryId));
            return ResponseEntity.ok(updateEntry);
        }

        @DeleteMapping("/delete/{entryId}")
        public ResponseEntity<Void> deleteEntry(@PathVariable long entryId){
            User user = userUtility.getAuthenticatedUser();

            logger.info("User {} is deleting journal entry with id {}", user.getId(),entryId);
            entryService.deleteEntry(user,entryId);
            return ResponseEntity.noContent().build();
        }


    }
