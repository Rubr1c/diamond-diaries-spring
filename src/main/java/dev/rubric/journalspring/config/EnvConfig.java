package dev.rubric.journalspring.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    @PostConstruct
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Database Configuration
            setEnvIfAbsent("POSTGRES_DB", dotenv);
            setEnvIfAbsent("POSTGRES_USER", dotenv);
            setEnvIfAbsent("POSTGRES_PASSWORD", dotenv);

            // Security Configuration
            setEnvIfAbsent("JWT_SECRET", dotenv);
            setEnvIfAbsent("ADMIN_KEY", dotenv);

            // Mail Configuration
            setEnvIfAbsent("MAIL_USERNAME", dotenv);
            setEnvIfAbsent("MAIL_PASSWORD", dotenv);

            // OAuth2 Configuration
            setEnvIfAbsent("GOOGLE_CLIENT_ID", dotenv);
            setEnvIfAbsent("GOOGLE_CLIENT_SECRET", dotenv);
            setEnvIfAbsent("OAUTH_REDIRECT_URI", dotenv);
            setEnvIfAbsent("FRONTEND_URL", dotenv);

            logger.info("Environment variables loaded successfully");
        } catch (Exception e) {
            logger.warn("Error loading .env file: {}", e.getMessage());
        }
    }

    private void setEnvIfAbsent(String key, Dotenv dotenv) {
        if (System.getenv(key) == null && dotenv.get(key) != null) {
            System.setProperty(key, dotenv.get(key));
            logger.debug("Set environment variable: {}", key);
        }
    }
}