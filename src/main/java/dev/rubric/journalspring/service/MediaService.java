package dev.rubric.journalspring.service;

import dev.rubric.journalspring.config.S3Service;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.Media;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.MediaRepository;
import dev.rubric.journalspring.response.MediaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class MediaService {
    private final MediaRepository mediaRepository;
    private final EntryRepository entryRepository;
    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    private final S3Service s3Service;

    public MediaService(MediaRepository mediaRepository, EntryRepository entryRepository, S3Service s3Service) {
        this.mediaRepository = mediaRepository;
        this.entryRepository = entryRepository;
        this.s3Service = s3Service;
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
