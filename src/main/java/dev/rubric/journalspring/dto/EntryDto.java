package dev.rubric.journalspring.dto;

import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.Tag;

import java.util.Set;

public record EntryDto(String title,
                       Long folderId,
                       String content,
                       Set<Long> tagIds,
                       Integer wordCount,
                       Boolean isFavorite) { }
