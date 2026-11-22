package com.example.evotingapp.model;

public class ViolationReport {
    private final String reportId;
    private final String ticketCode;
    private final String reporterId;      // null untuk laporan anonim
    private final String reporterDisplay;
    private final String description;
    private final ReportStatus status;
    private final long createdAt;

    public ViolationReport(String reportId, String ticketCode, String reporterId,
                           String reporterDisplay, String description,
                           ReportStatus status, long createdAt) {
        this.reportId = reportId;
        this.ticketCode = ticketCode;
        this.reporterId = reporterId;
        this.reporterDisplay = reporterDisplay;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getReportId() {
        return reportId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getReporterId() {
        return reporterId;
    }

    public String getReporterDisplay() {
        return reporterDisplay;
    }

    public String getDescription() {
        return description;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
