package dev.rubric.journalspring.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users", indexes = @Index(name = "user_email", columnList = "email"))
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id")
    private String googleId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true) // Allow null for OAuth users
    private String password;

    @Column(nullable = false)
    private Integer streak = 0;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "code_exp")
    private LocalDateTime codeExp;

    @Column(name = "is_activated")
    private Boolean isActivated = false;

    @Column(name = "last_login")
    private ZonedDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "profile_picture")
    private String profilePicture = "";

    public User(String googleId,
                String username,
                String email,
                String password,
                String profilePicture) {
        this.googleId = googleId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicture = profilePicture != null ? profilePicture : "";
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Optional<String> getGoogleId() {
        return Optional.ofNullable(googleId);
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getDisplayUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActivated != null && isActivated;
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
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

    public Optional<String> getVerificationCode() {
        return Optional.ofNullable(verificationCode);
    }

    public void setVerificationCode(String verificationCode) {
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

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture != null ? profilePicture : "";
    }

    public LocalDateTime getCodeExp() {
        return codeExp;
    }

    public void setCodeExp(LocalDateTime codeExp) {
        this.codeExp = codeExp;
    }
}
