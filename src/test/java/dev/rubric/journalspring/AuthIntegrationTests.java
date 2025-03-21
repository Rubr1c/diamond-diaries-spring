package dev.rubric.journalspring;

import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void signup_Success() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test",
                                "password": "P@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Created account successfully"));
    }

    @Test
    void signup_NullEmail() throws Exception{

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "test",
                                "password": "P@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing fields"));
    }

    @Test
    void signup_NullUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "password": "P@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing fields"));

    }

    @Test
    void signup_NullPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing fields"));


    }

    @Test
    void signup_AllNullInput() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());

    }


    @Test
    void signup_InvalidEmail() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test",
                                "username": "test",
                                "password": "P@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Invalid email"));

    }

    @Test
    void signup_ShortUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "te",
                                "password": "P@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Username does not meet requirements"));

    }

    @Test
    void signup_LongUsername() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "teststststststststststststst",
                                "password": "P@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Username does not meet requirements"));

    }

    @Test
    void signup_ShortPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test",
                                "password": "P@1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Password does not meet requirements"));

    }

    @Test
    void signup_NoUpperCaseCharacterInPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test",
                                "password": "p@ssword1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Password does not meet requirements"));

    }

    @Test
    void signup_NoLowerCaseCharacterInPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test",
                                "password": "P@SSWORD1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Password does not meet requirements"));

    }

    @Test
    void signup_NoDigitInPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test",
                                "password": "P@ssword"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Password does not meet requirements"));

    }

    @Test
    void signup_NoSpecialCharacterInPassword() throws Exception {

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "email": "test@example.com",
                                "username": "test",
                                "password": "Password1"
                            }
                        """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Password does not meet requirements"));

    }

    @Test
    void signup_AlreadyExistingEmail() throws Exception{
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setUsername("existingUser");
        existingUser.setPassword(passwordEncoder.encode("P@ssword1"));

        userRepository.save(existingUser);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "email": "test@example.com",
                    "username": "newUser",
                    "password": "P@ssword1"
                }
            """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void login_Success() throws Exception {
        // Create and save a user first
        User user = new User();
        user.setEmail("login@example.com");
        user.setUsername("logintest");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "login@example.com",
                            "password": "P@ssword1"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "nonexistent@example.com",
                            "password": "WrongP@ssword1"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void login_AccountNotVerified() throws Exception {
        User user = new User();
        user.setEmail("unverified@example.com");
        user.setUsername("unverified");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "unverified@example.com",
                            "password": "P@ssword1"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andExpect(content().string("Account not verified. Please verify your account."));
    }

    @Test
    void login_2FARequired() throws Exception {
        User user = new User();
        user.setEmail("2fa@example.com");
        user.setUsername("2fatest");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(true);
        user.setEnabled2fa(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "2fa@example.com",
                            "password": "P@ssword1"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isAccepted())
                .andExpect(content().string("2FA code sent to email"));
    }

    @Test
    void verify2FA_Success() throws Exception {
        User user = new User();
        user.setEmail("verify2fa@example.com");
        user.setUsername("verify2fa");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(true);
        user.setEnabled2fa(true);
        user.setVerificationCode("123456");
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/verify-2fa")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "verify2fa@example.com",
                            "verificationCode": "123456"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void verify2FA_InvalidCode() throws Exception {
        User user = new User();
        user.setEmail("verify2fa_invalid@example.com");
        user.setUsername("verify2fa_invalid");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(true);
        user.setEnabled2fa(true);
        user.setVerificationCode("123456");
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/verify-2fa")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "verify2fa_invalid@example.com",
                            "verificationCode": "654321"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid 2FA code"));
    }

    @Test
    void verifyUser_Success() throws Exception {
        User user = new User();
        user.setEmail("verify@example.com");
        user.setUsername("verify");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(false);
        user.setVerificationCode("123456");
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/verify")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "verify@example.com",
                            "verificationCode": "123456"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Account verified"));
    }

    @Test
    void verifyUser_InvalidCode() throws Exception {
        User user = new User();
        user.setEmail("verify_invalid@example.com");
        user.setUsername("verify_invalid");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(false);
        user.setVerificationCode("123456");
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/verify")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "email": "verify_invalid@example.com",
                            "code": "654321"
                        }
                    """))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid verification code"));
    }

    @Test
    void resendVerificationCode_Success() throws Exception {
        User user = new User();
        user.setEmail("resend@example.com");
        user.setUsername("resend");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "resend@example.com"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Code Resent"));
    }

    @Test
    void resendVerificationCode_AlreadyVerified() throws Exception {
        User user = new User();
        user.setEmail("already_verified@example.com");
        user.setUsername("already_verified");
        user.setPassword(passwordEncoder.encode("P@ssword1"));
        user.setActivated(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "already_verified@example.com"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account is already verified"));
    }

    @Test
    void resendVerificationCode_UserNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "nonexistent@example.com"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    void googleLogin_ReturnsRedirectInfo() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login/google")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
    }

}