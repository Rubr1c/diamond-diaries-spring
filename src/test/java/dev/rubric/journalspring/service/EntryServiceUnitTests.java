package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.EntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.*;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.response.EntryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EntryServiceUnitTests {
    @Mock
    EntryRepository entryRepository;

    @Mock
    EncryptionService encryptionService;


    @InjectMocks
    EntryService entryService;

    @Test
    void addEntry_Success(){
        User mockUser = new User();
        mockUser.setId(1L);

        Folder mockFolder = new Folder(
                mockUser,
                "testFolder"
        );

        Tag testTag = new Tag("testTag");
        Set<Tag> tags = Set.of(testTag);

        EntryDto entryDto = new EntryDto(
                "testTitle",
                mockFolder,
                "testContent",
                tags,
                1,
                true
        );

        Entry savedEntry = new Entry(
                mockUser,
                entryDto.folder(),
                entryDto.title(),
                entryDto.content(),
                entryDto.tags(),
                entryDto.wordCount()
        );
        savedEntry.setPublicId(UUID.randomUUID());

        when(entryRepository.save(any(Entry.class))).
                thenReturn(savedEntry);

        Entry response = entryService.addEntry(mockUser, entryDto);

        verify(entryRepository, times(1))
                .save(any(Entry.class));

        assertNotNull(response);
        assertNotNull(response.getPublicId());
        assertEquals("testTitle", response.getTitle());
        assertEquals("testContent", response.getContent());
        assertEquals(1, response.getWordCount());
    }

    @Test
    void addEntry_NullTitle() {
        User mockUser = new User();
        mockUser.setId(1L);

        EntryDto entryDto = new EntryDto(
                null,
                new Folder(mockUser, "testFolder"),
                "testContent",
                Set.of(new Tag("testTag")),
                1,
                true
        );

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));
    }

    @Test
    void addEntry_EmptyTitle() {
        User mockUser = new User();
        mockUser.setId(1L);

        EntryDto entryDto = new EntryDto(
                "",
                new Folder(mockUser, "testFolder"),
                "testContent",
                Set.of(new Tag("testTag")),
                1,
                true
        );

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));
    }

    @Test
    void addEntry_EmptyContent() {
        User mockUser = new User();
        mockUser.setId(1L);

        EntryDto entryDto = new EntryDto(
                "testTitle",
                new Folder(mockUser, "testFolder"),
                "",
                Set.of(new Tag("testTag")),
                1,
                true
        );

        assertDoesNotThrow(() -> entryService.addEntry(mockUser, entryDto));
    }

    @Test
    void addEntry_EncryptsContent() {
        User mockUser = new User();
        mockUser.setId(1L);

        Folder mockFolder = new Folder(mockUser, "testFolder");

        EntryDto entryDto = new EntryDto(
                "testTitle",
                mockFolder,
                "testContent",
                Set.of(new Tag("testTag")),
                1,
                true
        );

        String encryptedContent = "encryptedTestContent";
        when(encryptionService.encrypt("testContent")).thenReturn(encryptedContent);

        Entry savedEntry = new Entry(mockUser, entryDto.folder(), entryDto.title(), encryptedContent, entryDto.tags(), entryDto.wordCount());
        savedEntry.setPublicId(UUID.randomUUID());

        when(entryRepository.save(any(Entry.class))).thenReturn(savedEntry);

        Entry response = entryService.addEntry(mockUser, entryDto);

        verify(encryptionService, times(1)).encrypt("testContent");
        verify(entryRepository, times(1)).save(any(Entry.class));

        assertEquals(encryptedContent, response.getContent());
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

        when(entryRepository.findById(1L))
                .thenReturn(Optional.of(mockEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.getEntryById(unauthorizedUser, 1L);
        });

        verify(entryRepository, times(1)).findById(1L);

        assertEquals("User with id 2 is not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void getEntryById_NotFound() {
        when(entryRepository.findById(1L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.getEntryById(new User(), 1L);
        });

        assertEquals("Entry with 1 not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(entryRepository, times(1)).findById(1L);
    }

    @Test
    void getAllUserEntries_NoEntries() {
        User mockUser = new User();
        mockUser.setId(1L);

        when(entryRepository.findAllByUser(mockUser))
                .thenReturn(Collections.emptyList());

        List<Entry> responses = entryService.getAllUserEntries(mockUser);

        verify(entryRepository, times(1)).findAllByUser(mockUser);

        assertNotNull(responses);
        assertEquals(0, responses.size());
    }

    @Test
    void deleteEntry_Success() {
        User mockUser = new User();
        mockUser.setId(1L);

        Entry mockEntry = new Entry();
        mockEntry.setId(1L);
        mockEntry.setUser(mockUser);

        when(entryRepository.findById(1L))
                .thenReturn(Optional.of(mockEntry));

        assertDoesNotThrow(() -> entryService.deleteEntry(mockUser, 1L));

        verify(entryRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteEntry_EntryNotFound() {
        User mockUser = new User();
        mockUser.setId(1L);

        when(entryRepository.findById(1L))
                .thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.deleteEntry(mockUser, 1L);
        });

        assertEquals("Entry with 1 not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(entryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteEntry_UnauthorizedUser() {
        User authorizedUser = new User();
        authorizedUser.setId(1L);

        User unauthorizedUser = new User();
        unauthorizedUser.setId(2L);

        Entry mockEntry = new Entry();
        mockEntry.setId(1L);
        mockEntry.setUser(authorizedUser);

        when(entryRepository.findById(1L))
                .thenReturn(Optional.of(mockEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.deleteEntry(unauthorizedUser, 1L);
        });

        assertEquals("User with id 2 is not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(entryRepository, never()).deleteById(anyLong());
    }

    @Test
    void updateEntry_Success() {
        User mockUser = new User();
        mockUser.setId(1L);

        Entry mockEntry = new Entry();
        mockEntry.setId(1L);
        mockEntry.setUser(mockUser);
        mockEntry.setContent("oldContent");

        EntryDto updatedDetails = new EntryDto(
                "updatedTitle",
                new Folder(mockUser, "testFolder"),
                "updatedContent",
                Set.of(new Tag("testTag")),
                5,
                true
        );

        when(entryRepository.findById(1L))
                .thenReturn(Optional.of(mockEntry));
        when(entryRepository.save(any(Entry.class)))
                .thenReturn(mockEntry);

        Entry response = entryService.updateEntry(mockUser, updatedDetails, 1L);

        verify(entryRepository, times(1)).findById(1L);
        verify(entryRepository, times(1)).save(any(Entry.class));

        assertNotNull(response);
        assertEquals("updatedTitle", response.getTitle());
        assertEquals("updatedContent", response.getContent());
        assertEquals(5, response.getWordCount());
    }

    @Test
    void updateEntry_EntryNotFound() {
        User mockUser = new User();
        mockUser.setId(1L);

        EntryDto updatedDetails = new EntryDto(
                "updatedTitle",
                new Folder(mockUser, "testFolder"),
                "updatedContent",
                Set.of(new Tag("testTag")),
                5,
                true
        );

        when(entryRepository.findById(1L))
                .thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.updateEntry(mockUser, updatedDetails, 1L);
        });

        assertEquals("Entry with 1 not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(entryRepository, never()).save(any(Entry.class));
    }

    @Test
    void updateEntry_UnauthorizedUser() {
        User authorizedUser = new User();
        authorizedUser.setId(1L);

        User unauthorizedUser = new User();
        unauthorizedUser.setId(2L);

        Entry mockEntry = new Entry();
        mockEntry.setId(1L);
        mockEntry.setUser(authorizedUser);

        EntryDto updatedDetails = new EntryDto(
                "updatedTitle",
                new Folder(authorizedUser, "testFolder"),
                "updatedContent",
                Set.of(new Tag("testTag")),
                5,
                true
        );

        when(entryRepository.findById(1L))
                .thenReturn(Optional.of(mockEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entryService.updateEntry(unauthorizedUser, updatedDetails, 1L);
        });

        assertEquals("User with id 2 is not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(entryRepository, never()).save(any(Entry.class));
    }


}
