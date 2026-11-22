package com.example.evotingapp.service;

import com.example.evotingapp.repository.IBallotRepository;

/** Tanggung jawab tunggal: memeriksa keberadaan dan keutuhan surat suara. */
public class BallotAuditService {
    private final IBallotRepository ballotRepo;
    private final IHashChainService hashService;

    public BallotAuditService(IBallotRepository ballotRepo, IHashChainService hashService) {
        this.ballotRepo = ballotRepo;
        this.hashService = hashService;
    }

    public boolean verifyReceipt(String receiptHash) {
        if (receiptHash == null) {
            return false;
        }
        String normalized = receiptHash.trim().toLowerCase();
        return !normalized.isEmpty() && ballotRepo.existsByReceiptHash(normalized);
    }

    /** @return true bila rantai hash utuh (aman), false bila terdeteksi manipulasi. */
    public boolean runTamperCheck() {
        return hashService.verifyChain(ballotRepo.getAllBallotsOrdered());
    }

    public int countBallots() {
        return ballotRepo.getAllBallotsOrdered().size();
    }
}
