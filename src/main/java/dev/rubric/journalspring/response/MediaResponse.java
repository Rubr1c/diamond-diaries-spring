package dev.rubric.journalspring.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.models.Media;

public class MediaResponse {
    @JsonProperty
    private Long id;
    @JsonProperty
    private MediaType type;
    @JsonProperty
    private Long entryId;
    @JsonProperty
    private String presignedUrl;

    public MediaResponse(Media media, String presignedUrl) {
        if (media == null) {
            throw new IllegalArgumentException("Media cannot be null");
        }
        this.presignedUrl = presignedUrl;
        this.id = media.getId();
        this.type = media.getType();
        this.entryId = media.getEntry() != null ? media.getEntry().getId() : null;
    }

    public Long getId() { return id; }
    public MediaType getType() { return type; }
    public Long getEntryId() { return entryId; }
    public String getPresignedUrl() {
        return presignedUrl;
    }
}
