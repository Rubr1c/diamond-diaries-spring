package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class EntryService {
    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);
    private final EntryRepository entryRepository;
    private final EncryptionService encryptionService;
    private final MediaRepository mediaRepository;

    @Autowired
    public EntryService(EntryRepository entryRepository, EncryptionService encryptionService, MediaRepository mediaRepository) {
        this.entryRepository = entryRepository;
        this.encryptionService = encryptionService;
        this.mediaRepository = mediaRepository;
    }

    public Entry addEntry(User user, EntryDto details) {
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

    public Entry getEntryById(User user, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
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

    public List<Entry> getAllUserEntries(User user){

        List<Entry> entries = entryRepository.findAllByUser(user);
        
        // Decrypt all entries' content
        entries.forEach(entry -> {
            String decryptedContent = encryptionService.decrypt(entry.getContent());
            entry.setContent(decryptedContent);
        });
        logger.debug("Decrypted content for {} entries", entries.size());
        
        return new ArrayList<>(entries);
    }

    public void deleteEntry(User user, Long entryId){
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        entryRepository.deleteById(entryId);
    }

    public Entry updateEntry(User user, EntryDto details, Long entryId){
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
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

    //TODO: Fetch entries by Date e.g: Entries created in the past month

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


    public Entry addTags(User user, Long entryId,Set<Tag> tags){
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }
        entry.addTags(tags);
        entryRepository.save(entry);
        return entry;
    }


    //Fetching Entry
    public void verifyUserOwnsEntry(User user, Long entryId){
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }
    }
    
}
