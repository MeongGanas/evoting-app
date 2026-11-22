package com.example.evotingapp.repository;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.TallyResult;

import java.util.List;

public interface IElectionRepository {
    ElectionState getState();

    boolean updateState(ElectionState state);

    boolean saveResults(List<TallyResult> results);

    List<TallyResult> getResults();
}
