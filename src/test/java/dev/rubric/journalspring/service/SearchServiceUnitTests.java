package dev.rubric.journalspring.service;

import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SearchToken;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.SearchTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceUnitTests {

    @Mock
    private SearchTokenRepository searchTokenRepository;

    @Mock
    private TokenGeneratorService tokenGeneratorService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private SearchService searchService;

    @Test
    void indexEntry_Success() {
        Entry entry = new Entry();
        entry.setId(1L);
        entry.setTitle("Test Title");
        String plainTextContent = "Test content for entry";

        List<String> contentTokens = new ArrayList<>(Arrays.asList("test", "content", "entry"));
        List<String> titleTokens = new ArrayList<>(Arrays.asList("test", "title"));
        when(tokenGeneratorService.generateSearchTokens(plainTextContent)).thenReturn(contentTokens);
        when(tokenGeneratorService.generateSearchTokens(entry.getTitle())).thenReturn(titleTokens);

        searchService.indexEntry(entry, plainTextContent);

        verify(searchTokenRepository, times(1)).deleteAllByEntry(entry);

        ArgumentCaptor<List<SearchToken>> tokenListCaptor = ArgumentCaptor.forClass(List.class);
        verify(searchTokenRepository, times(1)).saveAll(tokenListCaptor.capture());

        List<SearchToken> savedTokens = tokenListCaptor.getValue();

        assertEquals(5, savedTokens.size());
        List<String> allTokens = new ArrayList<>();
        allTokens.addAll(contentTokens);
        allTokens.addAll(titleTokens);
        List<String> savedTokenValues = savedTokens.stream().map(SearchToken::getTokenValue).collect(Collectors.toList());
        assertTrue(savedTokenValues.containsAll(allTokens));
        savedTokens.forEach(token -> assertEquals(entry, token.getEntry()));
    }

    @Test
    void search_Success() {
        User user = new User();
        user.setId(1L);
        String query = "test query";

        List<String> queryTokens = Arrays.asList("test", "query");
        when(tokenGeneratorService.processSearchQuery(query)).thenReturn(queryTokens);

        Entry entry1 = new Entry();
        entry1.setId(1L);
        entry1.setContent("EncryptedContent1");

        Entry entry2 = new Entry();
        entry2.setId(2L);
        entry2.setContent("EncryptedContent2");

        SearchToken token1 = new SearchToken("test", entry1);
        SearchToken token2 = new SearchToken("query", entry1);
        SearchToken token3 = new SearchToken("test", entry2);

        List<SearchToken> matchingTokens = Arrays.asList(token1, token2, token3);
        when(searchTokenRepository.findByTokenValueInAndUser(queryTokens, user)).thenReturn(matchingTokens);

        when(encryptionService.decrypt("EncryptedContent1")).thenReturn("DecryptedContent1");
        when(encryptionService.decrypt("EncryptedContent2")).thenReturn("DecryptedContent2");

        List<Entry> result = searchService.search(user, query);

        verify(searchTokenRepository, times(1)).findByTokenValueInAndUser(queryTokens, user);

        assertEquals(2, result.size());
        assertEquals(entry1, result.get(0));
        assertEquals(entry2, result.get(1));

        assertEquals("DecryptedContent1", entry1.getContent());
        assertEquals("DecryptedContent2", entry2.getContent());
    }

    @Test
    void search_EmptyQuery_ThrowsException() {
        User user = new User();
        user.setId(1L);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            searchService.search(user, "   ");
        });
        assertEquals("Search query cannot be empty", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void search_NoMatchingTokens_ReturnsEmptyList() {
        User user = new User();
        user.setId(1L);
        String query = "unmatched query";

        List<String> queryTokens = Arrays.asList("unmatched", "query");
        when(tokenGeneratorService.processSearchQuery(query)).thenReturn(queryTokens);

        when(searchTokenRepository.findByTokenValueInAndUser(queryTokens, user)).thenReturn(Collections.emptyList());

        List<Entry> result = searchService.search(user, query);
        assertTrue(result.isEmpty());
    }

    @Test
    void removeEntryTokens_Success() {
        Entry entry = new Entry();
        entry.setId(1L);

        searchService.removeEntryTokens(entry);

        verify(searchTokenRepository, times(1)).deleteAllByEntry(entry);
    }
}

