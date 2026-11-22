package com.example.evotingapp.role;

public class KpuAdmin extends User implements IElectionManager {

    public KpuAdmin(String userId, String username) {
        super(userId, username);
    }

    @Override
    public boolean canManageElection() {
        return true;
    }

    @Override
    public String getRoleLabel() {
        return "Panitia (KPU)";
    }
}
