package dev.rubric.journalspring.models;

import dev.rubric.journalspring.enums.MediaType;
import jakarta.persistence.*;

@Entity
@Table(name = "media")
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "entry_id")
    private Entry entry;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;

    @Column(name = "s3_key", nullable = false, length = 2048)
    private String s3Key;

    @Column(nullable = false)
    private String filename;

    public Media(Entry entry, String url, MediaType type, String filename) {
        this.entry = entry;
        this.url = url;
        this.type = type;
        this.filename = filename;
    }

    public Media() {}

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public MediaType getMediaType() {
        return type;
    }

    public void setMediaType(MediaType type) {
        this.type = type;
    }
}
