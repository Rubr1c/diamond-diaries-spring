package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.service.EntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/entry/search")
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final EntryService entryService;

    @Autowired
    public SearchController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping
    public ResponseEntity<List<EntryResponse>> searchEntries(
            @AuthenticationPrincipal User user,
            @RequestParam("query") String query) {

        logger.debug("Search request from user '{}' with query '{}'", user.getId(), query);

        List<Entry> searchResults = entryService.searchEntries(user, query);

        List<EntryResponse> responseList = searchResults.stream()
                .map(EntryResponse::new)
                .collect(Collectors.toList());

        logger.debug("Returning {} search results for query '{}'", responseList.size(), query);

        return ResponseEntity.ok(responseList);
    }
}