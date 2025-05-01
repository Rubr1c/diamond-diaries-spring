package dev.rubric.journalspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.FolderRepository;
import dev.rubric.journalspring.repository.UserRepository;
import dev.rubric.journalspring.service.AuthService;
import dev.rubric.journalspring.service.FolderService;
import dev.rubric.journalspring.service.JwtService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;


import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class FolderIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private User otherUser;
    private String testUserToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        folderRepository.deleteAll();
        userRepository.deleteAll();

        testUser = createUser("testuser", "folderuser@example.com", "P@ssword1");
        otherUser = createUser("otheruser", "otherfolder@example.com", "P@ssword2");

        testUserToken = getAuthToken(testUser.getEmail(), "P@ssword1");
        otherUserToken = getAuthToken(otherUser.getEmail(), "P@ssword2");
    }

    private User createUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setActivated(true);
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

    private Folder createAndSaveFolder(String name, User user) {
         Folder folder = new Folder(user, name);
         return folderRepository.save(folder);
    }


    @Test
    void createFolder_Success() throws Exception {
        String folderName = "My New Folder";
        mockMvc.perform(post("/api/v1/folder/new/{name}", folderName)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(content().string("Created folder"));

        assertTrue(folderRepository.getAllByUser(testUser).stream()
                .anyMatch(f -> f.getName().equals(folderName)));
    }

    @Test
    void getFolderById_Success() throws Exception {
        Folder folder = createAndSaveFolder("Folder To Get", testUser);

        mockMvc.perform(get("/api/v1/folder/{id}", folder.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folder.getId()))
                .andExpect(jsonPath("$.name").value("Folder To Get"))
                .andExpect(jsonPath("$.publicId").value(folder.getPublicId().toString()));
    }

    @Test
    void getFolderById_NotFound() throws Exception {
         mockMvc.perform(get("/api/v1/folder/{id}", 9999L)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("folder with id '9999' does not exist"));
    }

     @Test
    void getFolderById_Unauthorized() throws Exception {
        Folder folder = createAndSaveFolder("Another User Folder", otherUser);

        mockMvc.perform(get("/api/v1/folder/{id}", folder.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                 .andExpect(jsonPath("$.message").value(String.format("User with id %d is not authorized", testUser.getId())));
    }


    @Test
    void getFolderByPublicId_Success() throws Exception {
        Folder folder = createAndSaveFolder("Public Get Test", testUser);
        UUID publicId = folder.getPublicId();

         mockMvc.perform(get("/api/v1/folder/public/{publicId}", publicId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folder.getId()))
                .andExpect(jsonPath("$.name").value("Public Get Test"))
                .andExpect(jsonPath("$.publicId").value(publicId.toString()));
    }

     @Test
    void getFolderByPublicId_NotFound() throws Exception {
         UUID nonExistentUUID = UUID.randomUUID();
         mockMvc.perform(get("/api/v1/folder/public/{publicId}", nonExistentUUID)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("folder with id")));
    }

    @Test
    void getAllFolders_Success() throws Exception {
        createAndSaveFolder("Folder 1", testUser);
        createAndSaveFolder("Folder 2", testUser);

         mockMvc.perform(get("/api/v1/folder")
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Folder 1"))
                .andExpect(jsonPath("$[1].name").value("Folder 2"));
    }

    @Test
    void getAllFolders_NoFolders() throws Exception {
         mockMvc.perform(get("/api/v1/folder")
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateFolderName_Success() throws Exception {
        Folder folder = createAndSaveFolder("Old Name", testUser);
        String newName = "Updated Folder Name";

         mockMvc.perform(put("/api/v1/folder/{id}/update-name/{name}", folder.getId(), newName)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Updated folder"));

        Optional<Folder> updatedFolder = folderRepository.findById(folder.getId());
        assertTrue(updatedFolder.isPresent());
        assertEquals(newName, updatedFolder.get().getName());
    }

    @Test
    void updateFolderName_NotFound() throws Exception {
        mockMvc.perform(put("/api/v1/folder/{id}/update-name/{name}", 9999L, "New Name")
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("folder with id '9999' does not exist"));
    }

     @Test
    void updateFolderName_Unauthorized() throws Exception {
         Folder folder = createAndSaveFolder("Belongs to Other", otherUser);
         String newName = "Attempted Update";

         mockMvc.perform(put("/api/v1/folder/{id}/update-name/{name}", folder.getId(), newName)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(String.format("User with id %d is not authorized", testUser.getId())));
     }


    @Test
    void deleteFolder_Success() throws Exception {
        Folder folder = createAndSaveFolder("To Be Deleted", testUser);

        mockMvc.perform(delete("/api/v1/folder/{id}", folder.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted folder"));

        assertFalse(folderRepository.findById(folder.getId()).isPresent());
    }

    @Test
    void deleteFolder_NotFound() throws Exception {
         mockMvc.perform(delete("/api/v1/folder/{id}", 9999L)
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("folder with id '9999' does not exist"));
    }

    @Test
    void deleteFolder_Unauthorized() throws Exception {
        Folder folder = createAndSaveFolder("Other User Delete", otherUser);

         mockMvc.perform(delete("/api/v1/folder/{id}", folder.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(String.format("User with id %d is not authorized", testUser.getId())));
    }
}