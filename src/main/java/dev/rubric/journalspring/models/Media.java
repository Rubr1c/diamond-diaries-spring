package dev.rubric.journalspring.models;

import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "media")
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @ManyToOne
    @Column(name = "entry_id")
    private Entry entry;

    @Column(name = "exp", nullable = false)
    private ZonedDateTime expiryTime;

    public Media(Entry entry, ZonedDateTime expiryTime) {
        this.publicId = UUID.randomUUID();
        this.entry = entry;
        this.expiryTime = expiryTime;
    }

    public Media() {}

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
}
