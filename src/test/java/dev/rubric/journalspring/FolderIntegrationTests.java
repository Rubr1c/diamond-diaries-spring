package dev.rubric.journalspring;

import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.FolderRepository;
import dev.rubric.journalspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class FolderIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        folderRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("folder-test@example.com");
        testUser.setUsername("foldertest");
        testUser.setPassword(passwordEncoder.encode("P@ssword1"));
        testUser.setActivated(true);
        userRepository.save(testUser);

        otherUser = new User();
        otherUser.setEmail("other-user@example.com");
        otherUser.setUsername("otheruser");
        otherUser.setPassword(passwordEncoder.encode("P@ssword1"));
        otherUser.setActivated(true);
        userRepository.save(otherUser);
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void createFolder_Success() throws Exception {
        mockMvc.perform(post("/api/v1/folder/new/TestFolder")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(content().string("Created folder"));

        List<Folder> folders = folderRepository.getAllByUser(testUser);
        assertEquals(1, folders.size());
        assertEquals("TestFolder", folders.get(0).getName());
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void getFolder_Success() throws Exception {
        Folder folder = new Folder(testUser, "GetTestFolder");
        folderRepository.save(folder);

        mockMvc.perform(get("/api/v1/folder/" + folder.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folder.getId()))
                .andExpect(jsonPath("$.name").value("GetTestFolder"));
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void getFolder_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/folder/999")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("folder with id '999' does not exist"));
    }

    @Test
    @WithMockUser(username = "other-user@example.com")
    void getFolder_Unauthorized() throws Exception {
        Folder folder = new Folder(testUser, "UnauthorizedFolder");
        folderRepository.save(folder);

        mockMvc.perform(get("/api/v1/folder/" + folder.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("is not authorized")));
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void getAllFolders_Success() throws Exception {
        Folder folder1 = new Folder(testUser, "Folder1");
        Folder folder2 = new Folder(testUser, "Folder2");
        Folder folder3 = new Folder(testUser, "Folder3");
        folderRepository.saveAll(List.of(folder1, folder2, folder3));

        Folder otherFolder = new Folder(otherUser, "OtherFolder");
        folderRepository.save(otherFolder);

        mockMvc.perform(get("/api/v1/folder")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Folder1", "Folder2", "Folder3")));
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void updateFolderName_Success() throws Exception {
        Folder folder = new Folder(testUser, "OldName");
        folderRepository.save(folder);

        mockMvc.perform(put("/api/v1/folder/" + folder.getId() + "/update-name/NewName")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Updated folder"));

        Folder updatedFolder = folderRepository.findById(folder.getId()).orElseThrow();
        assertEquals("NewName", updatedFolder.getName());
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void updateFolderName_NotFound() throws Exception {
        mockMvc.perform(put("/api/v1/folder/999/update-name/NewName")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("folder with id '999' does not exist"));
    }

    @Test
    @WithMockUser(username = "other-user@example.com")
    void updateFolderName_Unauthorized() throws Exception {
        Folder folder = new Folder(testUser, "TestFolder");
        folderRepository.save(folder);

        mockMvc.perform(put("/api/v1/folder/" + folder.getId() + "/update-name/NewName")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("is not authorized")));
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void deleteFolder_Success() throws Exception {
        Folder folder = new Folder(testUser, "DeleteTestFolder");
        folderRepository.save(folder);

        mockMvc.perform(delete("/api/v1/folder/" + folder.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted folder"));

        assertTrue(folderRepository.findById(folder.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "folder-test@example.com")
    void deleteFolder_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/folder/999")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("folder with id '999' does not exist"));
    }

    @Test
    @WithMockUser(username = "other-user@example.com")
    void deleteFolder_Unauthorized() throws Exception {
        Folder folder = new Folder(testUser, "TestFolder");
        folderRepository.save(folder);

        mockMvc.perform(delete("/api/v1/folder/" + folder.getId())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("is not authorized")));
    }
}