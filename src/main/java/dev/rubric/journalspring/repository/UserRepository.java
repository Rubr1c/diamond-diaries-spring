package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email IN :emails")
    List<User> findAllByEmail(@Param("emails") List<String> emails);

}
