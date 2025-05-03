package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Tag;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TagRepository extends CrudRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
