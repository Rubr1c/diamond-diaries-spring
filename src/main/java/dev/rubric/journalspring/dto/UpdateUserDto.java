package dev.rubric.journalspring.dto;

public record UpdateUserDto(String username,
                            Boolean enabled2fa,
                            Boolean aiAllowTitleAccess,
                            Boolean aiAllowContentAccess) {
}
