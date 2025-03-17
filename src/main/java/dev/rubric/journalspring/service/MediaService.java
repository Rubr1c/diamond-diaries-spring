package dev.rubric.journalspring.service;

import dev.rubric.journalspring.config.S3Service;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        if (file.isEmpty()) {
            logger.error("Media upload failed: file is empty");
            throw new ApplicationException("Cannot upload an empty file", HttpStatus.BAD_REQUEST);
        }

        String fileUrl = s3Service.uploadFile(file);
        logger.info("Media with URL {} uploaded successfully", fileUrl);
        return fileUrl;
    }

    public void deleteMedia(String fileUrl){
        if(fileUrl == null || fileUrl.isBlank()){
            logger.error("Media deletion failed: file URL is empty");
            throw new ApplicationException("Invalid file URL", HttpStatus.BAD_REQUEST);
        }

        s3Service.deleteFile(fileUrl);
        logger.info("Media with URL {} deleted successfully", fileUrl);
    }


}
