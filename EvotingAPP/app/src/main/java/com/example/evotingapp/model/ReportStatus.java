package com.example.evotingapp.model;

public enum ReportStatus {
    SUBMITTED("Diterima"),
    IN_REVIEW("Sedang Ditinjau"),
    RESOLVED("Selesai"),
    REJECTED("Ditolak");

    private final String label;

    ReportStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
