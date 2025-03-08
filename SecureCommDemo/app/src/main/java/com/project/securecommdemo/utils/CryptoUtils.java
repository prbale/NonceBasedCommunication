package com.project.securecommdemo.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import android.util.Base64;

/**
 * Utility class for performing AES encryption and decryption using GCM mode.
 * <p>
 * This class provides methods to encrypt and decrypt data using the AES/GCM/NoPadding transformation.
 * It generates a random nonce (IV) for each encryption and returns an {@link EncryptedPayload} object
 * that contains the Base64-encoded nonce, ciphertext, and authentication tag.
 * <p>
 * <b>Usage:</b>
 * <pre>
 *   // Generate an AES-256 key (for demonstration purposes)
 *   byte[] key = CryptoUtils.generateAESKey();
 *
 *   // Encrypt data
 *   byte[] plainText = "Hello, World!".getBytes("UTF-8");
 *   EncryptedPayload payload = CryptoUtils.encrypt(key, plainText);
 *
 *   // Decrypt data
 *   byte[] decryptedData = CryptoUtils.decrypt(key, payload.nonceBase64, payload.cipherTextBase64, payload.authTagBase64);
 * </pre>
 */
public class CryptoUtils {

    // Transformation used for encryption and decryption.
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    // Recommended nonce (IV) size for GCM mode.
    private static final int NONCE_SIZE = 12; // 12 bytes for GCM recommended

    // Authentication tag length in bits.
    private static final int TAG_LENGTH_BIT = 128; // 16 bytes auth tag

    /**
     * Container class for the encrypted payload.
     * <p>
     * It contains the Base64-encoded nonce (IV), ciphertext, and authentication tag.
     */
    public static class EncryptedPayload {
        public String nonceBase64;
        public String cipherTextBase64;
        public String authTagBase64;
    }

    /**
     * Encrypts the provided plaintext using AES/GCM/NoPadding.
     *
     * <p>
     * The method generates a random nonce (IV) for each encryption, and the resulting authentication tag
     * is appended to the ciphertext. The ciphertext and tag are then split and encoded in Base64.
     *
     * @param key       the AES key as a byte array (e.g., 256-bit key for AES-256)
     * @param plainText the plaintext data to encrypt, as a byte array
     * @return an {@link EncryptedPayload} containing the Base64-encoded nonce, ciphertext, and auth tag
     * @throws Exception if an error occurs during encryption
     */
    public static EncryptedPayload encrypt(byte[] key, byte[] plainText) throws Exception {

        // Generate a random nonce (IV)
        byte[] nonce = new byte[NONCE_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        // Initialize cipher for encryption using AES/GCM/NoPadding
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

        // Encrypt the plaintext. The resulting array contains ciphertext and authentication tag.
        byte[] cipherTextWithTag = cipher.doFinal(plainText);

        // Split the ciphertext and the auth tag. In AES/GCM, the auth tag is appended to the ciphertext.
        int tagLength = TAG_LENGTH_BIT / 8; // 16 bytes
        int cipherTextLength = cipherTextWithTag.length - tagLength;
        byte[] cipherText = new byte[cipherTextLength];
        byte[] authTag = new byte[tagLength];

        System.arraycopy(cipherTextWithTag, 0, cipherText, 0, cipherTextLength);
        System.arraycopy(cipherTextWithTag, cipherTextLength, authTag, 0, tagLength);

        // Prepare the payload with Base64-encoded data.
        EncryptedPayload payload = new EncryptedPayload();
        payload.nonceBase64 = Base64.encodeToString(nonce, Base64.NO_WRAP);
        payload.cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP);
        payload.authTagBase64 = Base64.encodeToString(authTag, Base64.NO_WRAP);

        return payload;
    }

    /**
     * Decrypts the encrypted payload using the provided AES key.
     *
     * <p>
     * The method decodes the Base64-encoded nonce, ciphertext, and authentication tag,
     * then combines the ciphertext and tag before decrypting using AES/GCM/NoPadding.
     *
     * @param key            the AES key as a byte array
     * @param nonceBase64    the Base64-encoded nonce (IV) used during encryption
     * @param cipherTextBase64 the Base64-encoded ciphertext (excluding the auth tag)
     * @param authTagBase64  the Base64-encoded authentication tag
     * @return the decrypted plaintext as a byte array
     * @throws Exception if decryption fails (e.g., due to an invalid key, altered data, etc.)
     */
    public static byte[] decrypt(byte[] key, String nonceBase64, String cipherTextBase64, String authTagBase64) throws Exception {
        byte[] nonce = Base64.decode(nonceBase64, Base64.NO_WRAP);
        byte[] cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP);
        byte[] authTag = Base64.decode(authTagBase64, Base64.NO_WRAP);

        // Combine cipherText and authTag as required by GCM
        byte[] cipherTextWithTag = new byte[cipherText.length + authTag.length];
        System.arraycopy(cipherText, 0, cipherTextWithTag, 0, cipherText.length);
        System.arraycopy(authTag, 0, cipherTextWithTag, cipherText.length, authTag.length);

        // Initialize cipher for decryption using the same transformation and nonce.
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

        // Decrypt and return the original plaintext.
        return cipher.doFinal(cipherTextWithTag);
    }

    /**
     * Generates a random AES key for encryption/decryption.
     *
     * <p>
     * This method generates a 256-bit AES key (AES-256) for demonstration purposes.
     * In a production environment, secure key exchange should be implemented.
     *
     * @return the generated AES key as a byte array
     * @throws Exception if key generation fails
     */
    public static byte[] generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES-256
        SecretKey key = keyGen.generateKey();
        return key.getEncoded();
    }
}
