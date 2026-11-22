package com.example.evotingapp.model;

/**
 * Surat suara terenkripsi. Sengaja tidak menyimpan userId maupun timestamp
 * sehingga suara tidak dapat dikaitkan kembali dengan pemilih.
 */
public class EncryptedBallot {
    /** Hash awal rantai (sebelum surat suara pertama). */
    public static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private final int sequenceNo;
    private final String encryptedPayload;
    private final String previousHash;
    private final String receiptHash;

    public EncryptedBallot(int sequenceNo, String encryptedPayload,
                           String previousHash, String receiptHash) {
        this.sequenceNo = sequenceNo;
        this.encryptedPayload = encryptedPayload;
        this.previousHash = previousHash;
        this.receiptHash = receiptHash;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getReceiptHash() {
        return receiptHash;
    }
}
