package com.example.evotingapp.service;

import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.EncryptedBallot;
import com.example.evotingapp.model.TallyResult;
import com.example.evotingapp.repository.IBallotRepository;
import com.example.evotingapp.repository.ICandidateRepository;
import com.example.evotingapp.repository.IElectionRepository;
import com.example.evotingapp.repository.IUserRepository;
import com.example.evotingapp.role.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tanggung jawab tunggal: membuka kotak suara (2 PIN) dan menghitung hasil. */
public class TallyService {
    private final IUserRepository userRepo;
    private final IBallotRepository ballotRepo;
    private final ICandidateRepository candidateRepo;
    private final IElectionRepository electionRepo;
    private final IBallotCipher ballotCipher;
    private final BallotAuditService auditService;

    public TallyService(IUserRepository userRepo, IBallotRepository ballotRepo,
                        ICandidateRepository candidateRepo, IElectionRepository electionRepo,
                        IBallotCipher ballotCipher, BallotAuditService auditService) {
        this.userRepo = userRepo;
        this.ballotRepo = ballotRepo;
        this.candidateRepo = candidateRepo;
        this.electionRepo = electionRepo;
        this.ballotCipher = ballotCipher;
        this.auditService = auditService;
    }

    public List<TallyResult> unlockAndTally(String kpuPin, String panwasPin) {
        ElectionState state = electionRepo.getState();
        if (state == ElectionState.PUBLISHED) {
            throw new ElectionException("Hasil sudah dipublikasikan.");
        }
        if (state == ElectionState.TAMPERED) {
            throw new ElectionException("Kotak suara dibekukan karena terindikasi manipulasi.");
        }
        if (state != ElectionState.CLOSED) {
            throw new ElectionException("Tutup pemungutan suara terlebih dahulu.");
        }
        if (!userRepo.verifyRolePin(User.ROLE_KPU, kpuPin)
                || !userRepo.verifyRolePin(User.ROLE_PANWAS, panwasPin)) {
            throw new ElectionException("PIN KPU atau PIN Panwas tidak valid.");
        }
        if (!auditService.runTamperCheck()) {
            electionRepo.updateState(ElectionState.TAMPERED);
            throw new ElectionException("TAMPER ALERT: rantai hash rusak. Kotak suara dibekukan.");
        }

        List<Candidate> candidates = candidateRepo.findAll();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            counts.put(c.getCandidateId(), 0);
        }
        List<EncryptedBallot> ballots = ballotRepo.getAllBallotsOrdered();
        for (EncryptedBallot b : ballots) {
            String candidateId = ballotCipher.decrypt(b.getEncryptedPayload());
            Integer current = counts.get(candidateId);
            if (current != null) {
                counts.put(candidateId, current + 1);
            }
        }

        int total = ballots.size();
        List<TallyResult> results = new ArrayList<>();
        for (Candidate c : candidates) {
            int votes = counts.get(c.getCandidateId());
            double pct = total == 0 ? 0 : votes * 100.0 / total;
            results.add(new TallyResult(c.getCandidateId(), c.getFullName(), votes, pct));
        }
        Collections.sort(results, (a, b) -> Integer.compare(b.getTotalVotes(), a.getTotalVotes()));

        if (!electionRepo.saveResults(results)) {
            throw new ElectionException("Gagal menyimpan hasil rekapitulasi.");
        }
        electionRepo.updateState(ElectionState.PUBLISHED);
        return results;
    }

    /** @return hasil resmi bila sudah PUBLISHED, selain itu daftar kosong (hasil masih terkunci). */
    public List<TallyResult> getPublishedResults() {
        if (electionRepo.getState() != ElectionState.PUBLISHED) {
            return Collections.emptyList();
        }
        return electionRepo.getResults();
    }
}
