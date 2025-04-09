package dev.rubric.journalspring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TokenGeneratorService {
    private static final Logger logger = LoggerFactory.getLogger(TokenGeneratorService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MIN_PREFIX_LENGTH = 3;
    private static final int MAX_PREFIX_LENGTH = 7;
    private static final int MIN_NGRAM_SIZE = 2;
    private static final int MAX_NGRAM_SIZE = 3;

    @Value("${encryption.token.secret:defaultTokenSecret}")
    private String tokenSecret;

    @Value("${encryption.token.salt:defaultTokenSalt}")
    private String tokenSalt;


    /**
     * Generates search tokens from text, including full words, prefixes, and
     * n-grams
     * 
     * @param text The plaintext content to tokenize
     * @return A list of encrypted tokens
     */
    public List<String> generateSearchTokens(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Normalize the text - lowercase and remove punctuation
        String normalizedText = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Set<String> tokens = new HashSet<>();

        // Add full words
        tokens.addAll(extractFullWords(normalizedText));

        // Add prefix tokens for partial matching
        tokens.addAll(generatePrefixTokens(normalizedText));

        // Add n-grams for multi-word search
        tokens.addAll(generateNGrams(normalizedText));

        // Encrypt tokens deterministically
        return tokens.stream()
                .map(this::encryptToken)
                .collect(Collectors.toList());
    }

    /**
     * Extracts full words from text
     */
    private List<String> extractFullWords(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(word -> word.length() >= MIN_TOKEN_LENGTH)
                .collect(Collectors.toList());
    }

    /**
     * Generates prefix tokens to enable partial matching
     */
    private List<String> generatePrefixTokens(String text) {
        List<String> prefixTokens = new ArrayList<>();
        List<String> words = Arrays.asList(text.split("\\s+"));

        for (String word : words) {
            if (word.length() < MIN_PREFIX_LENGTH)
                continue;

            // Generate prefixes of various lengths
            for (int i = MIN_PREFIX_LENGTH; i <= Math.min(word.length(), MAX_PREFIX_LENGTH); i++) {
                prefixTokens.add("prefix:" + word.substring(0, i));
            }
        }

        return prefixTokens;
    }

    /**
     * Generates n-grams (overlapping sequences of n words)
     */
    private List<String> generateNGrams(String text) {
        List<String> nGrams = new ArrayList<>();
        String[] words = text.split("\\s+");

        if (words.length < MIN_NGRAM_SIZE)
            return nGrams;

        for (int n = MIN_NGRAM_SIZE; n <= Math.min(MAX_NGRAM_SIZE, words.length); n++) {
            for (int i = 0; i <= words.length - n; i++) {
                String nGram = IntStream.range(i, i + n)
                        .mapToObj(j -> words[j])
                        .collect(Collectors.joining(" "));
                nGrams.add("ngram:" + nGram);
            }
        }

        return nGrams;
    }

    /**
     * Encrypts a token using HMAC for deterministic encryption
     * This allows the same token to encrypt to the same value for search purposes
     */
    public String encryptToken(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            // Combine token and salt for additional security
            String tokenWithSalt = token + tokenSalt;

            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    tokenSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(tokenWithSalt.getBytes(StandardCharsets.UTF_8));

            // Convert to Base64 string
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error encrypting token", e);
            throw new RuntimeException("Error encrypting token", e);
        }
    }

    /**
     * Processes a search query to generate searchable tokens
     */
    public List<String> processSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Normalize the query text
        String normalizedText = query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Set<String> tokens = new HashSet<>();

        // Add full words
        tokens.addAll(extractFullWords(normalizedText));

        // Add prefix tokens for partial matching
        tokens.addAll(generatePrefixTokens(normalizedText));

        // Add n-grams for multi-word search if query has multiple words
        if (normalizedText.contains(" ")) {
            tokens.addAll(generateNGrams(normalizedText));
        }

        // Encrypt tokens deterministically
        return tokens.stream()
                .map(this::encryptToken)
                .collect(Collectors.toList());
    }
}