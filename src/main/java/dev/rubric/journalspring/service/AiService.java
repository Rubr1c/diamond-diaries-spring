package dev.rubric.journalspring.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import okhttp3.*;
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
            jsonRequest = "{ \"contents\": " +
                            "[{ \"parts\": " +
                                "[{ \"text\": \"" + BASE_PROMPT + "\" } ]} ]}";
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
