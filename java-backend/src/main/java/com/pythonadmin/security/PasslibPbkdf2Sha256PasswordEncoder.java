package com.pythonadmin.security;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasslibPbkdf2Sha256PasswordEncoder implements PasswordEncoder {
    private static final String PREFIX = "$pbkdf2-sha256$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int iterations;
    private final int saltBytes;

    public PasslibPbkdf2Sha256PasswordEncoder(int iterations, int saltBytes) {
        if (iterations <= 0) throw new IllegalArgumentException("iterations must be positive");
        if (saltBytes <= 0) throw new IllegalArgumentException("saltBytes must be positive");
        this.iterations = iterations;
        this.saltBytes = saltBytes;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] salt = new byte[saltBytes];
        RANDOM.nextBytes(salt);
        byte[] dk = pbkdf2(rawPassword, salt, iterations, 32);
        return PREFIX
            + iterations
            + "$"
            + ab64Encode(salt)
            + "$"
            + ab64Encode(dk);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith(PREFIX)) return false;

        String[] parts = encodedPassword.split("\\$");
        if (parts.length < 5) return false;

        int it;
        try {
            it = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }

        String saltPart = parts[3];
        String hashPart = parts[4];

        byte[] salt;
        byte[] expected;
        try {
            salt = ab64Decode(saltPart);
            expected = ab64Decode(hashPart);
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] actual = pbkdf2(rawPassword, salt, it, expected.length);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(CharSequence rawPassword, byte[] salt, int iterations, int keyLenBytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                rawPassword.toString().toCharArray(),
                salt,
                iterations,
                keyLenBytes * 8
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("pbkdf2 failed", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= (a[i] ^ b[i]);
        }
        return diff == 0;
    }

    private static byte[] ab64Decode(String s) {
        if (s == null) throw new IllegalArgumentException("null");
        String normalized = s.replace('.', '+');
        int mod = normalized.length() % 4;
        if (mod == 2) normalized = normalized + "==";
        else if (mod == 3) normalized = normalized + "=";
        else if (mod == 1) throw new IllegalArgumentException("invalid base64 length");
        return Base64.getDecoder().decode(normalized);
    }

    private static String ab64Encode(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("null");
        return Base64.getEncoder().withoutPadding().encodeToString(bytes).replace('+', '.');
    }
}
