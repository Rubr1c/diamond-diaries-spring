package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.response.UserResponse;
import dev.rubric.journalspring.service.AdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Value("${admin.api-key}")
    private String adminApiKey;

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader("Authorization") String authHeader) {
        // Check if it's a Bearer token and validate it
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        if (!adminApiKey.equals(token)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        return ResponseEntity.ok(adminService.getAllUsers());
    }
}