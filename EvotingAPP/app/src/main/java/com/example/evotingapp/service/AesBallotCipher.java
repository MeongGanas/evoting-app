package com.example.evotingapp.service;

import com.example.evotingapp.util.Hashing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Enkripsi surat suara dengan AES-256 (CBC + PKCS5Padding) dan IV acak per surat suara,
 * sehingga dua suara untuk kandidat yang sama menghasilkan ciphertext berbeda.
 * Format keluaran: hex(IV || ciphertext).
 */
public class AesBallotCipher implements IBallotCipher {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    /** @param passphrase kunci rahasia; diturunkan menjadi kunci 256-bit dengan SHA-256. */
    public AesBallotCipher(String passphrase) {
        byte[] key = Hashing.sha256(passphrase.getBytes(StandardCharsets.UTF_8));
        this.secretKey = new SecretKeySpec(key, "AES");
    }

    @Override
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, out, IV_LENGTH, encrypted.length);
            return Hashing.toHex(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Gagal mengenkripsi surat suara", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        try {
            byte[] all = Hashing.fromHex(cipherText);
            if (all.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Ciphertext terlalu pendek");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] body = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, body, 0, body.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(body), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Gagal mendekripsi surat suara", e);
        }
    }
}
