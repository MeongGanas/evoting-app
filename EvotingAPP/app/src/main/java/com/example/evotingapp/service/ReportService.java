package com.example.evotingapp.service;

import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.ViolationReport;
import com.example.evotingapp.repository.IReportRepository;
import com.example.evotingapp.role.User;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/** Tanggung jawab tunggal: laporan dugaan pelanggaran dan tindak lanjutnya. */
public class ReportService {
    private static final String TICKET_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final IReportRepository reportRepo;
    private final SecureRandom random = new SecureRandom();

    public ReportService(IReportRepository reportRepo) {
        this.reportRepo = reportRepo;
    }

    /** @return Kode Tiket untuk melacak status laporan (berlaku juga untuk laporan anonim). */
    public String submitReport(User actor, IIdentityStrategy strategy, String desc) {
        if (desc == null || desc.trim().length() < 10) {
            throw new ElectionException("Uraian laporan minimal 10 karakter.");
        }
        String ticket = generateUniqueTicket();
        ViolationReport report = new ViolationReport(
                UUID.randomUUID().toString(),
                ticket,
                strategy.resolveReporterId(actor),
                strategy.resolveReporterName(actor),
                desc.trim(),
                ReportStatus.SUBMITTED,
                System.currentTimeMillis());
        if (!reportRepo.insert(report)) {
            throw new ElectionException("Gagal menyimpan laporan.");
        }
        return ticket;
    }

    public ViolationReport trackReport(String ticketCode) {
        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            return null;
        }
        return reportRepo.findByTicketCode(ticketCode.trim().toUpperCase());
    }

    public List<ViolationReport> getAllReports() {
        return reportRepo.findAll();
    }

    public boolean updateReportStatus(String reportId, ReportStatus status) {
        return reportRepo.updateStatus(reportId, status);
    }

    private String generateUniqueTicket() {
        String ticket;
        do {
            StringBuilder sb = new StringBuilder("LPR-");
            for (int i = 0; i < 6; i++) {
                sb.append(TICKET_CHARS.charAt(random.nextInt(TICKET_CHARS.length())));
            }
            ticket = sb.toString();
        } while (reportRepo.findByTicketCode(ticket) != null);
        return ticket;
    }
}
