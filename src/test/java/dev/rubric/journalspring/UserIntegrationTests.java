package dev.rubric.journalspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.dto.UpdateUserDto;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import dev.rubric.journalspring.service.AuthService;
import dev.rubric.journalspring.service.JwtService;
import dev.rubric.journalspring.service.S3Service;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private S3Service s3Service;

    private User testUser;
    private String testUserToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = createUser("testuser", "user@example.com", "P@ssword1");
        testUserToken = getAuthToken(testUser.getEmail(), "P@ssword1");
    }

    private User createUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setActivated(true);
        user.setProfilePicture(null);
        return userRepository.save(user);
    }

    private String getAuthToken(String email, String password) {
        try {
            LoginUserDto loginDto = new LoginUserDto(email, password);
            User user = authService.authenticate(loginDto);
            return jwtService.generateToken(user);
        } catch (Exception e) {
            fail("Failed to get auth token for user " + email, e);
            return null;
        }
    }

    @Test
    void getAuthenticatedUser_Success() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.username").value(testUser.getDisplayUsername()));
    }

    @Test
    void getAuthenticatedUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadProfilePicture_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "hello.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "Hello, World!".getBytes()
        );
        String expectedUrl = "http://s3.mock.url/hello.jpg";

        when(s3Service.uploadProfilePicture(any())).thenReturn(expectedUrl);

        mockMvc.perform(multipart(HttpMethod.POST,"/api/v1/user/profile-picture/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(content().string("Profile picture uploaded successfully: " + expectedUrl));

        User updatedUser = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        assertEquals(expectedUrl, updatedUser.getProfilePicture());
        verify(s3Service).uploadProfilePicture(any());
    }

    @Test
    void uploadProfilePicture_PictureAlreadyExists_Fails() throws Exception {
        testUser.setProfilePicture("http://existing.url/pic.jpg");
        userRepository.save(testUser);

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "new.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "New data".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.POST,"/api/v1/user/profile-picture/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Profile picture already exists"));

        verify(s3Service, never()).uploadProfilePicture(any());
    }

    @Test
    void uploadProfilePicture_EmptyFile_Fails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart(HttpMethod.POST,"/api/v1/user/profile-picture/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File must not be empty"));

        verify(s3Service, never()).uploadProfilePicture(any());
    }

    @Test
    void updateUserSettings_Success() throws Exception {
        UpdateUserDto updateDto = new UpdateUserDto("newUserName", true, true, false);

        mockMvc.perform(put("/api/v1/user/update/settings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Updated user"));

        User updatedUser = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        assertEquals("newUserName", updatedUser.getDisplayUsername());
        assertTrue(updatedUser.isEnabled2fa());
        assertTrue(updatedUser.isAiAllowTitleAccess());
        assertFalse(updatedUser.isAiAllowContentAccess());
    }

    @Test
    void updateUserSettings_PartialUpdate() throws Exception {
        UpdateUserDto updateDto = new UpdateUserDto(null, null, true, null);

        mockMvc.perform(put("/api/v1/user/update/settings")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Updated user"));

        User updatedUser = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        assertEquals("testuser", updatedUser.getDisplayUsername()); // Should not change
        assertTrue(updatedUser.isAiAllowTitleAccess()); // Should change
        assertFalse(updatedUser.isEnabled2fa()); // Should not change
    }

    @Test
    void updateUserSettings_Unauthorized() throws Exception {
        UpdateUserDto updateDto = new UpdateUserDto("newUserName", true, true, false);

        mockMvc.perform(put("/api/v1/user/update/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }
}