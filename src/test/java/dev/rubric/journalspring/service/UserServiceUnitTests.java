package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.UpdateUserDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setProfilePicture("");
        testUser.setEnabled2fa(false);
        testUser.setAiAllowContentAccess(false);
        testUser.setAiAllowTitleAccess(false);
    }


    @Test
    void updateUser_UpdateUsername() {
        UpdateUserDto updateDto = new UpdateUserDto("newUsername", null, null, null);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.updateUser(testUser, updateDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newUsername", userCaptor.getValue().getDisplayUsername());
    }

    @Test
    void updateUser_Update2FA() {
        UpdateUserDto updateDto = new UpdateUserDto(null, true, null, null);
         when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.updateUser(testUser, updateDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isEnabled2fa());
    }

     @Test
    void updateUser_UpdateAISettings() {
        UpdateUserDto updateDto = new UpdateUserDto(null, null, true, false);
         when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.updateUser(testUser, updateDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isAiAllowTitleAccess());
        assertFalse(userCaptor.getValue().isAiAllowContentAccess());
    }

     @Test
    void updateUser_UpdateMultipleFields() {
        UpdateUserDto updateDto = new UpdateUserDto("newerName", false, false, true);
         when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.updateUser(testUser, updateDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newerName", userCaptor.getValue().getDisplayUsername());
        assertFalse(userCaptor.getValue().isEnabled2fa());
        assertFalse(userCaptor.getValue().isAiAllowTitleAccess());
        assertTrue(userCaptor.getValue().isAiAllowContentAccess());
    }

    @Test
    void updateUser_NullValuesInDto_NoChanges() {
         String originalUsername = testUser.getDisplayUsername();
         Boolean original2FA = testUser.isEnabled2fa();
         boolean originalAiTitle = testUser.isAiAllowTitleAccess();
         boolean originalAiContent = testUser.isAiAllowContentAccess();

        UpdateUserDto updateDto = new UpdateUserDto(null, null, null, null);
         when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.updateUser(testUser, updateDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(originalUsername, savedUser.getDisplayUsername());
        assertEquals(original2FA, savedUser.isEnabled2fa());
        assertEquals(originalAiTitle, savedUser.isAiAllowTitleAccess());
        assertEquals(originalAiContent, savedUser.isAiAllowContentAccess());
    }
}