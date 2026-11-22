package com.example.evotingapp.service;

/** Kesalahan aturan bisnis pemilu; pesan ditampilkan langsung ke pengguna. */
public class ElectionException extends RuntimeException {
    public ElectionException(String message) {
        super(message);
    }
}
