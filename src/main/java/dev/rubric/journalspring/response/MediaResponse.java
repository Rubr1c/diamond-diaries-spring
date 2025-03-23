package dev.rubric.journalspring.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.models.Media;

public class MediaResponse {
    @JsonProperty
    private Long id;
    @JsonProperty
    private String url;
    @JsonProperty
    private MediaType type;
    @JsonProperty
    private Long entryId;

    public MediaResponse(Media media) {
        if (media == null) {
            throw new IllegalArgumentException("Media cannot be null");
        }
        this.id = media.getId();
        this.url = media.getUrl();
        this.type = media.getType();
        this.entryId = media.getEntry() != null ? media.getEntry().getId() : null;
    }

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public MediaType getType() { return type; }
    public Long getEntryId() { return entryId; }
}
