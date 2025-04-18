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

@Service
public class SharedEntryService {

    private final UserRepository userRepository;
    private final SharedEntryRepository sharedEntryRepository;
    private final EncryptionService encryptionService;
    private final EntryRepository entryRepository;

    public SharedEntryService(UserRepository userRepository, SharedEntryRepository sharedEntryRepository, EncryptionService encryptionService, EntryRepository entryRepository) {
        this.userRepository = userRepository;
        this.sharedEntryRepository = sharedEntryRepository;
        this.encryptionService = encryptionService;
        this.entryRepository = entryRepository;
    }


    public UUID createSharedEntry(User user,
                                  SharedEntryDto input) {
        Entry entry = entryRepository.findById(input.entryId())
                .orElseThrow(() -> new ApplicationException(
                        "Entry not found", HttpStatus.NOT_FOUND));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ApplicationException(
                    "Not authorized to share this entry", HttpStatus.UNAUTHORIZED);
        }

        List<SharedEntry> existing = sharedEntryRepository.getAllByEntry(entry);
        existing.stream()
                .filter(e -> e.getExpiryTime().isAfter(ZonedDateTime.now()))
                .findAny()
                .ifPresent(e -> { throw new ApplicationException(
                        "Already have an active share for this entry", HttpStatus.CONFLICT);
                });

        List<User> allowed = userRepository.findAllByEmail(input.allowedUsers());
        allowed.add(user);

        SharedEntry s = new SharedEntry(
                entry,
                ZonedDateTime.now().plusDays(1),
                allowed,
                Boolean.TRUE.equals(input.allowAnyone())
        );
        sharedEntryRepository.save(s);
        return s.getPublicId();
    }

    public Entry accessSharedEntry(User user,
                                   UUID sharedEntryUUID) {

        SharedEntry s = sharedEntryRepository.getByPublicId(sharedEntryUUID)
                .orElseThrow(() -> new ApplicationException(
                        "Share not found", HttpStatus.NOT_FOUND));

        boolean allowed = s.isAllowAnyone()
                || s.getAllowedUsers().stream()
                .map(User::getId)
                .anyMatch(id -> id.equals(user.getId()));

        if (!allowed) {
            throw new ApplicationException(
                    "Not authorized", HttpStatus.UNAUTHORIZED);
        }

        Entry e = s.getEntry();
        // decrypt before handing it back
        e.setContent(encryptionService.decrypt(e.getContent()));
        return e;
    }

    public void userCanAccessEntry(User user, Long entryId) {
       Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        "Entry not found", HttpStatus.NOT_FOUND)
                );
       
        SharedEntry sEntry = 
                sharedEntryRepository.getAllByEntryOrderByExpiryTimeDesc(entry).get(0);
        
        if (sEntry == null) throw new ApplicationException("Shared Entry not found", HttpStatus.NOT_FOUND);
        
        if (sEntry.getExpiryTime().isAfter(ZonedDateTime.now()))
            throw new ApplicationException("Shared Entry expired", HttpStatus.NOT_FOUND);

        boolean isAllowed = sEntry.getAllowedUsers()
                .stream()
                .map(User::getId)
                .anyMatch(id -> id.equals(user.getId()));

        if (!isAllowed && !sEntry.isAllowAnyone()) {
            throw new ApplicationException(
                    String.format("User with id %d is not authorized", user.getId()),
                    HttpStatus.UNAUTHORIZED
            );
        }
        
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

    public void removeUserFromSharedEntry(User user,
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

    public void removeSharedEntry(User user, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ApplicationException(
                        "Entry not found", HttpStatus.NOT_FOUND));

        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ApplicationException(
                    "Not your entry", HttpStatus.UNAUTHORIZED);
        }
        List<SharedEntry> list = sharedEntryRepository.getAllByEntry(entry);
        sharedEntryRepository.deleteAll(list);
    }
}
