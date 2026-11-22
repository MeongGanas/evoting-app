package com.example.evotingapp.service;

import com.example.evotingapp.role.User;

public class AnonymousStrategy implements IIdentityStrategy {

    @Override
    public String resolveReporterId(User actor) {
        return null;
    }

    @Override
    public String resolveReporterName(User actor) {
        return "Anonim";
    }
}
