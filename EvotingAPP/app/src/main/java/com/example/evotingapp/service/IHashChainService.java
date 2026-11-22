package com.example.evotingapp.service;

import com.example.evotingapp.model.EncryptedBallot;

import java.util.List;

public interface IHashChainService {
    String computeSha256(String input);

    /** Receipt Hash = SHA-256(previousHash + payload terenkripsi). */
    String computeReceipt(String previousHash, String encryptedPayload);

    /** @return true bila seluruh rantai utuh (tidak ada data yang diubah, dihapus, atau disisipkan). */
    boolean verifyChain(List<EncryptedBallot> ballots);
}
