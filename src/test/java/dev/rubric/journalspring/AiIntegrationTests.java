package dev.rubric.journalspring;

import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @MockBean
    private AuthUtil authUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user with minimal required properties.
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    @WithMockUser
    void dailyPrompt_Success() throws Exception {
        when(authUtil.getAuthenticatedUser()).thenReturn(testUser);

        when(aiService.generatePrompt(any(User.class))).thenReturn("Generated prompt for the day");

        mockMvc.perform(post("/api/v1/ai/daily-prompt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Generated prompt for the day"));
    }

    @Test
    @WithMockUser
    void dailyPrompt_RespectsCooldownPeriod() throws Exception {
        testUser.setAiCooldown(LocalDateTime.now().plusSeconds(30));

        when(authUtil.getAuthenticatedUser()).thenReturn(testUser);

        when(aiService.generatePrompt(any(User.class))).thenThrow(
                new ApplicationException(
                        "you can only request a prompt once every minute",
                        HttpStatus.TOO_MANY_REQUESTS
                )
        );

        mockMvc.perform(post("/api/v1/ai/daily-prompt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(containsString("you can only request a prompt once every minute")));
    }

    @Test
    @WithMockUser
    void dailyPrompt_AllowsAfterCooldownExpires() throws Exception {
        testUser.setAiCooldown(LocalDateTime.now().minusMinutes(2));

        when(authUtil.getAuthenticatedUser()).thenReturn(testUser);

        when(aiService.generatePrompt(any(User.class))).thenReturn("Generated prompt after cooldown");

        mockMvc.perform(post("/api/v1/ai/daily-prompt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Generated prompt after cooldown"));
    }

}
