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
    @Column(name = "entry_id")
    private Entry entry;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private MediaType type;

    public Media(Entry entry, String url, MediaType type) {
        this.entry = entry;
        this.url = url;
        this.type = type;
    }

    public Media() {}

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
}
