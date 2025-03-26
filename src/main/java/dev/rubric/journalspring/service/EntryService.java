package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.MediaRepository;
import dev.rubric.journalspring.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EntryService {
    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);
    private final EntryRepository entryRepository;
    private final EncryptionService encryptionService;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    @Autowired
    public EntryService(EntryRepository entryRepository, EncryptionService encryptionService,
            MediaRepository mediaRepository, UserRepository userRepository) {
        this.entryRepository = entryRepository;
        this.encryptionService = encryptionService;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    public Entry addEntry(User user, EntryDto details) {
        // Update user streak based on entry dates
        updateUserStreak(user);

        // Encrypt the content before saving
        String encryptedContent = encryptionService.encrypt(details.content());
        logger.debug("Content encrypted for new entry");

        Entry entry = new Entry(
                user,
                details.folder(),
                details.title(),
                encryptedContent,
                details.tags(),
                details.wordCount());

        entryRepository.save(entry);
        logger.info("Entry with id {} created for user {}", entry.getId(), user.getId());

        // Decrypt for the response
        entry.setContent(details.content()); // Use original content for response
        return entry;
    }

    /**
     * Updates the user's streak based on the date of their most recent entry.
     * - If the new entry is one day after the previous entry: increment streak
     * - If the new entry is on the same day as the previous entry: do nothing
     * Note: Streak reset logic (for gaps > 1 day) is handled in AuthService when
     * the user logs in
     */
    private void updateUserStreak(User user) {
        LocalDate today = LocalDate.now();

        // Find the most recent entry for this user
        List<Entry> recentEntries = entryRepository.findAllByUserOrderByJournalDateDesc(user, PageRequest.of(0, 1))
                .getContent();

        if (recentEntries.isEmpty()) {
            // This is the user's first entry, set streak to 1
            user.setStreak(1);
            userRepository.save(user);
            logger.info("First entry for user {}, set streak to 1", user.getId());
            return;
        }

        LocalDate lastEntryDate = recentEntries.get(0).getJournalDate();

        if (today.equals(lastEntryDate)) {
            // Entry created on the same day, do nothing with streak
            logger.debug("Entry created on same day for user {}, streak remains {}", user.getId(), user.getStreak());
        } else if (today.equals(lastEntryDate.plusDays(1))) {
            // Entry created exactly one day after the previous entry, increment streak
            Integer newStreak = user.getStreak() + 1;
            user.setStreak(newStreak);
            userRepository.save(user);
            logger.info("Incremented streak for user {} to {}", user.getId(), newStreak);
        }
        // Note: We don't handle the reset case here as it's now done in AuthService
    }

    public Entry getEntryById(User user, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        // Decrypt the content before returning
        String decryptedContent = encryptionService.decrypt(entry.getContent());
        entry.setContent(decryptedContent);
        logger.debug("Content decrypted for entry id: {}", entryId);

        return entry;
    }

    public List<Entry> getAllUserEntries(User user) {

        List<Entry> entries = entryRepository.findAllByUser(user);

        // Decrypt all entries' content
        entries.forEach(entry -> {
            String decryptedContent = encryptionService.decrypt(entry.getContent());
            entry.setContent(decryptedContent);
        });
        logger.debug("Decrypted content for {} entries", entries.size());

        return new ArrayList<>(entries);
    }

    public List<Entry> getUserEntries(User user, int offset, int count) {
        PageRequest pageRequest = PageRequest.of(offset, count, Sort.by(Sort.Direction.DESC, "journalDate"));
        List<Entry> entries = entryRepository.findAllByUserOrderByJournalDateDesc(user, pageRequest).getContent();

        // Decrypt all entries' content
        entries.forEach(entry -> {
            String decryptedContent = encryptionService.decrypt(entry.getContent());
            entry.setContent(decryptedContent);
        });

        logger.debug("Decrypted content for {} entries", entries.size());

        return entries;
    }

    public void deleteEntry(User user, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        entryRepository.deleteById(entryId);
    }

    public Entry updateEntry(User user, EntryDto details, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        // Encrypt the updated content
        String encryptedContent = encryptionService.encrypt(details.content());
        logger.debug("Content encrypted for updated entry id: {}", entryId);

        entry.setLastEdited(ZonedDateTime.now());
        entry.setContent(encryptedContent);
        entry.setFavorite(details.isFavorite());
        entry.setTitle(details.title());
        entry.setWordCount(details.wordCount());

        entryRepository.save(entry);

        // Decrypt for the response
        entry.setContent(details.content()); // Use original content for response
        return entry;
    }

    public List<Entry> getEntriesByYearAndMonth(User user, LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new ApplicationException("Date cannot be in the future", HttpStatus.BAD_REQUEST);
        }

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDate endOfMonthDate = YearMonth.of(date.getYear(), date.getMonth()).atEndOfMonth();

        ZonedDateTime startDate = date.atStartOfDay(zoneId);
        ZonedDateTime endDate = endOfMonthDate.atTime(23, 59, 59).atZone(zoneId);

        List<Entry> entries = entryRepository.findByDateCreatedBetweenAndUser(startDate, endDate, user);

        if (entries == null || entries.isEmpty()) {
            throw new ApplicationException(
                    String.format("No entries found for %d-%02d", date.getYear(), date.getMonthValue()),
                    HttpStatus.NOT_FOUND);
        }

        return entries;
    }

    public Map<LocalDate, List<Long>> getEntryIdsByTimeRange(User user, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ApplicationException("Start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        ZoneId zoneId = ZoneId.systemDefault();

        ZonedDateTime startDateTime = startDate.atStartOfDay(zoneId);
        ZonedDateTime endDateTime = endDate.atTime(23, 59, 59).atZone(zoneId);

        List<Entry> entries = entryRepository.findByDateCreatedBetweenAndUser(startDateTime, endDateTime, user);

        if (entries == null || entries.isEmpty()) {
            throw new ApplicationException(
                    String.format("No entries found between %s and %s",
                            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)),
                    HttpStatus.NOT_FOUND);
        }

        return entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getDateCreated().toLocalDate(),
                        Collectors.mapping(Entry::getId, Collectors.toList())));

    }

    public Entry addTags(User user, Long entryId, Set<Tag> tags) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }
        entry.addTags(tags);
        entryRepository.save(entry);
        return entry;
    }

    // Fetching Entry
    public void verifyUserOwnsEntry(User user, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }
    }

}
