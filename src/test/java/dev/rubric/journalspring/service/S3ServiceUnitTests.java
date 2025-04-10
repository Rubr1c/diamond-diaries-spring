package dev.rubric.journalspring.service;

import dev.rubric.journalspring.service.S3Service;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3ServiceUnitTests {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3Service s3Service;

    @Captor
    private ArgumentCaptor<PutObjectRequest> putObjectRequestCaptor;

    @Captor
    private ArgumentCaptor<RequestBody> requestBodyCaptor;

    @Captor
    private ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestCaptor;

    @Captor
    private ArgumentCaptor<GetObjectPresignRequest> presignRequestCaptor;

    @BeforeEach
    void setUp() {
        // Create the S3Service instance manually instead of using @InjectMocks
        s3Service = Mockito.spy(new S3Service("test-access-key", "test-secret-key"));

        // Set the mocked S3Client and S3Presigner
        ReflectionTestUtils.setField(s3Service, "s3Client", s3Client);
        ReflectionTestUtils.setField(s3Service, "s3Presigner", s3Presigner);

        // Set necessary fields using ReflectionTestUtils
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "urlExpirationMinutes", 15);
    }


    @Nested
    @DisplayName("Upload Profile Picture Tests")
    class UploadProfilePictureTests {

        @Test
        @DisplayName("Should successfully upload profile picture with public access")
        void shouldUploadProfilePictureSuccessfully() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String result = s3Service.uploadProfilePicture(file);

            // Assert
            verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
            PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();

            assertNotNull(result);
            assertTrue(result.contains("https://test-bucket.s3.amazonaws.com/profile-pictures/"));
            assertTrue(result.contains("profile.jpg"));
            assertEquals("test-bucket", capturedRequest.bucket());
            assertTrue(capturedRequest.key().startsWith("profile-pictures/"));
            assertTrue(capturedRequest.key().endsWith("profile.jpg"));
            assertEquals("image/jpeg", capturedRequest.contentType());


            assertEquals(ObjectCannedACL.PUBLIC_READ, capturedRequest.acl());
        }

        @Test
        @DisplayName("Should handle file reading error")
        void shouldHandleFileReadingError() throws IOException {
            // Arrange
            MultipartFile fileMock = mock(MultipartFile.class);
            when(fileMock.getOriginalFilename()).thenReturn("profile.jpg");
            when(fileMock.getContentType()).thenReturn("image/jpeg");
            when(fileMock.getBytes()).thenThrow(new IOException("Simulated IO error"));

            // Act & Assert
            ApplicationException exception = assertThrows(ApplicationException.class, () ->
                    s3Service.uploadProfilePicture(fileMock)
            );

            assertEquals("Failed to read file data", exception.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Should handle S3 upload error")
        void shouldHandleS3UploadError() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("S3 error").build());

            // Act & Assert
            ApplicationException exception = assertThrows(ApplicationException.class, () ->
                    s3Service.uploadProfilePicture(file)
            );

            assertTrue(exception.getMessage().contains("Failed to upload profile picture to S3"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Upload File Tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload image file with correct folder")
        void shouldUploadImageFileWithCorrectFolder() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image.png",
                    "image/png",
                    "test image content".getBytes()
            );

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String result = s3Service.uploadFile(file, MediaType.IMAGE);

            // Assert
            verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
            PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();

            assertTrue(result.startsWith("photos/"));
            assertTrue(result.contains("image.png"));
            assertEquals("test-bucket", capturedRequest.bucket());
            assertEquals("image/png", capturedRequest.contentType());
            assertNull(capturedRequest.acl()); // Private access by default
        }

        @Test
        @DisplayName("Should upload video file with correct folder")
        void shouldUploadVideoFileWithCorrectFolder() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "video.mp4",
                    "video/mp4",
                    "test video content".getBytes()
            );

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String result = s3Service.uploadFile(file, MediaType.VIDEO);

            // Assert
            verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
            PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();

            assertTrue(result.startsWith("videos/"));
            assertTrue(result.contains("video.mp4"));
        }

        @Test
        @DisplayName("Should use correct folder structure based on media type")
        void shouldUseCorrectFolderStructureBasedOnMediaType() throws IOException {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", "test".getBytes());
            MockMultipartFile videoFile = new MockMultipartFile(
                    "file", "video.mp4", "video/mp4", "test".getBytes());
            MockMultipartFile otherFile = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "test".getBytes());

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String imageKey = s3Service.uploadFile(imageFile, MediaType.IMAGE);
            String videoKey = s3Service.uploadFile(videoFile, MediaType.VIDEO);
            String otherKey = s3Service.uploadFile(otherFile, null);  // Or use an appropriate enum value

            // Assert
            assertTrue(imageKey.startsWith("photos/"));
            assertTrue(videoKey.startsWith("videos/"));
            assertTrue(otherKey.startsWith("files/"));
        }

        @Test
        @DisplayName("Should handle file reading error during upload")
        void shouldHandleFileReadingErrorDuringUpload() throws IOException {
            // Arrange
            MultipartFile fileMock = mock(MultipartFile.class);
            when(fileMock.getOriginalFilename()).thenReturn("file.pdf");
            when(fileMock.getContentType()).thenReturn("application/pdf");
            when(fileMock.getBytes()).thenThrow(new IOException("Simulated IO error"));

            // Act & Assert
            ApplicationException exception = assertThrows(ApplicationException.class, () ->
                    s3Service.uploadFile(fileMock, null)
            );

            assertEquals("Failed to read file data", exception.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should handle S3 error during file upload")
        void shouldHandleS3ErrorDuringFileUpload() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image.jpg",
                    "image/jpeg",
                    "test content".getBytes()
            );

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(S3Exception.builder().message("S3 upload error").build());

            // Act & Assert
            ApplicationException exception = assertThrows(ApplicationException.class, () ->
                    s3Service.uploadFile(file, MediaType.IMAGE)
            );

            assertTrue(exception.getMessage().contains("Failed to upload file to S3"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Generate Presigned URL Tests")
    class GeneratePresignedUrlTests {

        @Test
        @DisplayName("Should generate presigned URL successfully")
        void shouldGeneratePresignedUrlSuccessfully() {
            // Arrange
            String s3Key = "photos/uuid-test-image.jpg";
            URL mockUrl = mock(URL.class);
            when(mockUrl.toString()).thenReturn("https://presigned-url.com/test");

            PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
            when(presignedRequest.url()).thenReturn(mockUrl);

            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedRequest);

            // Act
            String result = s3Service.generatePresignedUrl(s3Key);

            // Assert
            verify(s3Presigner).presignGetObject(presignRequestCaptor.capture());
            GetObjectPresignRequest capturedRequest = presignRequestCaptor.getValue();

            assertEquals("https://presigned-url.com/test", result);
            assertEquals("test-bucket", capturedRequest.getObjectRequest().bucket());
            assertEquals(s3Key, capturedRequest.getObjectRequest().key());
            assertEquals(Duration.ofMinutes(15), capturedRequest.signatureDuration());
        }

        @Test
        @DisplayName("Should handle S3 error during presigned URL generation")
        void shouldHandleS3ErrorDuringPresignedUrlGeneration() {
            // Arrange
            String s3Key = "photos/test-image.jpg";

            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenThrow(S3Exception.builder().message("Presigning error").build());

            // Act & Assert
            ApplicationException exception = assertThrows(ApplicationException.class, () ->
                    s3Service.generatePresignedUrl(s3Key)
            );

            assertTrue(exception.getMessage().contains("Failed to generate presigned URL"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Delete File Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should delete file using S3 key")
        void shouldDeleteFileUsingS3Key() {
            // Arrange
            String s3Key = "photos/uuid-test-image.jpg";

            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenReturn(DeleteObjectResponse.builder().build());

            // Act
            s3Service.deleteFile(s3Key);

            // Assert
            verify(s3Client).deleteObject(deleteObjectRequestCaptor.capture());
            DeleteObjectRequest capturedRequest = deleteObjectRequestCaptor.getValue();

            assertEquals("test-bucket", capturedRequest.bucket());
            assertEquals(s3Key, capturedRequest.key());
        }

        @Test
        @DisplayName("Should delete file using full URL")
        void shouldDeleteFileUsingFullUrl() {
            // Arrange
            String fileUrl = "https://test-bucket.s3.amazonaws.com/profile-pictures/uuid-image.jpg";

            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenReturn(DeleteObjectResponse.builder().build());

            // Act
            s3Service.deleteFile(fileUrl);

            // Assert
            verify(s3Client).deleteObject(deleteObjectRequestCaptor.capture());
            DeleteObjectRequest capturedRequest = deleteObjectRequestCaptor.getValue();

            assertEquals("test-bucket", capturedRequest.bucket());
            assertEquals("profile-pictures/uuid-image.jpg", capturedRequest.key());
        }

        @Test
        @DisplayName("Should handle S3 error during file deletion")
        void shouldHandleS3ErrorDuringFileDeletion() {
            // Arrange
            String s3Key = "photos/test-image.jpg";

            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .thenThrow(S3Exception.builder().message("Delete error").build());

            // Act & Assert
            ApplicationException exception = assertThrows(ApplicationException.class, () ->
                    s3Service.deleteFile(s3Key)
            );

            assertTrue(exception.getMessage().contains("Failed to delete file from S3"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Key Generation and Pattern Verification Tests")
    class KeyGenerationTests {

        @Test
        @DisplayName("Should use correct folder structure based on media type")
        void shouldUseCorrectFolderStructureBasedOnMediaType() throws IOException {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", "test".getBytes());
            MockMultipartFile videoFile = new MockMultipartFile(
                    "file", "video.mp4", "video/mp4", "test".getBytes());
            MockMultipartFile otherFile = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "test".getBytes());

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String imageKey = s3Service.uploadFile(imageFile, MediaType.IMAGE);
            String videoKey = s3Service.uploadFile(videoFile, MediaType.VIDEO);
            String otherKey = s3Service.uploadFile(otherFile, null);

            // Assert
            assertTrue(imageKey.startsWith("photos/"));
            assertTrue(videoKey.startsWith("videos/"));
            assertTrue(otherKey.startsWith("files/"));
        }

        @Test
        @DisplayName("Should preserve original filename in the S3 key")
        void shouldPreserveOriginalFilenameInTheS3Key() throws IOException {
            // Arrange
            String originalFilename = "my-special-image.jpg";
            MockMultipartFile file = new MockMultipartFile(
                    "file", originalFilename, "image/jpeg", "test".getBytes());

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String key = s3Service.uploadFile(file, MediaType.IMAGE);

            // Assert
            assertTrue(key.contains(originalFilename));
            assertTrue(key.contains(originalFilename));
            // Should have UUID format before the filename
            assertTrue(key.matches("photos/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}-" + originalFilename));
        }

        @Test
        @DisplayName("Should create unique keys for files with the same name")
        void shouldCreateUniqueKeysForFilesWithTheSameName() throws IOException {
            // Arrange
            MockMultipartFile file1 = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", "test1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", "test2".getBytes());

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String key1 = s3Service.uploadFile(file1, MediaType.IMAGE);
            String key2 = s3Service.uploadFile(file2, MediaType.IMAGE);

            // Assert
            assertNotEquals(key1, key2);
            assertTrue(key1.contains("image.jpg"));
            assertTrue(key2.contains("image.jpg"));
        }

        @Test
        @DisplayName("Should handle files with special characters in name")
        void shouldHandleFilesWithSpecialCharactersInName() throws IOException {
            // Arrange
            String specialFilename = "file with spaces & symbols!.jpg";
            MockMultipartFile file = new MockMultipartFile(
                    "file", specialFilename, "image/jpeg", "test".getBytes());

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            // Act
            String key = s3Service.uploadProfilePicture(file);

            // Assert
            assertTrue(key.contains(specialFilename));
            verify(s3Client).putObject(putObjectRequestCaptor.capture(), any(RequestBody.class));
            PutObjectRequest capturedRequest = putObjectRequestCaptor.getValue();
            assertTrue(capturedRequest.key().contains(specialFilename));
        }
    }
}