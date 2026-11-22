package com.example.evotingapp.repository;

import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.ViolationReport;

import java.util.List;

public interface IReportRepository {
    boolean insert(ViolationReport report);

    boolean updateStatus(String reportId, ReportStatus status);

    /** @return laporan dengan kode tiket tersebut, atau null bila tidak ditemukan. */
    ViolationReport findByTicketCode(String ticketCode);

    List<ViolationReport> findAll();
}
