package dev.rubric.journalspring.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rubric.journalspring.models.User;

import java.time.ZonedDateTime;

public class UserResponse {
    @JsonProperty
    private String username;
    @JsonProperty
    private String email;
    @JsonProperty
    private String profilePicture;
    @JsonProperty
    private Integer streak;
    @JsonProperty
    private ZonedDateTime lastLogin;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private Boolean enabled2fa;
    @JsonProperty
    private boolean aiAllowTitleAccess;
    @JsonProperty
    private boolean aiAllowContentAccess;

    public UserResponse() {}

    public UserResponse(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.username = user.getDisplayUsername();
        this.email = user.getEmail();
        this.profilePicture = user.getProfilePicture();
        this.streak = user.getStreak();
        this.lastLogin = user.getLastLogin().orElse(null);
        this.createdAt = user.getCreatedAt();
        this.enabled2fa = user.isEnabled2fa();
        this.aiAllowTitleAccess = user.isAiAllowTitleAccess();
        this.aiAllowContentAccess = user.isAiAllowContentAccess();
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public Integer getStreak() {
        return streak;
    }

    public ZonedDateTime getLastLogin() {
        return lastLogin;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
} 