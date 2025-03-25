package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.dto.RegisterUserDto;
import dev.rubric.journalspring.dto.UpdatePasswordDto;
import dev.rubric.journalspring.dto.VerifyUserDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found", HttpStatus.NOT_FOUND));

        user.setVerificationCode(generateVerificationCode());
        user.setCodeExp(LocalDateTime.now().plusHours(1));
        sendPasswordResetEmail(user);
        userRepository.save(user);
    }

    public void updatePassword(UpdatePasswordDto input) {
        User user = userRepository.findByEmail(input.email())
                .orElseThrow(() -> new ApplicationException("User not found", HttpStatus.NOT_FOUND));

        if (user.getVerificationCode().isEmpty()) {
            throw new ApplicationException("No password reset was requested", HttpStatus.BAD_REQUEST);
        }

        if (user.getCodeExp().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("Verification code has expired", HttpStatus.BAD_REQUEST);
        }

        if (!user.getVerificationCode().get().equals(input.verificationCode())) {
            throw new ApplicationException("Invalid verification code", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(input.newPassword()));
        user.setVerificationCode(null);
        user.setCodeExp(null);
        userRepository.save(user);
    }

    private void sendPasswordResetEmail(User user) {
        String subject = "Diamond Diaries: Password Reset Request";
        if (user.getVerificationCode().isEmpty()) {
            throw new ApplicationException("Verification code not set", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String verificationCode = user.getVerificationCode().get();
        String htmlMessage = "<html>" +
                "<head>" +
                "  <style>" +
                "    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #fafafa; margin: 0; padding: 0; }" +
                "    .container { max-width: 600px; margin: 50px auto; background-color: #ffffff; padding: 30px; border: 1px solid #eaeaea; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }" +
                "    .header { text-align: center; padding-bottom: 20px; }" +
                "    .header h1 { margin: 0; color: #333333; }" +
                "    .content { color: #555555; font-size: 16px; line-height: 1.5; }" +
                "    .verification-code { display: inline-block; margin: 20px 0; padding: 10px 20px; background-color: #007bff; color: #ffffff; font-size: 20px; font-weight: bold; border-radius: 5px; }" +
                "    .footer { text-align: center; font-size: 12px; color: #999999; padding-top: 20px; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"container\">" +
                "    <div class=\"header\">" +
                "      <h1>Diamond Diaries</h1>" +
                "    </div>" +
                "    <div class=\"content\">" +
                "      <p>We received a request to reset your password.</p>" +
                "      <p>Please use the verification code below to reset your password:</p>" +
                "      <div class=\"verification-code\">" + verificationCode + "</div>" +
                "      <p>If you did not request a password reset, please ignore this email.</p>" +
                "    </div>" +
                "    <div class=\"footer\">" +
                "      <p>&copy; 2024 Diamond Diaries. All rights reserved.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";

        try {
            mailService.sendEmail(user.getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            throw new ApplicationException("Failed to send password reset email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            MailService mailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    private boolean passwordIsValid(String password) {
        return password.length() >= 8 &&
                password.chars().mapToObj(c -> (char) c).anyMatch(Character::isUpperCase) &&
                password.chars().mapToObj(c -> (char) c).anyMatch(Character::isLowerCase) &&
                password.chars().mapToObj(c -> (char) c).anyMatch(Character::isDigit) &&
                password.chars().mapToObj(c -> (char) c).anyMatch(ch -> !Character.isLetterOrDigit(ch));
    }

    private boolean emailIsValid(String email) {
        if (email == null) {
            return false;
        }
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return email.matches(emailRegex);
    }

    private boolean usernameIsValid(String username) {
        return username.length() >= 3 && username.length() <= 16;
    }

    public User signup(RegisterUserDto input) {

        if (input.email() == null ||
                input.username() == null ||
                input.password() == null) {
            throw new ApplicationException("Missing fields", HttpStatus.BAD_REQUEST);
        }

        if (!emailIsValid(input.email())) {
            throw new ApplicationException("Invalid email", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new ApplicationException("Email already registered", HttpStatus.CONFLICT);
        }

        if (!usernameIsValid(input.username())) {
            throw new ApplicationException("Username does not meet requirements", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!passwordIsValid(input.password())) {
            throw new ApplicationException("Password does not meet requirements", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User user = new User(
                null,
                input.username(),
                input.email(),
                passwordEncoder.encode(input.password()),
                "");

        user.setVerificationCode(generateVerificationCode());
        user.setCodeExp(LocalDateTime.now().plusMinutes(15));
        sendVerificationEmail(user);
        logger.info("New user with id '{}' registered", user.getId());
        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.email())
                .orElseThrow(() -> new ApplicationException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isEnabled()) {
            throw new ApplicationException("Account not verified. Please verify your account.", HttpStatus.FORBIDDEN);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            input.email(),
                            input.password()));

            user.setLastLogin(ZonedDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            throw new ApplicationException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        if (user.isEnabled2fa()) {
            user.setVerificationCode(generateVerificationCode());
            user.setCodeExp(LocalDateTime.now().plusMinutes(15));
            sendTwoFactorEmail(user);
            userRepository.save(user);
            throw new ApplicationException("2FA code sent to email", HttpStatus.ACCEPTED);
        }

        return user;
    }

    public User verify2FA(VerifyUserDto input) {
        User user = userRepository.findByEmail(input.email())
                .orElseThrow(() -> new ApplicationException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isEnabled2fa()) {
            throw new ApplicationException("2FA is not enabled for this account", HttpStatus.BAD_REQUEST);
        }

        if (user.getVerificationCode().isEmpty()) {
            throw new ApplicationException("No 2FA code was requested", HttpStatus.BAD_REQUEST);
        }

        if (user.getCodeExp().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("2FA code has expired", HttpStatus.BAD_REQUEST);
        }

        if (!user.getVerificationCode().get().equals(input.verificationCode())) {
            throw new ApplicationException("Invalid 2FA code", HttpStatus.BAD_REQUEST);
        }

        user.setVerificationCode(null);
        user.setCodeExp(null);
        userRepository.save(user);
        return user;
    }

    private void sendTwoFactorEmail(User user) {
        String subject = "Diamond Diaries: Two-Factor Authentication Code";
        if (user.getVerificationCode().isEmpty()) {
            throw new ApplicationException("2FA code not set", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String verificationCode = user.getVerificationCode().get();
        String htmlMessage = "<html>" +
                "<head>" +
                "  <style>" +
                "    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #fafafa; margin: 0; padding: 0; }"
                +
                "    .container { max-width: 600px; margin: 50px auto; background-color: #ffffff; padding: 30px; border: 1px solid #eaeaea; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }"
                +
                "    .header { text-align: center; padding-bottom: 20px; }" +
                "    .header h1 { margin: 0; color: #333333; }" +
                "    .content { color: #555555; font-size: 16px; line-height: 1.5; }" +
                "    .verification-code { display: inline-block; margin: 20px 0; padding: 10px 20px; background-color: #007bff; color: #ffffff; font-size: 20px; font-weight: bold; border-radius: 5px; }"
                +
                "    .footer { text-align: center; font-size: 12px; color: #999999; padding-top: 20px; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"container\">" +
                "    <div class=\"header\">" +
                "      <h1>Diamond Diaries</h1>" +
                "    </div>" +
                "    <div class=\"content\">" +
                "      <p>A login attempt was made to your Diamond Diaries account.</p>" +
                "      <p>Please use the verification code below to complete your login:</p>" +
                "      <div class=\"verification-code\">" + verificationCode + "</div>" +
                "      <p>If you did not attempt to log in, please secure your account immediately.</p>" +
                "    </div>" +
                "    <div class=\"footer\">" +
                "      <p>&copy; 2024 Diamond Diaries. All rights reserved.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";

        try {
            mailService.sendEmail(user.getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            throw new ApplicationException("Failed to send 2FA email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void verifyUser(VerifyUserDto input) {
        User user = userRepository.findByEmail(input.email())
                .orElseThrow(() -> new ApplicationException("User not found", HttpStatus.NOT_FOUND));

        if (user.getVerificationCode().isEmpty()) {
            throw new ApplicationException("User is already verified", HttpStatus.BAD_REQUEST);
        }

        if (user.getCodeExp().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("Verification code has expired", HttpStatus.BAD_REQUEST);
        }

        if (!user.getVerificationCode().get().equals(input.verificationCode())) {
            throw new ApplicationException("Invalid verification code", HttpStatus.BAD_REQUEST);
        }

        user.setActivated(true);
        user.setVerificationCode(null);
        user.setCodeExp(null);
        userRepository.save(user);
    }

    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found", HttpStatus.NOT_FOUND));

        if (user.isEnabled()) {
            throw new ApplicationException("Account is already verified", HttpStatus.BAD_REQUEST);
        }

        user.setVerificationCode(generateVerificationCode());
        user.setCodeExp(LocalDateTime.now().plusHours(1));
        sendVerificationEmail(user);
        userRepository.save(user);
    }

    private void sendVerificationEmail(User user) {
        String subject = "Diamond Diaries: Account Verification";
        if (user.getVerificationCode().isEmpty()) {
            throw new ApplicationException("Verification code not set", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String verificationCode = user.getVerificationCode().get();
        String htmlMessage = "<html>" +
                "<head>" +
                "  <style>" +
                "    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #fafafa; margin: 0; padding: 0; }"
                +
                "    .container { max-width: 600px; margin: 50px auto; background-color: #ffffff; padding: 30px; border: 1px solid #eaeaea; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }"
                +
                "    .header { text-align: center; padding-bottom: 20px; }" +
                "    .header h1 { margin: 0; color: #333333; }" +
                "    .content { color: #555555; font-size: 16px; line-height: 1.5; }" +
                "    .verification-code { display: inline-block; margin: 20px 0; padding: 10px 20px; background-color: #007bff; color: #ffffff; font-size: 20px; font-weight: bold; border-radius: 5px; }"
                +
                "    .footer { text-align: center; font-size: 12px; color: #999999; padding-top: 20px; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"container\">" +
                "    <div class=\"header\">" +
                "      <!-- TODO: Replace with Diamond Diaries logo -->" +
                "      <h1>Diamond Diaries</h1>" +
                "    </div>" +
                "    <div class=\"content\">" +
                "      <p>Welcome to Diamond Diaries, your personal journaling companion.</p>" +
                "      <p>Please use the verification code below to activate your account:</p>" +
                "      <div class=\"verification-code\">" + verificationCode + "</div>" +
                "      <p>If you did not sign up for a Diamond Diaries account, please ignore this email.</p>" +
                "    </div>" +
                "    <div class=\"footer\">" +
                "      <p>&copy; 2024 Diamond Diaries. All rights reserved.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";

        try {
            mailService.sendEmail(user.getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            throw new ApplicationException("Failed to send verification email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
