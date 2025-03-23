package dev.rubric.journalspring.service;


import dev.rubric.journalspring.dto.SharedEntryDto;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.SharedEntry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.SharedEntryRepository;
import dev.rubric.journalspring.repository.UserRepository;
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

    public SharedEntryService(EntryService entryService, UserRepository userRepository, SharedEntryRepository sharedEntryRepository) {
        this.entryService = entryService;
        this.userRepository = userRepository;
        this.sharedEntryRepository = sharedEntryRepository;
    }


    public UUID createSharedEntry(User user,
                                  SharedEntryDto input) {
        Entry entry = entryService.getEntryById(user, input.entryId());

        List<User> allowedUsers = userRepository.findAllByEmail(input.allowedUsers());

        SharedEntry sharedEntry = new SharedEntry(
                entry,
                ZonedDateTime.now().plusDays(1),
                allowedUsers,
                input.allowAnyone() != null ? input.allowAnyone() : false
        );


        sharedEntryRepository.save(sharedEntry);
        return sharedEntry.getPublicId();
    }
}
