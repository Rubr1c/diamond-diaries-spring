package dev.rubric.journalspring.dto;

public record UpdateUserDto(Boolean aiAllowTitleAccess,
                            Boolean aiAllowContentAccess) {
}
