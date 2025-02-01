package dev.rubric.journalspring.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = @Index(name = "user_email", columnList = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "google_id")
    private String googleId;

    @Column(nullable = false, length = 16)
    private String username;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Integer streak = 0;

    @Column(name = "verification_code")
    private Integer verificationCode;
    @Column(name = "code_exp")
    private LocalDateTime codeExp;
    @Column(name = "is_activated")
    private Boolean isActivated = false;

    @Column(name = "last_login")
    private ZonedDateTime lastLogin;
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @Column(name = "profile_picture", nullable = false)
    private String profilePicture;

    public User(String googleId,
                String username,
                String email,
                String password,
                ZonedDateTime createdAt,
                String profilePicture) {
        this.publicId = UUID.randomUUID();
        this.googleId = googleId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
        this.profilePicture = profilePicture;
    }

    public User() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public Optional<String> getGoogleId() {
        return Optional.ofNullable(googleId);
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStreak() {
        return streak;
    }

    public void setStreak(Integer streak) {
        this.streak = streak;
    }

    public Optional<Integer> getVerificationCode() {
        return Optional.ofNullable(verificationCode);
    }

    public void setVerificationCode(Integer verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Optional<Boolean> isActivated() {
        return Optional.ofNullable(isActivated);
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    public Optional<ZonedDateTime> getLastLogin() {
        return Optional.ofNullable(lastLogin);
    }

    public void setLastLogin(ZonedDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public LocalDateTime getCodeExp() {
        return codeExp;
    }

    public void setCodeExp(LocalDateTime codeExp) {
        this.codeExp = codeExp;
    }
}
