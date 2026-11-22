package com.example.evotingapp.repository;

import com.example.evotingapp.model.Candidate;

import java.util.List;

public interface ICandidateRepository {
    List<Candidate> findAll();

    /** Menyimpan kandidat baru atau memperbarui kandidat dengan id yang sama. */
    boolean save(Candidate candidate);
}
