package dev.rubric.journalspring.service;

import dev.rubric.journalspring.dto.LoginUserDto;
import dev.rubric.journalspring.dto.RegisterUserDto;
import dev.rubric.journalspring.dto.VerifyUserDto;
import dev.rubric.journalspring.exception.ApplicationException;
import dev.rubric.journalspring.models.Entry;
import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.EntryRepository;
import dev.rubric.journalspring.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MailService mailService;

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_Success() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "P@ssword1");

        when(userRepository.findByEmail(input.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(input.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(in -> in.getArgument(0));

        User user = authService.signup(input);

        assertNotNull(user);
        assertEquals("test", user.getDisplayUsername());
        assertEquals("test@example.com", user.getEmail());
        assertNotEquals("P@ssword1", user.getPassword());

        verify(userRepository, times(1)).findByEmail(input.email());
        verify(passwordEncoder, times(1)).encode(input.password());
        verify(userRepository, times(1)).save(any(User.class));

        verify(mailService, times(1)).sendEmail(anyString(), anyString(), anyString());

    }

    @Test
    void signup_NullEmail() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                null,
                "P@ssword1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Missing fields", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void signup_NullUsername() {
        RegisterUserDto input = new RegisterUserDto(
                null,
                "test@example.com",
                "P@ssword1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Missing fields", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void signup_NullPassword() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                null);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Missing fields", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void signup_AllNullInput() {
        RegisterUserDto input = new RegisterUserDto(
                null,
                null,
                null);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Missing fields", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void signup_InvalidEmail() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test123",
                "P@ssword1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Invalid email", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_ShortUsername() {
        RegisterUserDto input = new RegisterUserDto(
                "te",
                "test@example.com",
                "P@ssword1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Username does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_LongUsername() {
        RegisterUserDto input = new RegisterUserDto(
                "testestestetsetsestestestetsets",
                "test@example.com",
                "P@ssword1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Username does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_ShortPassword() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "P@swrd1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Password does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_NoUpperCaseCharacterInPassword() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "p@ssword1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Password does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_NoLowerCaseCharacterInPassword() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "P@SSWORD1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Password does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_NoDigitInPassword() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "p@ssword");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Password does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_NoSpecialCharacterInPassword() {
        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "Password1");

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Password does not meet requirements", exception.getMessage());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
    }

    @Test
    void signup_AlreadyExistingEmail() {
        User user = new User();
        user.setEmail("test@example.com");

        RegisterUserDto input = new RegisterUserDto(
                "test",
                "test@example.com",
                "P@ssword1");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authService.signup(input);
        });

        assertEquals("Email already registered", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(userRepository, times(1)).findByEmail(user.getEmail());
    }

    @Test
    void authenticate_Success() {
        String email = "test@example.com";
        String password = "P@ssword1";
        LoginUserDto input = new LoginUserDto(email, password);

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setActivated(true);
        mockUser.setStreak(5);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(entryRepository.findAllByUserOrderByJournalDateDesc(eq(mockUser), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = ArgumentCaptor
                .forClass(UsernamePasswordAuthenticationToken.class);

        User authenticatedUser = authService.authenticate(input);

        assertNotNull(authenticatedUser);
        assertEquals(email, authenticatedUser.getEmail());
        assertEquals(5, authenticatedUser.getStreak());

        verify(authenticationManager).authenticate(authCaptor.capture());
        UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();

        assertEquals(email, capturedAuth.getPrincipal());
        assertEquals(password, capturedAuth.getCredentials());
    }

    @Test
    void authenticate_StreakResetAfterInactivity() {
        String email = "test@example.com";
        String password = "P@ssword1";
        LoginUserDto input = new LoginUserDto(email, password);

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setActivated(true);
        mockUser.setStreak(5);

        Entry lastEntry = new Entry();
        lastEntry.setJournalDate(LocalDate.now().minusDays(3));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(entryRepository.findAllByUserOrderByJournalDateDesc(eq(mockUser), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(lastEntry)));

        User authenticatedUser = authService.authenticate(input);

        assertNotNull(authenticatedUser);
        assertEquals(0, authenticatedUser.getStreak());
        verify(userRepository, times(2)).save(mockUser);
    }

    @Test
    void authenticate_MaintainStreakWithRecentEntry() {
        String email = "test@example.com";
        String password = "P@ssword1";
        LoginUserDto input = new LoginUserDto(email, password);

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setActivated(true);
        mockUser.setStreak(5);

        Entry lastEntry = new Entry();
        lastEntry.setJournalDate(LocalDate.now().minusDays(1));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(entryRepository.findAllByUserOrderByJournalDateDesc(eq(mockUser), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(lastEntry)));

        User authenticatedUser = authService.authenticate(input);

        assertNotNull(authenticatedUser);
        assertEquals(5, authenticatedUser.getStreak());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void verifyUser_Success() {
        String email = "test@example.com";
        String validCode = "123456";
        User user = new User();
        user.setEmail(email);
        user.setVerificationCode(validCode);
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));
        user.setActivated(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, validCode);
        authService.verifyUser(input);

        assertTrue(user.isActivated());
        assertEquals(user.getVerificationCode(), Optional.empty());
        assertNull(user.getCodeExp());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void verifyUser_AlreadyVerified() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setVerificationCode(null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, "anyCode");
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verifyUser(input));
        assertEquals("User is already verified", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void verifyUser_CodeExpired() {
        String email = "test@example.com";
        String code = "123456";
        User user = new User();
        user.setEmail(email);
        user.setVerificationCode(code);
        user.setCodeExp(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, code);
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verifyUser(input));
        assertEquals("Verification code has expired", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void verifyUser_InvalidCode() {
        String email = "test@example.com";
        String correctCode = "123456";
        String wrongCode = "654321";
        User user = new User();
        user.setEmail(email);
        user.setVerificationCode(correctCode);
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, wrongCode);
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verifyUser(input));
        assertEquals("Invalid verification code", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void verify2FA_Success() {
        String email = "test@example.com";
        String validCode = "123456";
        User user = new User();
        user.setEmail(email);
        user.setEnabled2fa(true);
        user.setVerificationCode(validCode);
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, validCode);
        User result = authService.verify2FA(input);

        assertNotNull(result);
        assertEquals(result.getVerificationCode(), Optional.empty());
        assertNull(result.getCodeExp());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void verify2FA_NotEnabled() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setEnabled2fa(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, "anyCode");
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verify2FA(input));
        assertEquals("2FA is not enabled for this account", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void verify2FA_NoCodeRequested() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setEnabled2fa(true);
        user.setVerificationCode(null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, "anyCode");
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verify2FA(input));
        assertEquals("No 2FA code was requested", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void verify2FA_CodeExpired() {
        String email = "test@example.com";
        String validCode = "123456";
        User user = new User();
        user.setEmail(email);
        user.setEnabled2fa(true);
        user.setVerificationCode(validCode);
        user.setCodeExp(LocalDateTime.now().minusMinutes(5)); // Expired

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, validCode);
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verify2FA(input));
        assertEquals("2FA code has expired", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void verify2FA_InvalidCode() {
        String email = "test@example.com";
        String validCode = "123456";
        String wrongCode = "654321";
        User user = new User();
        user.setEmail(email);
        user.setEnabled2fa(true);
        user.setVerificationCode(validCode);
        user.setCodeExp(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        VerifyUserDto input = new VerifyUserDto(email, wrongCode);
        ApplicationException exception = assertThrows(ApplicationException.class, () -> authService.verify2FA(input));
        assertEquals("Invalid 2FA code", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void resendVerificationCode_Success() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setActivated(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(mailService).sendEmail(anyString(), anyString(), anyString());

        authService.resendVerificationCode(email);

        verify(userRepository, times(1)).findByEmail(email);
        verify(mailService, times(1)).sendEmail(anyString(), anyString(), anyString());
        verify(userRepository, times(1)).save(user);
        assertNotNull(user.getVerificationCode());
        assertNotNull(user.getCodeExp());
    }

    @Test
    void resendVerificationCode_AlreadyVerified() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setActivated(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> authService.resendVerificationCode(email));
        assertEquals("Account is already verified", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
