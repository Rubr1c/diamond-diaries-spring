package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.dto.UpdateUserDto;
import dev.rubric.journalspring.service.S3Service;
import dev.rubric.journalspring.enums.MediaType;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.UserResponse;
import dev.rubric.journalspring.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;


@RequestMapping("/api/v1/user")
@RestController
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService, S3Service s3Service) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> authenticatedUser(@AuthenticationPrincipal User user) {

        logger.debug("User '{}' requesting their info", user.getEmail());

        UserResponse response = new UserResponse(user);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/profile-picture", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfilePicture(@AuthenticationPrincipal User user,
                                                       @RequestParam("profilePicture") MultipartFile profilePicture) {

        logger.debug("User '{}' is uploading new profile picture", user.getEmail());


        if (profilePicture.isEmpty()) {
            return ResponseEntity.badRequest().body("File must not be empty");
        }

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            throw new ApplicationException("Profile picture already exists", HttpStatus.BAD_REQUEST);
        }

        try {
            String fileUrl = userService.uploadProfilePicture(user, profilePicture);
            return ResponseEntity.status(HttpStatus.CREATED).body("Profile picture uploaded successfully: " + fileUrl);
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload profile picture");
        }
    }

    @DeleteMapping
    public ResponseEntity<String> deleteProfilePicture(@AuthenticationPrincipal User user){

        if(user.getProfilePicture() != null){
            try{
                userService.deleteProfilePicture(user);
                return ResponseEntity.noContent().build();
            }
            catch (S3Exception e){
                logger.error("Error deleting profile picture: {}", e.getMessage());
                throw new ApplicationException("Failed to delete profile picture", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        else{
            return ResponseEntity.noContent().build();
        }
    }

    @PutMapping("/update/settings")
    public ResponseEntity<String> updateUserSettings(@AuthenticationPrincipal User user,
                                                     @RequestBody UpdateUserDto updatedInfo) {
        userService.updateUser(user, updatedInfo);

        return ResponseEntity.ok("Updated user");
    }

}
