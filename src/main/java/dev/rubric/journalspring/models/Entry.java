package dev.rubric.journalspring.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "entries",
    indexes = @Index(name = "entry_user_journal_idx", columnList = "user_id, journal_date")
)
public class Entry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name ="public_id", nullable = false, unique = true)
    private UUID publicId;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToMany
    @JoinTable(
            name = "entry_id",
            joinColumns = @JoinColumn(name = "entry_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;
    @CreationTimestamp
    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;
    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    private ZonedDateTime dateCreated;
    @Column(name = "last_edited")
    private ZonedDateTime lastEdited;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    public Entry(User user,
                 Folder folder,
                 String title,
                 String content,
                 Set<Tag> tags,
                 Integer wordCount) {
        this.publicId = UUID.randomUUID();
        this.user = user;
        this.folder = folder;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.wordCount = wordCount;
    }

    public Entry() {}

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Optional<Folder> getFolder() {
        return Optional.ofNullable(folder);
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {return content;}

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public LocalDate getJournalDate() {
        return journalDate;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public Optional<ZonedDateTime> getLastEdited() {
        return Optional.ofNullable(lastEdited);
    }

    public void setLastEdited(ZonedDateTime lastEdited) {
        this.lastEdited = lastEdited;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTags(Set<Tag> tags) {
        this.tags.addAll(tags);
    }
}
