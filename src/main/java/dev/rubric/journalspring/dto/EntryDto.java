package dev.rubric.journalspring.dto;

import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.Tag;

import java.util.Set;

public record EntryDto(String title, Folder folder, String content, Set<Tag> tags, Integer wordCount) { }
