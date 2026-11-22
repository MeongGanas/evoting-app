package com.example.evotingapp.model;

public class TallyResult {
    private final String candidateId;
    private final String candidateName;
    private final int totalVotes;
    private final double percentage;

    public TallyResult(String candidateId, String candidateName, int totalVotes, double percentage) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.totalVotes = totalVotes;
        this.percentage = percentage;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public double getPercentage() {
        return percentage;
    }
}
