package dev.rubric.journalspring.service;

import dev.rubric.journalspring.config.S3Service;
import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.*;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.MediaRepository;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.response.MediaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EntryService {
    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);
    private final EntryRepository entryRepository;
    private final EncryptionService encryptionService;
    private final MediaRepository mediaRepository;
    private final FolderService folderService;
    private final S3Service s3Service;

    @Autowired
    public EntryService(EntryRepository entryRepository, EncryptionService encryptionService, MediaRepository mediaRepository, FolderService folderService, S3Service s3Service) {
        this.entryRepository = entryRepository;
        this.encryptionService = encryptionService;
        this.mediaRepository = mediaRepository;
        this.s3Service = s3Service;
        this.folderService = folderService;
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
    public Entry verifyUserOwnsEntry(User user, Long entryId){
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("Entry with %d not found", entryId),
                        HttpStatus.NOT_FOUND));

        if(!entry.getUser().equals(user)){
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }
        return entry;
    }

    public void addEntryToFolder(User user, Long entryId, Long folderId) {
        Entry entry = verifyUserOwnsEntry(user, entryId);
        Folder folder = folderService.getFolder(user, folderId);

        entry.setFolder(folder);
        entryRepository.save(entry);
    }

    public void removeEntryFromFolder(User user,
                                      Long entryId) {
        Entry entry = verifyUserOwnsEntry(user, entryId);
        entry.setFolder(null);
        entryRepository.save(entry);
    }

    public List<Entry> getAllEntriesFromFolder(User user, Long folderId) {
        Folder folder = folderService.getFolder(user, folderId);

        return entryRepository.findAllByFolder(folder);
    }

    public String uploadMedia(Long entryId, MultipartFile file, MediaType mediaType) {
        Optional<Entry> entryOptional = entryRepository.findById(entryId);
        if (entryOptional.isEmpty()) {
            throw new ApplicationException("Entry not found", HttpStatus.NOT_FOUND);
        }

        Entry entry = entryOptional.get();

        try {
            String fileUrl = s3Service.uploadFile(file, mediaType);
            Media media = new Media(entry, fileUrl, mediaType);
            mediaRepository.save(media);

            logger.info("Uploaded media: {} ({} bytes) under type {}", file.getOriginalFilename(), file.getSize(), mediaType);
            return fileUrl;
        } catch (Exception e) {
            logger.error("Error uploading media: {}", e.getMessage());
            throw new ApplicationException("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteMedia(Long mediaId, Long entryId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ApplicationException("Media not found", HttpStatus.NOT_FOUND));

        if (!media.getEntry().getId().equals(entryId)) {
            throw new ApplicationException("Media does not belong to the specified entry", HttpStatus.BAD_REQUEST);
        }

        s3Service.deleteFile(media.getUrl());

        mediaRepository.delete(media);
    }

    public List<MediaResponse> getMediaByEntryId(Long entryId) {
        List<Media> mediaList = mediaRepository.findByEntryId(entryId);
        return mediaList.stream()
                .map(MediaResponse::new)
                .toList();
    }
    
}
