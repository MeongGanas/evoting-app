package com.example.evotingapp.service;

import com.example.evotingapp.model.EncryptedBallot;
import com.example.evotingapp.util.Hashing;

import java.util.List;

public class Sha256HashChainService implements IHashChainService {

    @Override
    public String computeSha256(String input) {
        return Hashing.sha256Hex(input);
    }

    @Override
    public String computeReceipt(String previousHash, String encryptedPayload) {
        return computeSha256(previousHash + "|" + encryptedPayload);
    }

    @Override
    public boolean verifyChain(List<EncryptedBallot> ballots) {
        String expectedPrevious = EncryptedBallot.GENESIS_HASH;
        for (EncryptedBallot ballot : ballots) {
            // 1) tautan ke surat suara sebelumnya harus cocok
            if (!expectedPrevious.equals(ballot.getPreviousHash())) {
                return false;
            }
            // 2) hash harus sesuai dengan isi surat suara saat ini
            String recomputed = computeReceipt(ballot.getPreviousHash(), ballot.getEncryptedPayload());
            if (!recomputed.equals(ballot.getReceiptHash())) {
                return false;
            }
            expectedPrevious = ballot.getReceiptHash();
        }
        return true;
    }
}
