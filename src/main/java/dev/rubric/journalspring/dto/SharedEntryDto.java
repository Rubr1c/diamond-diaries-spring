package dev.rubric.journalspring.dto;

import dev.rubric.journalspring.models.User;

import java.time.ZonedDateTime;
import java.util.List;

public record SharedEntryDto(Long entryId,
                             List<String> allowedUsers,
                             Boolean allowAnyone) {
}
