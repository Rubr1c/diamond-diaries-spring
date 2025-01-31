package dev.rubric.journalspring.models;

import jakarta.persistence.*;

@Entity
@Table(name = "tags", indexes = @Index(name = "tag_name_idx", columnList = "name"))
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    public Tag(String name) {
        this.name = name;
    }

    public Tag() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
