package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FolderRepository extends CrudRepository<Folder, Long> {

    List<Folder> getAllByUser(User user);
}
