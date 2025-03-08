package com.project.securecommdemo.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import android.util.Base64;

public class CryptoUtils {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_SIZE = 12; // 12 bytes for GCM recommended
    private static final int TAG_LENGTH_BIT = 128; // 16 bytes auth tag

    public static class EncryptedPayload {
        public String nonceBase64;
        public String cipherTextBase64;
        public String authTagBase64;
    }

    // Encrypts the plain text using the given AES key
    public static EncryptedPayload encrypt(byte[] key, byte[] plainText) throws Exception {
        // Generate a random nonce (IV)
        byte[] nonce = new byte[NONCE_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

        byte[] cipherTextWithTag = cipher.doFinal(plainText);

        // In AES/GCM the auth tag is appended to the ciphertext.
        int tagLength = TAG_LENGTH_BIT / 8; // 16 bytes
        int cipherTextLength = cipherTextWithTag.length - tagLength;
        byte[] cipherText = new byte[cipherTextLength];
        byte[] authTag = new byte[tagLength];

        System.arraycopy(cipherTextWithTag, 0, cipherText, 0, cipherTextLength);
        System.arraycopy(cipherTextWithTag, cipherTextLength, authTag, 0, tagLength);

        EncryptedPayload payload = new EncryptedPayload();
        payload.nonceBase64 = Base64.encodeToString(nonce, Base64.NO_WRAP);
        payload.cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP);
        payload.authTagBase64 = Base64.encodeToString(authTag, Base64.NO_WRAP);

        return payload;
    }

    // Decrypts the payload using the given AES key
    public static byte[] decrypt(byte[] key, String nonceBase64, String cipherTextBase64, String authTagBase64) throws Exception {
        byte[] nonce = Base64.decode(nonceBase64, Base64.NO_WRAP);
        byte[] cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP);
        byte[] authTag = Base64.decode(authTagBase64, Base64.NO_WRAP);

        // Combine cipherText and authTag as required by GCM
        byte[] cipherTextWithTag = new byte[cipherText.length + authTag.length];
        System.arraycopy(cipherText, 0, cipherTextWithTag, 0, cipherText.length);
        System.arraycopy(authTag, 0, cipherTextWithTag, cipherText.length, authTag.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

        return cipher.doFinal(cipherTextWithTag);
    }

    // For demonstration: generate a random AES-256 key.
    public static byte[] generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES-256
        SecretKey key = keyGen.generateKey();
        return key.getEncoded();
    }
}
