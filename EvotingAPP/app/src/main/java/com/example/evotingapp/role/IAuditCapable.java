package com.example.evotingapp.role;

public interface IAuditCapable {
    boolean canRunTamperCheck();

    boolean canUpdateReport();
}
