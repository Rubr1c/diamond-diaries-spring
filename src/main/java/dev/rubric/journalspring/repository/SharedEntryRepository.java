package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SharedEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedEntryRepository extends CrudRepository<SharedEntry, Long> {
    Optional<SharedEntry> getById(Long id);
    Optional<SharedEntry> getByPublicId(UUID id);

    List<SharedEntry> getAllByEntry(Entry entry);
    List<SharedEntry> getAllByEntryOrderByExpiryTimeDesc(Entry entry);
}
