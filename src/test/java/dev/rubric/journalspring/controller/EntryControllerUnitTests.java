package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.EntryResponse;
import dev.rubric.journalspring.service.EntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EntryControllerUnitTests {

    @Mock
    EntryService entryService;

    @Mock
    AuthUtil authUtil;

    @InjectMocks
    EntryController entryController;

    User mockUser;
    Entry mockEntry;
    EntryResponse mockEntryResponse;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = new User();
        mockUser.setId(1L);

        mockEntry = new Entry();
        mockEntry.setId(1L);

        mockEntryResponse = new EntryResponse(mockEntry);
    }

    @Test
    void getEntryById_Success() {
        when(entryService.getEntryById(mockUser, mockEntry.getId())).thenReturn(mockEntry);

        ResponseEntity<EntryResponse> response = entryController.getEntryById(mockEntry.getId());

        EntryResponse entryResponse = response.getBody();
        assertNotNull(entryResponse);
        assertEquals(mockEntry.getTitle(), entryResponse.getTitle());
        assertEquals(mockEntry.getContent(), entryResponse.getContent());
    }
}
