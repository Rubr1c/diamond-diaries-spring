package dev.rubric.journalspring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntryService entryService;

    @Mock
    private OkHttpClient mockClient;

    @InjectMocks
    private AiService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        Field clientField = AiService.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(aiService, mockClient);
    }

    @Test
    void generatePrompt_CooldownActive() {
        User user = new User();
        user.setAiCooldown(LocalDateTime.now().plusSeconds(30));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            aiService.generatePrompt(user);
        });
        assertEquals("you can only request a prompt once every minute", exception.getMessage());
    }

    @Test
    void generatePrompt_GeneralPrompt() throws Exception {
        User user = new User();
        user.setAiAllowTitleAccess(false);
        user.setAiAllowContentAccess(false);
        user.setAiCooldown(LocalDateTime.now().minusMinutes(1));

        String fakeResponseJson = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Test prompt\"}]}}]}";
        Response fakeResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .body(ResponseBody.create(fakeResponseJson, MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String prompt = aiService.generatePrompt(user);
        assertEquals("Test prompt", prompt);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.getAiCooldown().isAfter(LocalDateTime.now()));
    }

    @Test
    void generatePrompt_WithTitlesOnly() throws Exception {
        User user = new User();
        user.setAiAllowTitleAccess(true);
        user.setAiAllowContentAccess(false);
        user.setAiCooldown(LocalDateTime.now().minusMinutes(1));

        Entry entry1 = new Entry();
        entry1.setTitle("Morning Thoughts");
        Entry entry2 = new Entry();
        entry2.setTitle("Evening Reflections");
        List<Entry> entries = List.of(entry1, entry2);

        when(entryService.getUserEntries(user, 0, 20)).thenReturn(entries);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String fakeResponseJson = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Prompt based on titles\"}]}}]}";
        Response fakeResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .body(ResponseBody.create(fakeResponseJson, MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        String prompt = aiService.generatePrompt(user);
        assertEquals("Prompt based on titles", prompt);

        verify(entryService, times(1)).getUserEntries(user, 0, 20);
    }

    @Test
    void generatePrompt_WithTitlesAndContent() throws Exception {
        User user = new User();
        user.setAiAllowTitleAccess(true);
        user.setAiAllowContentAccess(true);
        user.setAiCooldown(LocalDateTime.now().minusMinutes(1));

        Entry entry1 = new Entry();
        entry1.setTitle("Gratitude");
        entry1.setContent("Today I am thankful for my family.");
        Entry entry2 = new Entry();
        entry2.setTitle("Challenge");
        entry2.setContent("Faced a difficult decision at work.");
        List<Entry> entries = List.of(entry1, entry2);

        when(entryService.getUserEntries(user, 0, 20)).thenReturn(entries);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String fakeResponseJson = "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"Prompt based on entries\"}]}}]}";
        Response fakeResponse = new Response.Builder()
                .code(200)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .body(ResponseBody.create(fakeResponseJson, MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        String prompt = aiService.generatePrompt(user);
        assertEquals("Prompt based on entries", prompt);

        verify(entryService, times(1)).getUserEntries(user, 0, 20);
    }

    @Test
    void generatePrompt_ApiCallFails() throws Exception {
        User user = new User();
        user.setAiAllowTitleAccess(false);
        user.setAiAllowContentAccess(false);
        user.setAiCooldown(LocalDateTime.now().minusMinutes(1));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Response fakeResponse = new Response.Builder()
                .code(500)
                .message("Internal Server Error")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .body(ResponseBody.create("", MediaType.get("application/json")))
                .build();

        Call mockCall = mock(Call.class);
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(fakeResponse);

        IOException exception = assertThrows(IOException.class, () -> {
            aiService.generatePrompt(user);
        });
        assertTrue(exception.getMessage().contains("Unexpected response"));
    }
}
