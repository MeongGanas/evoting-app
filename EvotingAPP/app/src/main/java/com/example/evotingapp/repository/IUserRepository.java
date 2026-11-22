package com.example.evotingapp.repository;

import com.example.evotingapp.role.User;
import com.example.evotingapp.role.Voter;

import java.util.List;

public interface IUserRepository {
    /** @return user yang cocok, atau null bila username/password salah. */
    User authenticate(String username, String password);

    boolean hasVoted(String userId);

    boolean markAsVoted(String userId);

    boolean verifyRolePin(String role, String rawPin);

    /** Mendaftarkan pemilih baru ke DPT; password disimpan dalam bentuk hash. */
    boolean registerVoter(Voter voter, String rawPassword);

    boolean existsByUsername(String username);

    boolean existsByNik(String nik);

    /** @return seluruh pemilih terdaftar (DPT) beserta status sudah/belum memilih. */
    List<Voter> findAllVoters();
}
