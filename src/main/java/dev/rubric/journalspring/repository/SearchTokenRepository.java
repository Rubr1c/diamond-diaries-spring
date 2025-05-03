package dev.rubric.journalspring.repository;

import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SearchToken;
import dev.rubric.journalspring.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchTokenRepository extends CrudRepository<SearchToken, Long> {
    List<SearchToken> findByTokenValueContaining(String tokenValue);

    List<SearchToken> findByTokenValueIn(List<String> tokenValues);

    @Query("SELECT st FROM SearchToken st WHERE st.tokenValue IN :tokenValues AND st.entry.user = :user")
    List<SearchToken> findByTokenValueInAndUser(@Param("tokenValues") List<String> tokenValues,
            @Param("user") User user);

    void deleteAllByEntry(Entry entry);
}