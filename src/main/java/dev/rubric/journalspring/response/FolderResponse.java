package dev.rubric.journalspring.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rubric.journalspring.models.Folder;

import java.time.ZonedDateTime;

public class FolderResponse {
    @JsonProperty
    private final Long id;
    @JsonProperty
    private final String name;
    @JsonProperty
    private final ZonedDateTime createdAt;

    public FolderResponse(Long id,
                          String name,
                          ZonedDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public FolderResponse(Folder folder) {
        id = folder.getId();
        name = folder.getName();
        createdAt = folder.getCreatedAt();
    }

}
