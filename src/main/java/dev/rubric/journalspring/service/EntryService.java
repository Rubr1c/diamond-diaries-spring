package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.Tag;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.response.EntryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EntryService {
    //TODO: Hash entry content
    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);
    private final EntryRepository entryRepository;


    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }
    //TODO: fix return type to EntryResponse
    public EntryResponse addEntry(User user, EntryDto details){
        Entry entry = new Entry(
                user,
                details.folder(),
                details.title(),
                details.content(),
                details.tags(),
                details.wordCount());
        entryRepository.save(entry);
        logger.info("Entry with id {} created for user {}", entry.getId(), user.getId());
        return new EntryResponse(entry);

    }

    public EntryResponse getEntryById(User user, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }
        //Test to see if Exception prints to console using postman
        return new EntryResponse(entry);
    }

    public List<Entry> getAllUserEntries(User user){
        return entryRepository.findAllByUser(user);
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

    public EntryResponse updateEntry(User user, EntryDto details, Long entryId){
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        //TODO: set Tags and Folder
        entry.setLastEdited(ZonedDateTime.now());
        entry.setContent(details.content());
        entry.setFavorite(details.isFavorite());
        entry.setTitle(details.title());
        entry.setWordCount(details.wordCount());

        entryRepository.save(entry);
        return new EntryResponse(entry);
    }

    public EntryResponse addTags(User user, Long entryId,Set<Tag> tags){
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
        return new EntryResponse(entry);
    }
    
}
