package com.example.evotingapp;

import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.EncryptedBallot;
import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.TallyResult;
import com.example.evotingapp.model.ViolationReport;
import com.example.evotingapp.repository.IBallotRepository;
import com.example.evotingapp.repository.ICandidateRepository;
import com.example.evotingapp.repository.IElectionRepository;
import com.example.evotingapp.repository.IReportRepository;
import com.example.evotingapp.repository.IUserRepository;
import com.example.evotingapp.role.User;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.util.Hashing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository palsu di memori. Karena service hanya bergantung pada interface (DIP),
 * logika bisnis bisa diuji tanpa SQLite/Android.
 */
final class InMemoryRepositories {

    private InMemoryRepositories() {
    }

    static class Users implements IUserRepository {
        final Set<String> voted = new HashSet<>();
        final Map<String, String> pinHashes = new HashMap<>();

        Users() {
            pinHashes.put(User.ROLE_KPU, Hashing.hashPin(User.ROLE_KPU, "1111"));
            pinHashes.put(User.ROLE_PANWAS, Hashing.hashPin(User.ROLE_PANWAS, "2222"));
        }

        @Override
        public User authenticate(String username, String password) {
            return null;
        }

        @Override
        public boolean hasVoted(String userId) {
            return voted.contains(userId);
        }

        @Override
        public boolean markAsVoted(String userId) {
            return voted.add(userId);
        }

        @Override
        public boolean verifyRolePin(String role, String rawPin) {
            return rawPin != null && Hashing.hashPin(role, rawPin).equals(pinHashes.get(role));
        }

        final List<Voter> voters = new ArrayList<>();

        @Override
        public boolean registerVoter(Voter voter, String rawPassword) {
            return voters.add(voter);
        }

        @Override
        public boolean existsByUsername(String username) {
            for (Voter v : voters) {
                if (v.getUsername().equals(username)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean existsByNik(String nik) {
            for (Voter v : voters) {
                if (v.getNik().equals(nik)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Voter> findAllVoters() {
            return new ArrayList<>(voters);
        }
    }

    static class Ballots implements IBallotRepository {
        final List<EncryptedBallot> ballots = new ArrayList<>();

        @Override
        public boolean insertBallot(EncryptedBallot b) {
            ballots.add(new EncryptedBallot(ballots.size() + 1, b.getEncryptedPayload(),
                    b.getPreviousHash(), b.getReceiptHash()));
            return true;
        }

        @Override
        public String getLatestHash() {
            return ballots.isEmpty() ? EncryptedBallot.GENESIS_HASH
                    : ballots.get(ballots.size() - 1).getReceiptHash();
        }

        @Override
        public List<EncryptedBallot> getAllBallotsOrdered() {
            return new ArrayList<>(ballots);
        }

        @Override
        public boolean existsByReceiptHash(String receiptHash) {
            for (EncryptedBallot b : ballots) {
                if (b.getReceiptHash().equals(receiptHash)) {
                    return true;
                }
            }
            return false;
        }

        /** Meniru penyerang yang mengubah isi surat suara langsung di database. */
        void tamperPayload(int index, String newPayload) {
            EncryptedBallot old = ballots.get(index);
            ballots.set(index, new EncryptedBallot(old.getSequenceNo(), newPayload,
                    old.getPreviousHash(), old.getReceiptHash()));
        }
    }

    static class Candidates implements ICandidateRepository {
        final Map<String, Candidate> map = new LinkedHashMap<>();

        Candidates() {
            save(new Candidate("C01", "01 - A", "", 0, ""));
            save(new Candidate("C02", "02 - B", "", 0, ""));
            save(new Candidate("C03", "03 - C", "", 0, ""));
        }

        @Override
        public List<Candidate> findAll() {
            return new ArrayList<>(map.values());
        }

        @Override
        public boolean save(Candidate candidate) {
            map.put(candidate.getCandidateId(), candidate);
            return true;
        }
    }

    static class Election implements IElectionRepository {
        ElectionState state = ElectionState.NOT_STARTED;
        List<TallyResult> results = new ArrayList<>();

        @Override
        public ElectionState getState() {
            return state;
        }

        @Override
        public boolean updateState(ElectionState state) {
            this.state = state;
            return true;
        }

        @Override
        public boolean saveResults(List<TallyResult> results) {
            this.results = new ArrayList<>(results);
            return true;
        }

        @Override
        public List<TallyResult> getResults() {
            return new ArrayList<>(results);
        }
    }

    static class Reports implements IReportRepository {
        final Map<String, ViolationReport> byId = new LinkedHashMap<>();

        @Override
        public boolean insert(ViolationReport report) {
            byId.put(report.getReportId(), report);
            return true;
        }

        @Override
        public boolean updateStatus(String reportId, ReportStatus status) {
            ViolationReport r = byId.get(reportId);
            if (r == null) {
                return false;
            }
            byId.put(reportId, new ViolationReport(r.getReportId(), r.getTicketCode(), r.getReporterId(),
                    r.getReporterDisplay(), r.getDescription(), status, r.getCreatedAt()));
            return true;
        }

        @Override
        public ViolationReport findByTicketCode(String ticketCode) {
            for (ViolationReport r : byId.values()) {
                if (r.getTicketCode().equals(ticketCode)) {
                    return r;
                }
            }
            return null;
        }

        @Override
        public List<ViolationReport> findAll() {
            return new ArrayList<>(byId.values());
        }
    }
}
