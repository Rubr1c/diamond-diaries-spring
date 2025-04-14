package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.models.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EntryRepository extends CrudRepository<Entry, Long> {
    List<Entry> findAllByUser(User user);
    List<Entry> findByDateCreatedBetweenAndUser(ZonedDateTime startDate, ZonedDateTime endDate, User user);
    Page<Entry> findAllByUserOrderByDateCreatedDesc(User user, Pageable pageable);
    List<Entry> findAllByFolder(Folder folder);
    @Query("SELECT DISTINCT e FROM Entry e JOIN e.tags t WHERE e.user = :user AND t IN :tags ORDER BY e.journalDate DESC")
    Page<Entry> findByUserAndTags(@Param("user") User user, @Param("tags") Set<Tag> tags, Pageable pageable);

    Optional<Entry> findEntryByPublicId(UUID publicId);
}
