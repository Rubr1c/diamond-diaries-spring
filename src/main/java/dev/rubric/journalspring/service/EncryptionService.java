package dev.rubric.journalspring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class EncryptionService {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    @Value("${encryption.secret}")
    private String secret;

    @Value("${encryption.salt}")
    private String salt;

    @Value("${encryption.token.secret:defaultTokenSecret}")
    private String tokenSecret;

    /**
     * Encrypts the given text using AES encryption
     * 
     * @param text The text to encrypt
     * @return Base64 encoded encrypted string with IV prepended
     */
    public String encrypt(String text) {
        try {
            if (text == null || text.isEmpty()) {
                return text;
            }

            // Generate a random IV
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Generate the secret key
            SecretKey secretKey = generateSecretKey();

            // Initialize the cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            // Encrypt the text
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted part
            byte[] encryptedIVAndText = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedIVAndText, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedIVAndText, iv.length, encrypted.length);

            // Encode with Base64
            return Base64.getEncoder().encodeToString(encryptedIVAndText);
        } catch (Exception e) {
            logger.error("Error encrypting text", e);
            throw new RuntimeException("Error encrypting text", e);
        }
    }

    /**
     * Decrypts the given encrypted text
     * 
     * @param encryptedText Base64 encoded encrypted text with IV prepended
     * @return The decrypted text
     */
    public String decrypt(String encryptedText) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return encryptedText;
            }

            // Decode with Base64
            byte[] encryptedIVAndText = Base64.getDecoder().decode(encryptedText);

            // Extract IV
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedIVAndText, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Extract encrypted part
            byte[] encrypted = new byte[encryptedIVAndText.length - iv.length];
            System.arraycopy(encryptedIVAndText, iv.length, encrypted, 0, encrypted.length);

            // Generate the secret key
            SecretKey secretKey = generateSecretKey();

            // Initialize the cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            // Decrypt the text
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error decrypting text", e);
            throw new RuntimeException("Error decrypting text", e);
        }
    }

    private SecretKey generateSecretKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
                secret.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                ITERATION_COUNT,
                KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * Gets the token encryption key for use by the TokenGeneratorService
     * This uses a separate key from the main content encryption for security
     * separation
     * 
     * @return The token encryption key
     */
    public String getTokenEncryptionKey() {
        return tokenSecret;
    }
}