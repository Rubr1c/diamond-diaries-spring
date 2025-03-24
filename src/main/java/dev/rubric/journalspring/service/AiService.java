package dev.rubric.journalspring.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AiService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String BASE_PROMPT =
            "This is a journaling app made for people to make personal journal entries. " +
            "Give this user a prompt for inspiration (only send the prompt)";

    private final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    private final EntryService entryService;

    public AiService(EntryService entryService) {
        this.entryService = entryService;
    }

    public String generatePrompt(User user) throws IOException {
        boolean allowTitle = user.isAiAllowTitleAccess();
        boolean allowContent = user.isAiAllowContentAccess();


        String jsonRequest = "";

        if (!allowTitle && !allowContent) {
            logger.info("Making general prompt for user {}", user.getId());
            jsonRequest = "{ \"contents\": " +
                            "[{ \"parts\": " +
                                "[{ \"text\": \"" + BASE_PROMPT + "\" } ]} ]}";
        } else if (!allowContent) {
            List<String> titles = entryService.getUserEntries(user, 0, 20)
                                                .stream().map(Entry::getTitle).toList();

            logger.info("Making prompt with titles for user {}", user.getId());
            jsonRequest = "{ \"contents\": " +
                    "[{ \"parts\": " +
                    "[{ \"text\": \"" +
                    BASE_PROMPT +
                    "these were their last 20 prompt titles for inspiration or follow ups " +
                    "try to make it related to one of them that seems to most interesting or has potential for a follow up." +
                    "dont make it too long" +
                    titles +"\" } ]} ]}";
        }
        //TODO: content prompts


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
