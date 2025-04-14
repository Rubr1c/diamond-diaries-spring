package dev.rubric.journalspring.dto;

import java.util.Set;

public record EntryDto(String title,
                       Long folderId,
                       String content,
                       Set<Long> tagIds,
                       Integer wordCount,
                       Boolean isFavorite) { }
