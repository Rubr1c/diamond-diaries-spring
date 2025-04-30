package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllTags(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tagService.getAllTagNames());
    }
}
