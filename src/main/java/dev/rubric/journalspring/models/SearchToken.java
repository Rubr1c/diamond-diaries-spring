package dev.rubric.journalspring.models;

import jakarta.persistence.*;

@Entity
@Table(name = "search_tokens", indexes = @Index(name = "search_token_idx", columnList = "token_value"))
public class SearchToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_value", nullable = false)
    private String tokenValue;

    @ManyToOne
    @JoinColumn(name = "entry_id", nullable = false)
    private Entry entry;

    public SearchToken() {
    }

    public SearchToken(String tokenValue, Entry entry) {
        this.tokenValue = tokenValue;
        this.entry = entry;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }
}