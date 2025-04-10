package dev.rubric.journalspring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceUnitTests {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() throws Exception {
        encryptionService = new EncryptionService();
        setPrivateField(encryptionService, "secret", "mySuperSecretKey1234567890");
        setPrivateField(encryptionService, "salt", "mySaltValue");
    }

    private void setPrivateField(Object target, String fieldName, String value) throws Exception {
        Field field = EncryptionService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testEncryptDecrypt_NonEmpty() {
        String original = "This is a secret message.";
        String encrypted = encryptionService.encrypt(original);
        assertNotNull(encrypted, "Encrypted string should not be null");
        assertNotEquals(original, encrypted, "Encrypted string should differ from the original");

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted, "Decrypted text should match the original");
    }

    @Test
    void testEncrypt_NullInput() {
        assertNull(encryptionService.encrypt(null), "Encrypting null should return null");
    }

    @Test
    void testEncrypt_EmptyInput() {
        assertEquals("", encryptionService.encrypt(""), "Encrypting an empty string should return an empty string");
    }

    @Test
    void testDecrypt_NullInput() {
        assertNull(encryptionService.decrypt(null), "Decrypting null should return null");
    }

    @Test
    void testDecrypt_EmptyInput() {
        assertEquals("", encryptionService.decrypt(""), "Decrypting an empty string should return an empty string");
    }

    @Test
    void testEncryptionProducesDifferentCiphertext() {
        String original = "Repeatable message";
        String encrypted1 = encryptionService.encrypt(original);
        String encrypted2 = encryptionService.encrypt(original);
        assertNotEquals(encrypted1, encrypted2, "Ciphertexts should be different due to random IV");

        assertEquals(original, encryptionService.decrypt(encrypted1));
        assertEquals(original, encryptionService.decrypt(encrypted2));
    }
}

