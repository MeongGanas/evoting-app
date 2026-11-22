package com.example.evotingapp.service;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.repository.IElectionRepository;
import com.example.evotingapp.repository.IUserRepository;
import com.example.evotingapp.role.Voter;

import java.util.List;
import java.util.UUID;

/**
 * Tanggung jawab tunggal: pengelolaan Daftar Pemilih Tetap (DPT) oleh KPU.
 * Pemilih tidak dapat mendaftar sendiri agar satu orang hanya memiliki satu hak pilih.
 */
public class VoterRegistryService {
    private final IUserRepository userRepo;
    private final IElectionRepository electionRepo;

    public VoterRegistryService(IUserRepository userRepo, IElectionRepository electionRepo) {
        this.userRepo = userRepo;
        this.electionRepo = electionRepo;
    }

    public Voter registerVoter(String nik, String username, String password) {
        ElectionState state = electionRepo.getState();
        if (state != ElectionState.NOT_STARTED && state != ElectionState.OPEN) {
            throw new ElectionException("DPT dikunci setelah pemungutan suara ditutup.");
        }

        String cleanNik = nik == null ? "" : nik.trim();
        String cleanUsername = username == null ? "" : username.trim().toLowerCase();

        if (!cleanNik.matches("\\d{16}")) {
            throw new ElectionException("NIK harus 16 digit angka.");
        }
        if (!cleanUsername.matches("[a-z0-9_]{4,20}")) {
            throw new ElectionException("Username 4–20 karakter: huruf kecil, angka, atau garis bawah.");
        }
        if (password == null || password.length() < 6) {
            throw new ElectionException("Password awal minimal 6 karakter.");
        }
        if (userRepo.existsByNik(cleanNik)) {
            throw new ElectionException("NIK sudah terdaftar di DPT.");
        }
        if (userRepo.existsByUsername(cleanUsername)) {
            throw new ElectionException("Username sudah dipakai.");
        }

        Voter voter = new Voter("U-" + UUID.randomUUID().toString().substring(0, 8),
                cleanUsername, cleanNik, false);
        if (!userRepo.registerVoter(voter, password)) {
            throw new ElectionException("Gagal menyimpan pemilih.");
        }
        return voter;
    }

    public List<Voter> getAllVoters() {
        return userRepo.findAllVoters();
    }
}
