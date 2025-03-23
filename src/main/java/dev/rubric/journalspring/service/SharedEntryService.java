package dev.rubric.journalspring.service;


import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SharedEntry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.SharedEntryRepository;
import dev.rubric.journalspring.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class SharedEntryService {

    private final EntryService entryService;
    private final UserRepository userRepository;
    private final SharedEntryRepository sharedEntryRepository;
    private final EntryRepository entryRepository;

    public SharedEntryService(EntryService entryService, UserRepository userRepository, SharedEntryRepository sharedEntryRepository, EntryRepository entryRepository) {
        this.entryService = entryService;
        this.userRepository = userRepository;
        this.sharedEntryRepository = sharedEntryRepository;
        this.entryRepository = entryRepository;
    }


    public UUID createSharedEntry(User user,
                                  SharedEntryDto input) {
        Entry entry = entryService.getEntryById(user, input.entryId());

        List<SharedEntry> existingSharedEntries =
                sharedEntryRepository.getAllByEntry(entry);

        for (SharedEntry e: existingSharedEntries) {
            if (e.getExpiryTime().isAfter(ZonedDateTime.now())) {
                throw new ApplicationException(
                      String.format("There already exists a shared entry for entry id '%d'", entry.getId()),
                      HttpStatus.CONFLICT
                );
            }
        }

        List<User> allowedUsers = userRepository.findAllByEmail(input.allowedUsers());
        allowedUsers.add(user);

        SharedEntry sharedEntry = new SharedEntry(
                entry,
                ZonedDateTime.now().plusDays(1),
                allowedUsers,
                input.allowAnyone() != null ? input.allowAnyone() : false
        );


        sharedEntryRepository.save(sharedEntry);
        return sharedEntry.getPublicId();
    }

    public Entry accessSharedEntry(User user,
                                   UUID sharedEntryUUID) {

        SharedEntry sEntry = sharedEntryRepository.getByPublicId(sharedEntryUUID)
                .orElseThrow(() -> new ApplicationException(
                        String.format("No shared entry with public id '%s' found", sharedEntryUUID), HttpStatus.NOT_FOUND
                        )
                );

        if (!sEntry.getAllowedUsers().contains(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED
            );
        }

        return sEntry.getEntry();
    }

    public void addUserToSharedEntry(User user,
                                     UUID sEntryUUID,
                                     String targetEmail) {
        SharedEntry sEntry = sharedEntryRepository.getByPublicId(sEntryUUID)
                .orElseThrow(() -> new ApplicationException(
                                String.format("No shared entry with public id '%s' found", sEntryUUID), HttpStatus.NOT_FOUND
                        )
                );

        Entry entry = sEntry.getEntry();

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        User target = userRepository.findByEmail(targetEmail).orElseThrow(
                () -> new ApplicationException(
                        String.format("User with email '%s' does not exist", targetEmail)
                        , HttpStatus.NOT_FOUND
                )
        );

        sEntry.addToAllowedUsers(target);
        sharedEntryRepository.save(sEntry);
    }

    public void removeUserToSharedEntry(User user,
                                        UUID sEntryUUID,
                                        String targetEmail) {
        SharedEntry sEntry = sharedEntryRepository.getByPublicId(sEntryUUID)
                .orElseThrow(() -> new ApplicationException(
                                String.format("No shared entry with public id '%s' found", sEntryUUID), HttpStatus.NOT_FOUND
                        )
                );

        Entry entry = sEntry.getEntry();

        if (!entry.getUser().equals(user)) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED);
        }

        User target = userRepository.findByEmail(targetEmail).orElseThrow(
                () -> new ApplicationException(
                        String.format("User with email '%s' does not exist", targetEmail)
                        , HttpStatus.NOT_FOUND
                )
        );

        if (!sEntry.removeFromAllowedUsers(target)) {
            throw new ApplicationException(
                    String.format("User '%s' already does not have access to entry"),
                    HttpStatus.NOT_FOUND
            );
        }

        sharedEntryRepository.save(sEntry);
    }
}
