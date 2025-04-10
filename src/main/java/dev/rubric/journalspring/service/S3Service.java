package dev.rubric.journalspring.service;

import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // URL expiration time in minutes
    @Value("${aws.s3.url-expiration:15}")
    private int urlExpirationMinutes;


    public S3Service(@Value("${aws.accessKeyId}") String accessKey,
                     @Value("${aws.secretAccessKey}") String secretKey) {
        Region region = Region.US_EAST_1;
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }


    /**
     * Upload profile picture (public access)
     */
    public String uploadProfilePicture(MultipartFile file) {
        try {
            String folder = "profile-pictures/";
            String key = folder + UUID.randomUUID() + "-" + file.getOriginalFilename();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    // Public read access for profile pictures
                    .acl("public-read")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            // Return the full public URL for profile pictures
            return "https://" + bucketName + ".s3.amazonaws.com/" + key;
        } catch (IOException e) {
            throw new ApplicationException("Failed to read file data", HttpStatus.BAD_REQUEST);
        } catch (S3Exception e) {
            throw new ApplicationException("Failed to upload profile picture to S3: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Upload a file to S3 with private access
     * @return S3 key (not a public URL)
     */
    public String uploadFile(MultipartFile file, MediaType mediaType) {
        try {
            String folder;
            if (mediaType == null) {
                folder = "files/";
            } else {
                folder = switch (mediaType) {
                    case IMAGE -> "photos/";
                    case VIDEO -> "videos/";
                    default -> "files/";
                };
            }


            String key = folder + UUID.randomUUID() + "-" + file.getOriginalFilename();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    // Private access for journal media
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            // Return the S3 key for private files
            return key;
        } catch (IOException e) {
            throw new ApplicationException("Failed to read file data", HttpStatus.BAD_REQUEST);
        } catch (S3Exception e) {
            throw new ApplicationException("Failed to upload file to S3: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate a pre-signed URL for secure access to a file
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(urlExpirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            throw new ApplicationException("Failed to generate presigned URL: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a file from S3
     * @param fileIdentifier Either a full URL (for profile pictures) or S3 key (for private files)
     */
    public void deleteFile(String fileIdentifier) {
        try {
            // Extract key from URL if it's a profile picture
            String key;
            if (fileIdentifier.startsWith("https://")) {
                key = fileIdentifier.substring(fileIdentifier.indexOf(".com/") + 5);
            } else {
                // Already a key
                key = fileIdentifier;
            }

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            throw new ApplicationException("Failed to delete file from S3: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}