package com.example.evotingapp.service;

import com.example.evotingapp.role.User;

/** Strategy Pattern: menentukan identitas pelapor yang disimpan pada laporan. */
public interface IIdentityStrategy {
    /** @return id pelapor, atau null bila identitas disembunyikan. */
    String resolveReporterId(User actor);

    String resolveReporterName(User actor);
}
