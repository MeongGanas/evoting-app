package com.example.evotingapp.service;

public interface IBallotCipher {
    String encrypt(String plainText);

    String decrypt(String cipherText);
}
