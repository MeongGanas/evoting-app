package com.example.evotingapp.service;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.repository.IElectionRepository;

/** Tanggung jawab tunggal: mengatur status pemungutan suara. */
public class ElectionService {
    private final IElectionRepository electionRepo;

    public ElectionService(IElectionRepository electionRepo) {
        this.electionRepo = electionRepo;
    }

    public ElectionState getState() {
        return electionRepo.getState();
    }

    public boolean openElection() {
        if (electionRepo.getState() != ElectionState.NOT_STARTED) {
            throw new ElectionException("Pemungutan suara hanya bisa dibuka sekali, dari status Belum Dimulai.");
        }
        return electionRepo.updateState(ElectionState.OPEN);
    }

    public boolean closeElection() {
        if (electionRepo.getState() != ElectionState.OPEN) {
            throw new ElectionException("Pemungutan suara belum dibuka atau sudah ditutup.");
        }
        return electionRepo.updateState(ElectionState.CLOSED);
    }
}
