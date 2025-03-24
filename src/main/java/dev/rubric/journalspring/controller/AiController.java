package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final AuthUtil authUtil;

    public AiController(AiService aiService, AuthUtil authUtil) {
        this.aiService = aiService;
        this.authUtil = authUtil;
    }

    @PostMapping("/daily-prompt")
    public String generateText() throws IOException {
        return aiService.generatePrompt(authUtil.getAuthenticatedUser());
    }
}
