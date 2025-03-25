package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.config.AuthUtil;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> generateText() {
        try {
            return ResponseEntity.ok(aiService.generatePrompt(authUtil.getAuthenticatedUser()));
        } catch (ApplicationException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("API error: " + e.getMessage());
        }
    }
}
