package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.*;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.FolderRepository; // Assuming you might need it, added import
import dev.rubric.journalspring.repository.TagRepository;
import dev.rubric.journalspring.repository.SharedEntryRepository; // Added import
import dev.rubric.journalspring.repository.MediaRepository; // Added import
import jakarta.persistence.EntityManager; // Added import
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EntryServiceUnitTests {
    @Mock
    EntryRepository entryRepository;

    @Mock
    EncryptionService encryptionService;

    @Mock
    SearchService searchService;

    @Mock
    FolderService folderService; // Mock FolderService

    @Mock
    TagRepository tagRepository; // Mock TagRepository

    // Add mocks for other dependencies if needed by methods under test
    @Mock MediaRepository mediaRepository;
    @Mock S3Service s3Service;
    @Mock SharedEntryService sharedEntryService;
    @Mock SharedEntryRepository sharedEntryRepository;
    @Mock EntityManager entityManager;


    @InjectMocks
    EntryService entryService;

    @Test
    void addEntry_Success(){
        // --- Arrange ---
        User mockUser = new User();
        mockUser.setId(1L);

        Long folderId = 10L;
        Folder mockFolder = new Folder(mockUser, "testFolder");
        mockFolder.setId(folderId);

        String tagName = "testTag";
        Tag testTag = new Tag(tagName);

        String originalContent = "testContent";
        List<String> tagNames = List.of(tagName);

        EntryDto entryDto = new EntryDto(
                "testTitle",
                folderId,
                originalContent,
                tagNames,
                1, // wordCount
                true // isFavorite (though EntryDto doesn't have it, constructor does)
                // Note: EntryDto record doesn't include isFavorite, adjust if needed based on actual usage
        );

        String expectedEncryptedContent = "encryptedSuccessContent";

        // Mock dependencies
        when(folderService.getFolder(mockUser, folderId)).thenReturn(mockFolder);
        when(tagRepository.findByName(tagName)).thenReturn(Optional.of(testTag));
        when(encryptionService.encrypt(originalContent)).thenReturn(expectedEncryptedContent);
        // Mock search service (important for void methods)
        doNothing().when(searchService).indexEntry(any(Entry.class), eq(originalContent));
        // Mock repository save for void method
        // when(entryRepository.save(any(Entry.class))).thenReturn(null); // No need for void methods

        // --- Act ---
        entryService.addEntry(mockUser, entryDto); // Call the void method

        // --- Assert ---
        // Verify interactions and capture the saved entry
        verify(folderService, times(1)).getFolder(mockUser, folderId);
        verify(tagRepository, times(1)).findByName(tagName);
        verify(encryptionService, times(1)).encrypt(originalContent);

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(1)).save(entryCaptor.capture());

        // Verify searchService interaction with the *captured* entry and *original* content
        verify(searchService, times(1)).indexEntry(eq(entryCaptor.getValue()), eq(originalContent));

        // Assertions on the captured entry that was saved
        Entry savedEntry = entryCaptor.getValue();
        assertNotNull(savedEntry);
        assertNotNull(savedEntry.getPublicId()); // UUID should be set in the Entry constructor
        assertEquals("testTitle", savedEntry.getTitle());
        assertEquals(expectedEncryptedContent, savedEntry.getContent()); // Check ENCRYPTED content was saved
        assertEquals(1, savedEntry.getWordCount());
        assertEquals(mockUser, savedEntry.getUser());
        assertEquals(mockFolder, savedEntry.getFolder().orElse(null));
        assertTrue(savedEntry.getTags().stream().anyMatch(t -> t.getName().equals(tagName)));
        // isFavorite is not directly in EntryDto, the Entry constructor sets it to false by default
        // If EntryService logic sets favorite based on DTO, test that logic.
    }

    @Test
    void addEntry_NullTitle() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long folderId = 10L;
        Folder mockFolder = new Folder(mockUser, "testFolder");
        mockFolder.setId(folderId);

        EntryDto entryDto = new EntryDto(
                null, // Null title
                folderId,
                "testContent",
                List.of("testTag"),
                1,
                true
        );

        // Mock dependencies as needed (folderService, tagRepository, encryptionService, etc.)
        when(folderService.getFolder(mockUser, folderId)).thenReturn(mockFolder);
        when(tagRepository.findByName("testTag")).thenReturn(Optional.of(new Tag("testTag")));
        when(encryptionService.encrypt(anyString())).thenReturn("encryptedContent");

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));

        // Verify save was called, capturing the entry
        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(1)).save(entryCaptor.capture());
        assertNull(entryCaptor.getValue().getTitle()); // Title should be null in the saved entity
        verify(searchService, times(1)).indexEntry(any(Entry.class), eq("testContent"));
    }

    @Test
    void addEntry_EmptyTitle() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long folderId = 10L;
        Folder mockFolder = new Folder(mockUser, "testFolder");
        mockFolder.setId(folderId);


        EntryDto entryDto = new EntryDto(
                "", // Empty title
                folderId,
                "testContent",
                List.of("testTag"),
                1,
                true
        );
        // Mock dependencies as needed
        when(folderService.getFolder(mockUser, folderId)).thenReturn(mockFolder);
        when(tagRepository.findByName("testTag")).thenReturn(Optional.of(new Tag("testTag")));
        when(encryptionService.encrypt(anyString())).thenReturn("encryptedContent");

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));

        // Verify save was called, capturing the entry
        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(1)).save(entryCaptor.capture());
        assertEquals("", entryCaptor.getValue().getTitle()); // Title should be empty in the saved entity
        verify(searchService, times(1)).indexEntry(any(Entry.class), eq("testContent"));
    }

    @Test
    void addEntry_NullContent() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long folderId = 10L;
        Folder mockFolder = new Folder(mockUser, "testFolder");
        mockFolder.setId(folderId);

        EntryDto entryDto = new EntryDto(
                "testTitle",
                folderId,
                null, // Null content
                List.of("testTag"),
                0, // Word count likely 0
                true
        );
        // Mock dependencies as needed
        when(folderService.getFolder(mockUser, folderId)).thenReturn(mockFolder);
        when(tagRepository.findByName("testTag")).thenReturn(Optional.of(new Tag("testTag")));
        when(encryptionService.encrypt(null)).thenReturn(null); // Mock encrypting null

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(1)).save(entryCaptor.capture());
        assertNull(entryCaptor.getValue().getContent()); // Encrypted content should be null

        // Verify searchService interaction - plainTextContent is null here
        verify(searchService, times(1)).indexEntry(eq(entryCaptor.getValue()), eq(null));
    }


    @Test
    void addEntry_EmptyContent() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long folderId = 10L;
        Folder mockFolder = new Folder(mockUser, "testFolder");
        mockFolder.setId(folderId);

        EntryDto entryDto = new EntryDto(
                "testTitle",
                folderId,
                "", // Empty content
                List.of("testTag"),
                0, // Word count 0
                true
        );
        // Mock dependencies as needed
        when(folderService.getFolder(mockUser, folderId)).thenReturn(mockFolder);
        when(tagRepository.findByName("testTag")).thenReturn(Optional.of(new Tag("testTag")));
        when(encryptionService.encrypt("")).thenReturn(""); // Mock encrypting empty string

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(1)).save(entryCaptor.capture());
        assertEquals("", entryCaptor.getValue().getContent()); // Encrypted content should be empty

        // Verify searchService interaction
        verify(searchService, times(1)).indexEntry(eq(entryCaptor.getValue()), eq(""));
    }

    @Test
    void addEntry_EncryptsContent() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long folderId = 10L;
        Folder mockFolder = new Folder(mockUser, "testFolder");
        mockFolder.setId(folderId);

        String originalContent = "testContent";
        String tagName = "testTag";
        EntryDto entryDto = new EntryDto(
                "testTitle",
                folderId,
                originalContent,
                List.of(tagName),
                1,
                true
        );

        String expectedEncryptedContent = "encryptedTestContent";
        when(folderService.getFolder(mockUser, folderId)).thenReturn(mockFolder);
        when(tagRepository.findByName(tagName)).thenReturn(Optional.of(new Tag(tagName)));
        when(encryptionService.encrypt(originalContent)).thenReturn(expectedEncryptedContent);
        doNothing().when(searchService).indexEntry(any(Entry.class), eq(originalContent));

        // Act
        entryService.addEntry(mockUser, entryDto);

        // Assert
        verify(encryptionService, times(1)).encrypt(originalContent);

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, times(1)).save(entryCaptor.capture());
        verify(searchService, times(1)).indexEntry(eq(entryCaptor.getValue()), eq(originalContent));

        Entry savedEntry = entryCaptor.getValue();
        assertEquals(expectedEncryptedContent, savedEntry.getContent());
        assertNotEquals(originalContent, savedEntry.getContent());
    }

    @Test
    void getEntryById_InvalidUser() {
        User authorizedUser = new User();
        authorizedUser.setId(1L);

        User unauthorizedUser = new User();
        unauthorizedUser.setId(2L);

        Entry mockEntry = new Entry();
        mockEntry.setId(1L);
        mockEntry.setUser(authorizedUser);

        when(entryRepository.findById(1L)).thenReturn(Optional.of(mockEntry));
        // No need to mock entityManager.detach as it won't be reached

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.getEntryById(unauthorizedUser, 1L);
        });

        verify(entryRepository, times(1)).findById(1L);
        verify(encryptionService, never()).decrypt(anyString()); // Decrypt should not be called
        verify(entityManager, never()).detach(any()); // Detach should not be called

        assertEquals("User with id 2 is not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void getEntryById_NotFound() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long entryId = 1L;

        when(entryRepository.findById(entryId)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.getEntryById(mockUser, entryId);
        });

        assertEquals("Entry with 1 not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(entryRepository, times(1)).findById(entryId);
        verify(encryptionService, never()).decrypt(anyString());
        verify(entityManager, never()).detach(any());
    }

    @Test
    void getAllUserEntries_Success() {
        User mockUser = new User();
        mockUser.setId(1L);

        Entry entry1 = new Entry(); entry1.setId(1L); entry1.setUser(mockUser); entry1.setContent("encrypted1");
        Entry entry2 = new Entry(); entry2.setId(2L); entry2.setUser(mockUser); entry2.setContent("encrypted2");
        List<Entry> mockEntries = List.of(entry1, entry2);

        when(entryRepository.findAllByUser(mockUser)).thenReturn(mockEntries);
        when(encryptionService.decrypt("encrypted1")).thenReturn("decrypted1");
        when(encryptionService.decrypt("encrypted2")).thenReturn("decrypted2");

        List<Entry> results = entryService.getAllUserEntries(mockUser);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("decrypted1", results.get(0).getContent());
        assertEquals("decrypted2", results.get(1).getContent());
        verify(entryRepository, times(1)).findAllByUser(mockUser);
        verify(encryptionService, times(1)).decrypt("encrypted1");
        verify(encryptionService, times(1)).decrypt("encrypted2");
    }


    @Test
    void getAllUserEntries_NoEntries() {
        User mockUser = new User();
        mockUser.setId(1L);

        when(entryRepository.findAllByUser(mockUser)).thenReturn(Collections.emptyList());

        List<Entry> responses = entryService.getAllUserEntries(mockUser);

        verify(entryRepository, times(1)).findAllByUser(mockUser);
        verify(encryptionService, never()).decrypt(anyString()); // Ensure decrypt is not called

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void deleteEntry_Success() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long entryId = 1L;

        Entry mockEntry = new Entry();
        mockEntry.setId(entryId);
        mockEntry.setUser(mockUser);
        mockEntry.setTags(new HashSet<>()); // Initialize tags

        when(entryRepository.findById(entryId)).thenReturn(Optional.of(mockEntry));
        doNothing().when(searchService).removeEntryTokens(mockEntry);
        doNothing().when(sharedEntryService).removeSharedEntry(mockUser, entryId); // Mock shared entry removal
        doNothing().when(entryRepository).deleteById(entryId);


        assertDoesNotThrow(() -> entryService.deleteEntry(mockUser, entryId));

        verify(entryRepository, times(1)).findById(entryId);
        verify(searchService, times(1)).removeEntryTokens(mockEntry);
        verify(sharedEntryService, times(1)).removeSharedEntry(mockUser, entryId); // Verify shared entry removal
        verify(entryRepository, times(1)).deleteById(entryId);
    }


    @Test
    void deleteEntry_EntryNotFound() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long entryId = 1L;

        when(entryRepository.findById(entryId)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.deleteEntry(mockUser, entryId);
        });

        assertEquals("Entry with 1 not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(entryRepository, times(1)).findById(entryId);
        verify(searchService, never()).removeEntryTokens(any(Entry.class));
        verify(sharedEntryService, never()).removeSharedEntry(any(User.class), anyLong());
        verify(entryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteEntry_UnauthorizedUser() {
        User authorizedUser = new User();
        authorizedUser.setId(1L);

        User unauthorizedUser = new User();
        unauthorizedUser.setId(2L);
        Long entryId = 1L;

        Entry mockEntry = new Entry();
        mockEntry.setId(entryId);
        mockEntry.setUser(authorizedUser);

        when(entryRepository.findById(entryId)).thenReturn(Optional.of(mockEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.deleteEntry(unauthorizedUser, entryId);
        });

        assertEquals("User with id 2 is not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(entryRepository, times(1)).findById(entryId);
        verify(searchService, never()).removeEntryTokens(any(Entry.class));
        verify(sharedEntryService, never()).removeSharedEntry(any(User.class), anyLong());
        verify(entryRepository, never()).deleteById(anyLong());
    }

    @Test
    void updateEntry_EntryNotFound() {
        User mockUser = new User();
        mockUser.setId(1L);
        Long entryId = 1L;

        EntryDto updatedDetails = new EntryDto(
                "updatedTitle",
                null, // No folder update
                "updatedContent",
                Collections.emptyList(), // No tags
                5,
                true
        );

        when(entryRepository.findById(entryId)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.updateEntry(mockUser, updatedDetails, entryId);
        });

        assertEquals("Entry with 1 not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(entryRepository, times(1)).findById(entryId);
        verify(encryptionService, never()).encrypt(anyString());
        verify(entryRepository, never()).save(any(Entry.class));
        verify(searchService, never()).indexEntry(any(Entry.class), anyString());
    }

    @Test
    void updateEntry_UnauthorizedUser() {
        User authorizedUser = new User();
        authorizedUser.setId(1L);

        User unauthorizedUser = new User();
        unauthorizedUser.setId(2L);
        Long entryId = 1L;

        Entry mockEntry = new Entry();
        mockEntry.setId(entryId);
        mockEntry.setUser(authorizedUser); // Belongs to authorizedUser

        EntryDto updatedDetails = new EntryDto(
                "updatedTitle",
                null,
                "updatedContent",
                Collections.emptyList(),
                5,
                true
        );

        when(entryRepository.findById(entryId)).thenReturn(Optional.of(mockEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            // Attempt update by unauthorizedUser
            entryService.updateEntry(unauthorizedUser, updatedDetails, entryId);
        });

        assertEquals("User with id 2 is not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(entryRepository, times(1)).findById(entryId);
        verify(encryptionService, never()).encrypt(anyString());
        verify(entryRepository, never()).save(any(Entry.class));
        verify(searchService, never()).indexEntry(any(Entry.class), anyString());
    }
}