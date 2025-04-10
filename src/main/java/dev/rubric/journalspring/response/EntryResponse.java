package dev.rubric.journalspring.response;

import dev.rubric.journalspring.models.Entry;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.Tag;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntryResponse {
    @JsonProperty
    private UUID publicId;
    @JsonProperty
    private String title;
    @JsonProperty
    private String content;
    @JsonProperty
    private Set<String> tags;
    @JsonProperty
    private Integer wordCount;
    @JsonProperty
    private LocalDate journalDate;
    @JsonProperty
    private ZonedDateTime lastEdited;
    @JsonProperty
    private boolean isFavorite;
    @JsonProperty
    private Long folderId;

    public EntryResponse(Entry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry cannot be null");
        }
        this.publicId = entry.getPublicId();
        this.title = entry.getTitle();
        this.content = entry.getContent();
        this.tags = entry.getTags().stream().map(Tag::getName).collect(Collectors.toSet());
        this.wordCount = entry.getWordCount();
        this.journalDate = entry.getJournalDate();
        this.lastEdited = entry.getLastEdited().orElse(null);
        this.isFavorite = entry.isFavorite();
        this.folderId = entry.getFolder().map(Folder::getId).orElse(null);
    }

    public UUID getPublicId() { return publicId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Set<String> getTags() { return tags; }
    public Integer getWordCount() { return wordCount; }
    public LocalDate getJournalDate() { return journalDate; }
    public ZonedDateTime getLastEdited() { return lastEdited; }
    public boolean isFavorite() { return isFavorite; }
    public Long getFolderId() { return folderId; }
}
