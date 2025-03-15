package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface EntryRepository extends CrudRepository<Entry, Long> {
    List<Entry> findAllByUser(User user);
}
