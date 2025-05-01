package dev.rubric.journalspring;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.dto.RegisterUserDto;
import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SharedEntry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.SharedEntryRepository;
import dev.rubric.journalspring.repository.UserRepository;
import dev.rubric.journalspring.service.AuthService;
import dev.rubric.journalspring.service.EncryptionService;
import dev.rubric.journalspring.service.EntryService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class SharedEntryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntryRepository entryRepository;

    @Autowired
    private SharedEntryRepository sharedEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private EntryService entryService;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private JwtService jwtService;

    private User ownerUser;
    private User otherUser;
    private User thirdUser;
    private Entry testEntry;
    private String ownerToken;
    private String otherToken;
    private String thirdToken;

    @BeforeEach
    void setUp() {
        sharedEntryRepository.deleteAll();
        entryRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = createUser("owner", "owner@example.com", "P@ssword1");
        otherUser = createUser("other", "other@example.com", "P@ssword2");
        thirdUser = createUser("third", "third@example.com", "P@ssword3");

        ownerToken = getAuthToken(ownerUser.getEmail(), "P@ssword1");
        otherToken = getAuthToken(otherUser.getEmail(), "P@ssword2");
        thirdToken = getAuthToken(thirdUser.getEmail(), "P@ssword3");

        EntryDto entryDto = new EntryDto("Test Title", null, "Test Content", Collections.emptyList(), 2, false);
        testEntry = entryService.addEntry(ownerUser, entryDto);
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

    @Test
    void shareEntry_Success() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), List.of(otherUser.getEmail()), false);

        MvcResult result = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(notNullValue()))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        UUID sharedUuid = objectMapper.readValue(responseContent, UUID.class);
        assertTrue(sharedEntryRepository.getByPublicId(sharedUuid).isPresent());
    }

    @Test
    void shareEntry_EntryNotFound() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(9999L, List.of(otherUser.getEmail()), false);

        mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Entry not found"));
    }

    @Test
    void shareEntry_Unauthorized_NotOwner() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), List.of(thirdUser.getEmail()), false);

        mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Not authorized to share this entry"));
    }

    @Test
    void getSharedEntry_Success_AllowedUser() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), List.of(otherUser.getEmail()), false);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk())
                .andReturn();
        UUID sharedUuid = objectMapper.readValue(shareResult.getResponse().getContentAsString(), UUID.class);

        mockMvc.perform(get("/api/v1/shared-entry/{id}", sharedUuid)
                        .header("Authorization", "Bearer " + otherToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEntry.getId()))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    void getSharedEntry_Unauthorized_NotAllowed() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), List.of(otherUser.getEmail()), false);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk())
                .andReturn();
        UUID sharedUuid = objectMapper.readValue(shareResult.getResponse().getContentAsString(), UUID.class);

        mockMvc.perform(get("/api/v1/shared-entry/{id}", sharedUuid)
                        .header("Authorization", "Bearer " + thirdToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Not authorized"));
    }

    @Test
    void getSharedEntry_NotFound() throws Exception {
        UUID nonExistentUuid = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/shared-entry/{id}", nonExistentUuid)
                        .header("Authorization", "Bearer " + ownerToken))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Share not found"));
    }

    @Test
    void addUserToSharedEntry_Success() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), Collections.emptyList(), false);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk())
                .andReturn();
        UUID sharedUuid = objectMapper.readValue(shareResult.getResponse().getContentAsString(), UUID.class);

        Map<String, String> requestBody = Map.of("userEmail", otherUser.getEmail());

        mockMvc.perform(post("/api/v1/shared-entry/{id}/add-user", sharedUuid)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("User added to entry"));

        SharedEntry updatedShare = sharedEntryRepository.getByPublicId(sharedUuid).orElseThrow();
        assertTrue(updatedShare.getAllowedUsers().stream().anyMatch(u -> u.getId().equals(otherUser.getId())));
    }

     @Test
    void removeUserFromSharedEntry_Success() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), List.of(otherUser.getEmail()), false);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk())
                .andReturn();
        UUID sharedUuid = objectMapper.readValue(shareResult.getResponse().getContentAsString(), UUID.class);

        Map<String, String> requestBody = Map.of("userEmail", otherUser.getEmail());

        mockMvc.perform(delete("/api/v1/shared-entry/{id}/remove-user", sharedUuid)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("User removed to entry"));

        SharedEntry updatedShare = sharedEntryRepository.getByPublicId(sharedUuid).orElseThrow();
        assertTrue(updatedShare.getAllowedUsers().stream().noneMatch(u -> u.getId().equals(otherUser.getId())));
        assertTrue(updatedShare.getAllowedUsers().stream().anyMatch(u -> u.getId().equals(ownerUser.getId())));
    }

    @Test
    void removeUserFromSharedEntry_UserNotFoundInShare() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), Collections.emptyList(), false);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk())
                .andReturn();
        UUID sharedUuid = objectMapper.readValue(shareResult.getResponse().getContentAsString(), UUID.class);

        Map<String, String> requestBody = Map.of("userEmail", thirdUser.getEmail()); // thirdUser is not in the share

        mockMvc.perform(delete("/api/v1/shared-entry/{id}/remove-user", sharedUuid)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format("User '%s' already does not have access to entry", thirdUser.getEmail())));
    }

     @Test
    void removeUserFromSharedEntry_Unauthorized_NotOwner() throws Exception {
        SharedEntryDto shareDto = new SharedEntryDto(testEntry.getId(), List.of(otherUser.getEmail()), false);
        MvcResult shareResult = mockMvc.perform(post("/api/v1/shared-entry/new")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk())
                .andReturn();
        UUID sharedUuid = objectMapper.readValue(shareResult.getResponse().getContentAsString(), UUID.class);

        Map<String, String> requestBody = Map.of("userEmail", ownerUser.getEmail()); // Trying to remove the owner

        mockMvc.perform(delete("/api/v1/shared-entry/{id}/remove-user", sharedUuid)
                        .header("Authorization", "Bearer " + otherToken) // Non-owner attempting removal
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(String.format("User with id %d is not authorized", otherUser.getId())));
    }
}