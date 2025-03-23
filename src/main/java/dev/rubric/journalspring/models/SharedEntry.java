package dev.rubric.journalspring.models;

import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.*;

@Entity
@Table(name = "shared_entries")
public class SharedEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @ManyToOne
    @JoinColumn(name = "entry_id")
    private Entry entry;

    @Column(name = "expiry_time", nullable = false)
    private ZonedDateTime expiryTime;

    @ManyToMany
    @JoinTable(
            name = "allowed_users",
            joinColumns = @JoinColumn(name = "shared_entry_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> allowedUsers = new ArrayList<>();

    @Column(name = "allow_anyone", nullable = false)
    private boolean allowAnyone;

    public SharedEntry(Entry entry,
                       ZonedDateTime expiryTime,
                       List<User> allowedUsers,
                       boolean allowAnyone) {
        this.publicId = UUID.randomUUID();
        this.entry = entry;
        this.expiryTime = expiryTime;
        this.allowedUsers = allowedUsers;
        this.allowAnyone = allowAnyone;
    }

    public SharedEntry() {}

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

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public ZonedDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(ZonedDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public List<User> getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(List<User> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }
}
