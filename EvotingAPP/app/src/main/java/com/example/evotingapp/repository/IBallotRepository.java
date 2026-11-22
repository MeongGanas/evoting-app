package com.example.evotingapp.repository;

import com.example.evotingapp.model.EncryptedBallot;

import java.util.List;

public interface IBallotRepository {
    boolean insertBallot(EncryptedBallot ballot);

    /** @return receiptHash surat suara terakhir, atau {@link EncryptedBallot#GENESIS_HASH} bila kosong. */
    String getLatestHash();

    List<EncryptedBallot> getAllBallotsOrdered();

    boolean existsByReceiptHash(String receiptHash);
}
