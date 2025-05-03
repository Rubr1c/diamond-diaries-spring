package dev.rubric.journalspring.dto;

import java.util.List;

public record EntryDto(String title,
                       Long folderId,
                       String content,
                       List<String> tagNames,
                       Integer wordCount,
                       Boolean isFavorite) { }
