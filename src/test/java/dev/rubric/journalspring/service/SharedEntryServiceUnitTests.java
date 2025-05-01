package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SharedEntry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.SharedEntryRepository;
import dev.rubric.journalspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SharedEntryServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SharedEntryRepository sharedEntryRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private SharedEntryService sharedEntryService;

    private User ownerUser;
    private User allowedUser;
    private User nonAllowedUser;
    private Entry entry;
    private SharedEntry sharedEntry;
    private SharedEntry sharedEntryAllowAnyone;

    @BeforeEach
    void setUp() {
        ownerUser = new User();
        ownerUser.setId(1L);
        ownerUser.setEmail("owner@example.com");

        allowedUser = new User();
        allowedUser.setId(2L);
        allowedUser.setEmail("allowed@example.com");

        nonAllowedUser = new User();
        nonAllowedUser.setId(3L);
        nonAllowedUser.setEmail("nonallowed@example.com");

        entry = new Entry();
        entry.setId(10L);
        entry.setUser(ownerUser);
        entry.setContent("EncryptedContent");

        List<User> users = new ArrayList<>();
        users.add(ownerUser);
        users.add(allowedUser);

        sharedEntry = new SharedEntry();
        sharedEntry.setId(100L);
        sharedEntry.setPublicId(UUID.randomUUID());
        sharedEntry.setEntry(entry);
        sharedEntry.setExpiryTime(ZonedDateTime.now().plusDays(1));
        sharedEntry.setAllowedUsers(users);
        sharedEntry.setAllowAnyone(false);

        sharedEntryAllowAnyone = new SharedEntry();
        sharedEntryAllowAnyone.setId(101L);
        sharedEntryAllowAnyone.setPublicId(UUID.randomUUID());
        sharedEntryAllowAnyone.setEntry(entry);
        sharedEntryAllowAnyone.setExpiryTime(ZonedDateTime.now().plusDays(1));
        sharedEntryAllowAnyone.setAllowedUsers(List.of(ownerUser));
        sharedEntryAllowAnyone.setAllowAnyone(true);
    }

    @Test
    void createSharedEntry_Success() {
        List<String> allowedEmails = List.of("allowed@example.com");
        SharedEntryDto input = new SharedEntryDto(entry.getId(), allowedEmails, false);

        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(userRepository.findAllByEmail(allowedEmails)).thenReturn(new ArrayList<>(List.of(allowedUser)));
        when(sharedEntryRepository.getAllByEntry(entry)).thenReturn(Collections.emptyList());
        when(sharedEntryRepository.save(any(SharedEntry.class))).thenAnswer(i -> i.getArgument(0));

        UUID publicId = sharedEntryService.createSharedEntry(ownerUser, input);

        assertNotNull(publicId);
        ArgumentCaptor<SharedEntry> captor = ArgumentCaptor.forClass(SharedEntry.class);
        verify(sharedEntryRepository).save(captor.capture());
        SharedEntry savedShare = captor.getValue();
        assertEquals(entry, savedShare.getEntry());
        assertFalse(savedShare.isAllowAnyone());
        assertTrue(savedShare.getAllowedUsers().contains(ownerUser));
        assertTrue(savedShare.getAllowedUsers().contains(allowedUser));
        assertEquals(2, savedShare.getAllowedUsers().size());
    }

    @Test
    void createSharedEntry_Success_AllowAnyone() {
        SharedEntryDto input = new SharedEntryDto(entry.getId(), Collections.emptyList(), true);

        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(userRepository.findAllByEmail(Collections.emptyList())).thenReturn(new ArrayList<>());
        when(sharedEntryRepository.getAllByEntry(entry)).thenReturn(Collections.emptyList());
        when(sharedEntryRepository.save(any(SharedEntry.class))).thenAnswer(i -> i.getArgument(0));

        UUID publicId = sharedEntryService.createSharedEntry(ownerUser, input);

        assertNotNull(publicId);
        ArgumentCaptor<SharedEntry> captor = ArgumentCaptor.forClass(SharedEntry.class);
        verify(sharedEntryRepository).save(captor.capture());
        SharedEntry savedShare = captor.getValue();
        assertEquals(entry, savedShare.getEntry());
        assertTrue(savedShare.isAllowAnyone());
        assertTrue(savedShare.getAllowedUsers().contains(ownerUser));
        assertEquals(1, savedShare.getAllowedUsers().size());
    }


    @Test
    void createSharedEntry_EntryNotFound() {
        SharedEntryDto input = new SharedEntryDto(99L, List.of("allowed@example.com"), false);
        when(entryRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.createSharedEntry(ownerUser, input);
        });
        assertEquals("Entry not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(sharedEntryRepository, never()).save(any());
    }

    @Test
    void createSharedEntry_NotAuthorized() {
        SharedEntryDto input = new SharedEntryDto(entry.getId(), List.of("allowed@example.com"), false);
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.createSharedEntry(nonAllowedUser, input);
        });
        assertEquals("Not authorized to share this entry", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(sharedEntryRepository, never()).save(any());
    }

    @Test
    void createSharedEntry_ActiveShareExists() {
        SharedEntryDto input = new SharedEntryDto(entry.getId(), List.of("allowed@example.com"), false);
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(sharedEntryRepository.getAllByEntry(entry)).thenReturn(List.of(sharedEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.createSharedEntry(ownerUser, input);
        });
        assertEquals("Already have an active share for this entry", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(sharedEntryRepository, never()).save(any());
    }

    @Test
    void accessSharedEntry_SuccessAllowedUser() {
        when(sharedEntryRepository.getByPublicId(sharedEntry.getPublicId())).thenReturn(Optional.of(sharedEntry));
        when(encryptionService.decrypt("EncryptedContent")).thenReturn("DecryptedContent");

        Entry accessedEntry = sharedEntryService.accessSharedEntry(allowedUser, sharedEntry.getPublicId());

        assertNotNull(accessedEntry);
        assertEquals(entry.getId(), accessedEntry.getId());
        assertEquals("DecryptedContent", accessedEntry.getContent());
        verify(encryptionService, times(1)).decrypt("EncryptedContent");
    }

    @Test
    void accessSharedEntry_SuccessAllowAnyone() {
        when(sharedEntryRepository.getByPublicId(sharedEntryAllowAnyone.getPublicId())).thenReturn(Optional.of(sharedEntryAllowAnyone));
        when(encryptionService.decrypt("EncryptedContent")).thenReturn("DecryptedContent");

        Entry accessedEntry = sharedEntryService.accessSharedEntry(nonAllowedUser, sharedEntryAllowAnyone.getPublicId());

        assertNotNull(accessedEntry);
        assertEquals(entry.getId(), accessedEntry.getId());
        assertEquals("DecryptedContent", accessedEntry.getContent());
        verify(encryptionService, times(1)).decrypt("EncryptedContent");
    }

    @Test
    void accessSharedEntry_ShareNotFound() {
        UUID nonExistentUUID = UUID.randomUUID();
        when(sharedEntryRepository.getByPublicId(nonExistentUUID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.accessSharedEntry(ownerUser, nonExistentUUID);
        });
        assertEquals("Share not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(encryptionService, never()).decrypt(anyString());
    }

    @Test
    void accessSharedEntry_NotAuthorized() {
        when(sharedEntryRepository.getByPublicId(sharedEntry.getPublicId())).thenReturn(Optional.of(sharedEntry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.accessSharedEntry(nonAllowedUser, sharedEntry.getPublicId());
        });
        assertEquals("Not authorized", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(encryptionService, never()).decrypt(anyString());
    }

    @Test
    void removeSharedEntry_Success() {
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(sharedEntryRepository.getAllByEntry(entry)).thenReturn(List.of(sharedEntry));

        assertDoesNotThrow(() -> sharedEntryService.removeSharedEntry(ownerUser, entry.getId()));

        verify(sharedEntryRepository).deleteAll(List.of(sharedEntry));
    }

    @Test
    void removeSharedEntry_EntryNotFound() {
        when(entryRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.removeSharedEntry(ownerUser, 99L);
        });
        assertEquals("Entry not found", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(sharedEntryRepository, never()).deleteAll(any());
    }

    @Test
    void removeSharedEntry_NotYourEntry() {
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            sharedEntryService.removeSharedEntry(nonAllowedUser, entry.getId());
        });
        assertEquals("Not your entry", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(sharedEntryRepository, never()).deleteAll(any());
    }
}