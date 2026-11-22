package com.example.evotingapp.service;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.EncryptedBallot;
import com.example.evotingapp.repository.IBallotRepository;
import com.example.evotingapp.repository.IElectionRepository;
import com.example.evotingapp.repository.IUserRepository;
import com.example.evotingapp.role.Voter;

/** Tanggung jawab tunggal: menerima, mengenkripsi, dan merantai satu suara. */
public class VotingService {
    private final IUserRepository userRepo;
    private final IBallotRepository ballotRepo;
    private final IElectionRepository electionRepo;
    private final IBallotCipher ballotCipher;
    private final IHashChainService hashService;

    public VotingService(IUserRepository userRepo, IBallotRepository ballotRepo,
                         IElectionRepository electionRepo, IBallotCipher ballotCipher,
                         IHashChainService hashService) {
        this.userRepo = userRepo;
        this.ballotRepo = ballotRepo;
        this.electionRepo = electionRepo;
        this.ballotCipher = ballotCipher;
        this.hashService = hashService;
    }

    /**
     * @return Receipt Hash (SHA-256) yang menjadi bukti suara tercatat.
     * @throws ElectionException bila pemilu tidak OPEN atau pemilih sudah memilih.
     */
    public String castVote(Voter voter, String candidateId) {
        if (electionRepo.getState() != ElectionState.OPEN) {
            throw new ElectionException("Pemungutan suara sedang tidak dibuka.");
        }
        if (candidateId == null || candidateId.isEmpty()) {
            throw new ElectionException("Kandidat belum dipilih.");
        }
        if (!voter.canCastVote() || userRepo.hasVoted(voter.getUserId())) {
            throw new ElectionException("Anda sudah menggunakan hak pilih.");
        }
        // Tandai sudah memilih lebih dulu agar tidak mungkin ada suara ganda.
        if (!userRepo.markAsVoted(voter.getUserId())) {
            throw new ElectionException("Anda sudah menggunakan hak pilih.");
        }
        voter.setHasVoted(true);

        String payload = ballotCipher.encrypt(candidateId);
        String previousHash = ballotRepo.getLatestHash();
        String receipt = hashService.computeReceipt(previousHash, payload);
        if (!ballotRepo.insertBallot(new EncryptedBallot(0, payload, previousHash, receipt))) {
            throw new ElectionException("Gagal menyimpan surat suara.");
        }
        return receipt;
    }
}
