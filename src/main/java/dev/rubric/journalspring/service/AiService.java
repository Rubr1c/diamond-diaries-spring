package dev.rubric.journalspring.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final EntryService entryService;

    public AiService(EntryService entryService, UserRepository userRepository) {
        this.entryService = entryService;
        this.userRepository = userRepository;
    }

    public String generatePrompt(User user) throws IOException {
        if (user.getAiCooldown() != null && user.getAiCooldown().isAfter(LocalDateTime.now())) {
            throw new ApplicationException(
                    "you can only request a prompt once every minute",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        boolean allowTitle = user.isAiAllowTitleAccess();
        boolean allowContent = user.isAiAllowContentAccess();


        String BASE_PROMPT = "This is a journaling app for personal entries. ";

        String jsonRequest;

        user.setAiCooldown(LocalDateTime.now().plusMinutes(1));
        userRepository.save(user);

        if (!allowTitle && !allowContent) {
            logger.info("Making general prompt for user {}", user.getId());

            jsonRequest = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" +
                    BASE_PROMPT +
                    "Generate a concise one-line journaling prompt for inspiration. Do not refer to any past entries.\" }]}]}";

        }
        else if (!allowContent) {
            List<String> titles = entryService.getUserEntries(user, 0, 20)
                    .stream()
                    .map(Entry::getTitle)
                    .toList();

            logger.info("Making prompt with titles for user {}", user.getId());

            jsonRequest = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" +
                    BASE_PROMPT +
                    "The user's last 20 entry titles are: " + titles +
                    ". Generate a concise one-line journaling prompt that draws inspiration from these titles. \" }]}]}";
        }
        else {
            Map<String, String> contentMap = entryService.getUserEntries(user, 0, 20)
                    .stream()
                    .collect(Collectors.toMap(Entry::getTitle, Entry::getContent));

            logger.info("Making prompt with titles and content for user {}", user.getId(

            ));

            jsonRequest = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" +
                    BASE_PROMPT +
                    "The user's last 20 entries with titles and content are: " + contentMap +
                    ". Generate a concise one-line journaling prompt that incorporates elements from these titles and content for inspiration.\" }]}]}";
        }


        RequestBody body = RequestBody.create(jsonRequest, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();



        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);


            return jsonResponse.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        }

    }

}
