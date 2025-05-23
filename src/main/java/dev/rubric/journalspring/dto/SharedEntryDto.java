package dev.rubric.journalspring.dto;

import java.util.List;

public record SharedEntryDto(Long entryId,
                             List<String> allowedUsers,
                             Boolean allowAnyone) {
}
