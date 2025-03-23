package dev.rubric.journalspring.dto;

public record  UpdatePasswordDto(String email, String verificationCode, String newPassword) {
}
