package dev.rubric.journalspring.service;

import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FolderServiceUnitTests {

    @Mock
    private FolderRepository folderRepository;

    @InjectMocks
    private FolderService folderService;

    @Test
    void createFolder_Success() {
        User user = new User();
        user.setId(1L);
        String folderName = "My Folder";

        folderService.createFolder(user, folderName);

        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderRepository, times(1)).save(folderCaptor.capture());
        Folder savedFolder = folderCaptor.getValue();

        assertEquals(user, savedFolder.getUser());
        assertEquals(folderName, savedFolder.getName());
    }

    @Test
    void getFolder_Success() {
        User user = new User();
        user.setId(1L);

        Folder folder = new Folder(user, "Folder1");
        folder.setId(100L);

        when(folderRepository.findById(100L)).thenReturn(Optional.of(folder));

        Folder returnedFolder = folderService.getFolder(user, 100L);
        assertNotNull(returnedFolder);
        assertEquals("Folder1", returnedFolder.getName());
        assertEquals(user, returnedFolder.getUser());
    }

    @Test
    void getFolder_NotFound() {
        User user = new User();
        user.setId(1L);

        when(folderRepository.findById(200L)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            folderService.getFolder(user, 200L);
        });
        assertEquals(String.format("folder with id '%d' does not exist", 200L), exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void getFolder_Unauthorized() {
        User user = new User();
        user.setId(1L);
        User anotherUser = new User();
        anotherUser.setId(2L);

        Folder folder = new Folder(anotherUser, "Folder of another user");
        folder.setId(300L);

        when(folderRepository.findById(300L)).thenReturn(Optional.of(folder));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            folderService.getFolder(user, 300L);
        });
        assertEquals(String.format("User with id %d is not authorized", user.getId()), exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void getAllUserFolders_Success() {
        User user = new User();
        user.setId(1L);

        Folder folder1 = new Folder(user, "Folder1");
        Folder folder2 = new Folder(user, "Folder2");

        List<Folder> folders = Arrays.asList(folder1, folder2);
        when(folderRepository.getAllByUser(user)).thenReturn(folders);

        List<Folder> returnedFolders = folderService.getAllUserFolders(user);
        assertEquals(2, returnedFolders.size());
        assertTrue(returnedFolders.contains(folder1));
        assertTrue(returnedFolders.contains(folder2));
    }

    @Test
    void deleteFolder_Success() {
        User user = new User();
        user.setId(1L);

        Folder folder = new Folder(user, "FolderToDelete");
        folder.setId(400L);

        when(folderRepository.findById(400L)).thenReturn(Optional.of(folder));

        folderService.deleteFolder(user, 400L);
        verify(folderRepository, times(1)).delete(folder);
    }

    @Test
    void deleteFolder_Unauthorized() {
        User user = new User();
        user.setId(1L);
        User anotherUser = new User();
        anotherUser.setId(2L);

        Folder folder = new Folder(anotherUser, "FolderOfAnotherUser");
        folder.setId(500L);

        when(folderRepository.findById(500L)).thenReturn(Optional.of(folder));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            folderService.deleteFolder(user, 500L);
        });
        assertEquals(String.format("User with id %d is not authorized", user.getId()), exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(folderRepository, never()).delete(any(Folder.class));
    }

    @Test
    void updateFolderName_Success() {
        User user = new User();
        user.setId(1L);

        Folder folder = new Folder(user, "Old Name");
        folder.setId(600L);

        when(folderRepository.findById(600L)).thenReturn(Optional.of(folder));
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newName = "New Folder Name";
        folderService.updateFolderName(user, 600L, newName);

        assertEquals(newName, folder.getName());
        verify(folderRepository, times(1)).save(folder);
    }

    @Test
    void updateFolderName_Unauthorized() {
        User user = new User();
        user.setId(1L);
        User anotherUser = new User();
        anotherUser.setId(2L);

        Folder folder = new Folder(anotherUser, "Folder Name");
        folder.setId(700L);

        when(folderRepository.findById(700L)).thenReturn(Optional.of(folder));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            folderService.updateFolderName(user, 700L, "Attempted New Name");
        });
        assertEquals(String.format("User with id %d is not authorized", user.getId()), exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        verify(folderRepository, never()).save(any(Folder.class));
    }
}
