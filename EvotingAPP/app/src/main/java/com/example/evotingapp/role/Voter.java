package com.example.evotingapp.role;

public class Voter extends User implements IVotable {
    private final String nik;
    private boolean hasVoted;

    public Voter(String userId, String username, String nik, boolean hasVoted) {
        super(userId, username);
        this.nik = nik;
        this.hasVoted = hasVoted;
    }

    public String getNik() {
        return nik;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    @Override
    public boolean canCastVote() {
        return !hasVoted;
    }

    @Override
    public String getRoleLabel() {
        return "Pemilih";
    }
}
