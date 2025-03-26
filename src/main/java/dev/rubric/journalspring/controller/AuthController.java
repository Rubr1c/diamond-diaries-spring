package dev.rubric.journalspring.controller;

import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.dto.RegisterUserDto;
import dev.rubric.journalspring.dto.UpdatePasswordDto;
import dev.rubric.journalspring.dto.VerifyUserDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.response.LoginResponse;
import dev.rubric.journalspring.service.AuthService;
import dev.rubric.journalspring.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
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

        logger.debug("New user trying to sign up with email '{}'", registerUserDto.email());

        authenticationService.signup(registerUserDto);
        return ResponseEntity.ok("Created account successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginUserDto loginUserDto) {

        logger.debug("User '{}' trying to login", loginUserDto.email());

        try {
            User authenticatedUser = authenticationService.authenticate(loginUserDto);
            String jwtToken = jwtService.generateToken(authenticatedUser);
            LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
            return ResponseEntity.ok(loginResponse);
        } catch (ApplicationException e) {
            if (e.getStatus() == HttpStatus.ACCEPTED) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(e.getMessage());
            }
            throw e;
        }
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<LoginResponse> verify2FA(@RequestBody VerifyUserDto verifyUserDto) {

        logger.debug("User '{}' is verifying with 2fa", verifyUserDto.email());

        User authenticatedUser = authenticationService.verify2FA(verifyUserDto);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {

        logger.debug("User '{}' is verifying their account", verifyUserDto.email());

        authenticationService.verifyUser(verifyUserDto);
        return ResponseEntity.ok("Account verified");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam String email) {

        logger.debug("User '{}' has resent verification email", email);

        authenticationService.resendVerificationCode(email);
        return ResponseEntity.ok("Code Resent");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> initiatePasswordReset(@RequestParam String email) {

        logger.debug("User '{}' has forgotten password", email);

        authenticationService.initiatePasswordReset(email);
        return ResponseEntity.ok("Password reset email sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody UpdatePasswordDto updatePasswordDto) {

        logger.debug("User '{}' is changing their password", updatePasswordDto.email());

        authenticationService.updatePassword(updatePasswordDto);
        return ResponseEntity.ok("Password updated successfully");
    }
}
