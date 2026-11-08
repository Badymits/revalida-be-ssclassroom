package org.ssglobal.revalida.ssclassroom.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NameEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public NameEncryptionService(@Value("${APP_ENCRYPTION_KEY}") String base64Key) {
        this.key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
    }

    public String encrypt(String name) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(name.getBytes(StandardCharsets.UTF_8));

            byte[] stored = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();

            return Base64.getEncoder().encodeToString(stored);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt name", e);
        }
    }

    public String decrypt(String storedValue) {
        try {
            byte[] stored = Base64.getDecoder().decode(storedValue);
            ByteBuffer buffer = ByteBuffer.wrap(stored);

            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not decrypt name", e);
        }
    }
}