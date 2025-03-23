package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.SharedEntry;
import org.springframework.data.repository.CrudRepository;

public interface SharedEntryRepository extends CrudRepository<SharedEntry, Long> {
}
