package com.example.evotingapp.role;

public class Panwas extends User implements IAuditCapable {

    public Panwas(String userId, String username) {
        super(userId, username);
    }

    @Override
    public boolean canRunTamperCheck() {
        return true;
    }

    @Override
    public boolean canUpdateReport() {
        return true;
    }

    @Override
    public String getRoleLabel() {
        return "Pengawas (Panwas)";
    }
}
