package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.dto.RegisterUserDto;
import dev.rubric.journalspring.dto.UpdatePasswordDto;
import dev.rubric.journalspring.dto.VerifyUserDto;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.LoginResponse;
import dev.rubric.journalspring.service.AuthService;
import dev.rubric.journalspring.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    private final AuthService authenticationService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(JwtService jwtService, AuthService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> register(@RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok("Created account successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        authenticationService.verifyUser(verifyUserDto);
        return ResponseEntity.ok("Account verified");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam String email) {
        authenticationService.resendVerificationCode(email);
        return ResponseEntity.ok("Code Resent");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> initiatePasswordReset(@RequestParam String email) {
        authenticationService.initiatePasswordReset(email);
        return ResponseEntity.ok("Password reset email sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody UpdatePasswordDto updatePasswordDto) {
        authenticationService.updatePassword(updatePasswordDto);
        return ResponseEntity.ok("Password updated successfully");
    }

    @GetMapping("/login/google")
    public ResponseEntity<String> googleLogin() {
        String googleAuthUrl = "/oauth2/authorization/google";
        return ResponseEntity.ok("To login with Google, redirect to: " + googleAuthUrl);
    }
}
