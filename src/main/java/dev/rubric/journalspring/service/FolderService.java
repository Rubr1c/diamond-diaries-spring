package dev.rubric.journalspring.service;

import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Folder;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.FolderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    public void createFolder(User user, String name) {
        Folder folder = new Folder(user, name);

        folderRepository.save(folder);
    }

    public Folder getFolder(User user, Long id) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(
                        String.format("folder with id '%d' does not exist", id),
                        HttpStatus.NOT_FOUND)
                );

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        return folder;
    }

    public Folder getFolderByPublicId(User user, UUID publicId) {
        Folder folder = folderRepository.getByPublicId(publicId)
                .orElseThrow(() -> new ApplicationException(
                        String.format("folder with id '%d' does not exist", publicId),
                        HttpStatus.NOT_FOUND)
                );

        if (!folder.getUser().getId().equals(user.getId())) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        return folder;
    }

    public List<Folder> getAllUserFolders(User user) {
        return folderRepository.getAllByUser(user);
    }

    public void deleteFolder(User user, Long id) {
        Folder folder = getFolder(user, id);

        if (!folder.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        folderRepository.delete(folder);
    }

    public void updateFolderName(User user, Long id, String name) {
        Folder folder = getFolder(user, id);

        if (!folder.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        folder.setName(name);
        folderRepository.save(folder);
    }
}
