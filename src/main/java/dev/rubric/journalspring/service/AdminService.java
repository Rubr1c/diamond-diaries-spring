package dev.rubric.journalspring.service;

import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import dev.rubric.journalspring.response.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }
}