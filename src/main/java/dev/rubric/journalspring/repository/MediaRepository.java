package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Media;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MediaRepository extends CrudRepository<Media, Long> {
    List<Media> findByEntryId(Long entryId);

    List<Media> findAllByEntryId(Long entryId);
}
