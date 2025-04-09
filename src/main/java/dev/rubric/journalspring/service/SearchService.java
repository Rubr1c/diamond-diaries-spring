package dev.rubric.journalspring.service;

import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SearchToken;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.SearchTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final SearchTokenRepository searchTokenRepository;
    private final TokenGeneratorService tokenGeneratorService;
    private final EncryptionService encryptionService;

    @Autowired
    public SearchService(
            SearchTokenRepository searchTokenRepository,
            TokenGeneratorService tokenGeneratorService,
            EncryptionService encryptionService) {
        this.searchTokenRepository = searchTokenRepository;
        this.tokenGeneratorService = tokenGeneratorService;
        this.encryptionService = encryptionService;
    }

    /**
     * Indexes an entry by generating and storing search tokens
     * 
     * @param entry            The entry to index
     * @param plainTextContent The plaintext content of the entry
     */
    @Transactional
    public void indexEntry(Entry entry, String plainTextContent) {
        // Generate tokens from the entry content
        List<String> tokens = tokenGeneratorService.generateSearchTokens(plainTextContent);

        // Also add tokens from the title for better search coverage
        tokens.addAll(tokenGeneratorService.generateSearchTokens(entry.getTitle()));

        logger.debug("Generated {} tokens for entry {}", tokens.size(), entry.getId());

        // Remove any existing tokens for this entry (for updates)
        searchTokenRepository.deleteAllByEntry(entry);

        // Save the new tokens
        List<SearchToken> searchTokens = tokens.stream()
                .map(token -> new SearchToken(token, entry))
                .collect(Collectors.toList());

        searchTokenRepository.saveAll(searchTokens);
        logger.debug("Saved {} search tokens for entry {}", searchTokens.size(), entry.getId());
    }

    /**
     * Searches for entries matching the given query
     * 
     * @param user  The user performing the search
     * @param query The search query
     * @return List of entries matching the query, with decrypted content
     */
    public List<Entry> search(User user, String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ApplicationException("Search query cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // Generate tokens from the search query
        List<String> queryTokens = tokenGeneratorService.processSearchQuery(query);

        if (queryTokens.isEmpty()) {
            logger.warn("No valid search tokens generated from query: {}", query);
            return Collections.emptyList();
        }

        logger.debug("Generated {} search tokens from query", queryTokens.size());

        // Find all matching tokens for this user's entries
        List<SearchToken> matchingTokens = searchTokenRepository.findByTokenValueInAndUser(queryTokens, user);

        if (matchingTokens.isEmpty()) {
            logger.debug("No matching tokens found for query: {}", query);
            return Collections.emptyList();
        }

        // Group entries by how many query tokens they match
        Map<Entry, Integer> entryRelevanceMap = new HashMap<>();

        for (SearchToken token : matchingTokens) {
            Entry entry = token.getEntry();
            entryRelevanceMap.put(entry, entryRelevanceMap.getOrDefault(entry, 0) + 1);
        }

        // Sort entries by relevance (number of matching tokens)
        List<Entry> results = entryRelevanceMap.entrySet().stream()
                .sorted(Map.Entry.<Entry, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Decrypt the content for each entry
        results.forEach(entry -> {
            String decryptedContent = encryptionService.decrypt(entry.getContent());
            entry.setContent(decryptedContent);
        });

        logger.debug("Found {} matching entries for query: {}", results.size(), query);
        return results;
    }

    /**
     * Removes all search tokens for an entry
     * Used when an entry is deleted
     * 
     * @param entry The entry to remove tokens for
     */
    @Transactional
    public void removeEntryTokens(Entry entry) {
        searchTokenRepository.deleteAllByEntry(entry);
        logger.debug("Removed all search tokens for entry {}", entry.getId());
    }
}