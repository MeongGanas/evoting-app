package com.example.evotingapp.model;

/**
 * Siklus status pemilu:
 * NOT_STARTED -> OPEN -> CLOSED -> PUBLISHED, atau CLOSED -> TAMPERED bila Tamper Check gagal.
 */
public enum ElectionState {
    NOT_STARTED("Belum Dimulai"),
    OPEN("Pemungutan Suara Dibuka"),
    CLOSED("Pemungutan Suara Ditutup"),
    PUBLISHED("Hasil Dipublikasikan"),
    TAMPERED("Terindikasi Manipulasi");

    private final String label;

    ElectionState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
