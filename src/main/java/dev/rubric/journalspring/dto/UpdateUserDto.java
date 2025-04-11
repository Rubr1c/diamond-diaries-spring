package dev.rubric.journalspring.dto;

public record UpdateUserDto(Boolean enabled2fa,
                            Boolean aiAllowTitleAccess,
                            Boolean aiAllowContentAccess) {
}
