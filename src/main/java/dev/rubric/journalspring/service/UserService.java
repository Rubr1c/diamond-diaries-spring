package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.UpdateUserDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final S3Service s3Service;


    public UserService(UserRepository userRepository, S3Service s3Service) {
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    public User findByUsername(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public String uploadProfilePicture(User user, MultipartFile profilePicture) {
        if (profilePicture == null || profilePicture.isEmpty()) {
            throw new ApplicationException("Profile picture cannot be empty", HttpStatus.BAD_REQUEST);
        }

        try {
            String uploadedUrl = s3Service.uploadProfilePicture(profilePicture);

            user.setProfilePicture(uploadedUrl);
            userRepository.save(user);

            logger.info("Successfully uploaded profile picture: Name='{}', Size={} bytes",
                    profilePicture.getOriginalFilename(),
                    profilePicture.getSize());
            return uploadedUrl;
        } catch (S3Exception e) {
            throw new ApplicationException("Failed to upload profile picture: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteProfilePicture(User user){
        if(user.getProfilePicture() != null){
            try{
                s3Service.deleteFile(user.getProfilePicture());
                user.setProfilePicture(null);
                userRepository.save(user);
            }
            catch (S3Exception e){
                logger.error("Error deleting profile picture: {}", e.getMessage());
                throw new ApplicationException("Failed to delete profile picture", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    public void updateUser(User user,
                           UpdateUserDto updatedInfo) {

        if (updatedInfo.aiAllowContentAccess() != null) {
            user.setAiAllowContentAccess(updatedInfo.aiAllowContentAccess());
        }
        if (updatedInfo.aiAllowTitleAccess() != null) {
            user.setAiAllowTitleAccess(updatedInfo.aiAllowTitleAccess());
        }
        if (updatedInfo.enabled2fa() != null) {
            user.setEnabled2fa(updatedInfo.enabled2fa());
        }
        if (updatedInfo.username() != null) {
            user.setUsername(updatedInfo.username());
        }

        userRepository.save(user);
    }
}
