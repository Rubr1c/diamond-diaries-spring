package dev.rubric.journalspring.config;

import dev.rubric.journalspring.models.User;
import dev.rubric.journalspring.repository.UserRepository;
import dev.rubric.journalspring.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        logger.info("OAuth2 authentication success handler triggered");

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            logger.error("Authentication is not OAuth2AuthenticationToken");
            return;
        }

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        // Log all attributes to debug
        logger.debug("OAuth2User attributes:");
        oauth2User.getAttributes().forEach((key, value) -> logger.debug("{}: {}", key, value));

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        String googleId = oauth2User.getAttribute("sub");

        if (email == null || name == null || googleId == null) {
            logger.error("Required OAuth2 user attributes are missing - email: {}, name: {}, googleId: {}",
                    email != null, name != null, googleId != null);
            return;
        }

        logger.info("Processing OAuth2 user - Email: {}, Name: {}, GoogleId: {}", email, name, googleId);

        try {
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;

            if (existingUser.isEmpty()) {
                logger.info("Creating new user for email: {}", email);
                user = new User(
                        googleId,
                        name,
                        email,
                        null, // Empty password for OAuth users
                        picture != null ? picture : "");
                user.setActivated(true); // Google-authenticated users are automatically verified
                user = userRepository.save(user);
                logger.info("New user created with ID: {}", user.getId());
            } else {
                user = existingUser.get();
                logger.info("Found existing user with ID: {}", user.getId());
                // Update existing user's Google details if needed
                boolean needsUpdate = false;

                if (user.getGoogleId().isEmpty()) {
                    logger.info("Updating existing user's Google ID");
                    user.setGoogleId(googleId);
                    needsUpdate = true;
                }

                if (picture != null && !picture.equals(user.getProfilePicture())
                        && Objects.equals(user.getProfilePicture(), "")) {
                    logger.info("Updating existing user's profile picture");
                    user.setProfilePicture(picture);
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    user = userRepository.save(user);
                    logger.info("User updated successfully");
                }
            }

            String token = jwtService.generateToken(user);
            String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + token;
            logger.info("Redirecting to frontend: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            logger.error("Error processing OAuth2 user", e);
            throw e; // Re-throw to ensure Spring Security handles the error
        }
    }
}