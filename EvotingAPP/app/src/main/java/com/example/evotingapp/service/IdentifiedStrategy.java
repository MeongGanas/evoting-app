package com.example.evotingapp.service;

import com.example.evotingapp.role.User;

public class IdentifiedStrategy implements IIdentityStrategy {

    @Override
    public String resolveReporterId(User actor) {
        return actor.getUserId();
    }

    @Override
    public String resolveReporterName(User actor) {
        return actor.getUsername() + " (" + actor.getRoleLabel() + ")";
    }
}
