package com.example.evotingapp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Utilitas umum SHA-256 dan konversi hex (dipakai repository & kriptografi). */
public final class Hashing {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hashing() {
    }

    public static String sha256Hex(String input) {
        return toHex(sha256(input.getBytes(StandardCharsets.UTF_8)));
    }

    public static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tidak tersedia", e);
        }
    }

    public static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Panjang hex tidak valid");
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Karakter hex tidak valid");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    /** Hash password akun (disalt dengan username). */
    public static String hashPassword(String username, String password) {
        return sha256Hex("pwd:" + username + ":" + password);
    }

    /** Hash PIN otorisasi per peran. */
    public static String hashPin(String role, String pin) {
        return sha256Hex("pin:" + role + ":" + pin);
    }
}
