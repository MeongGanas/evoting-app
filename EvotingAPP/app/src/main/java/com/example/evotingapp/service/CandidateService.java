package com.example.evotingapp.service;

import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.repository.ICandidateRepository;

import java.util.List;

/** Tanggung jawab tunggal: data kandidat dan laporan dana kampanye. */
public class CandidateService {
    private final ICandidateRepository candidateRepo;

    public CandidateService(ICandidateRepository candidateRepo) {
        this.candidateRepo = candidateRepo;
    }

    public List<Candidate> getAllCandidates() {
        return candidateRepo.findAll();
    }

    public boolean saveCandidate(Candidate candidate) {
        if (candidate.getCandidateId() == null || candidate.getCandidateId().trim().isEmpty()) {
            throw new ElectionException("Nomor/ID kandidat wajib diisi.");
        }
        if (candidate.getFullName() == null || candidate.getFullName().trim().isEmpty()) {
            throw new ElectionException("Nama kandidat wajib diisi.");
        }
        if (candidate.getCampaignFundAmount() < 0) {
            throw new ElectionException("Dana kampanye tidak boleh negatif.");
        }
        return candidateRepo.save(candidate);
    }
}
