package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final Logger logger = LoggerFactory.getLogger(AiController.class);

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/daily-prompt")
    public ResponseEntity<String> generateText(@AuthenticationPrincipal User user) {

        logger.debug("User {} is generating a ai prompt", user.getEmail());

        try {
            return ResponseEntity.ok(aiService.generatePrompt(user));
        } catch (ApplicationException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("API error: " + e.getMessage());
        }
    }
}
