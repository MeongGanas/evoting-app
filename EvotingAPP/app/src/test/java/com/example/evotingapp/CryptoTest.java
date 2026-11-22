package com.example.evotingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.evotingapp.model.EncryptedBallot;
import com.example.evotingapp.service.AesBallotCipher;
import com.example.evotingapp.service.Sha256HashChainService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CryptoTest {

    @Test
    public void aesRoundTripAndRandomIv() {
        AesBallotCipher cipher = new AesBallotCipher("kunci-uji");
        String a = cipher.encrypt("C01");
        String b = cipher.encrypt("C01");
        assertNotEquals("IV acak: pilihan sama harus menghasilkan ciphertext berbeda", a, b);
        assertEquals("C01", cipher.decrypt(a));
        assertEquals("C01", cipher.decrypt(b));
    }

    @Test
    public void aesWrongKeyCannotRevealChoice() {
        String ct = new AesBallotCipher("kunci-benar").encrypt("C02");
        try {
            // Padding hampir selalu gagal; bila kebetulan lolos, hasilnya pasti bukan "C02".
            assertNotEquals("C02", new AesBallotCipher("kunci-salah").decrypt(ct));
        } catch (IllegalStateException expected) {
            // ok: dekripsi ditolak
        }
    }

    @Test
    public void sha256KnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                new Sha256HashChainService().computeSha256("abc"));
    }

    @Test
    public void hashChainDetectsEveryKindOfTampering() {
        Sha256HashChainService chain = new Sha256HashChainService();
        List<EncryptedBallot> ballots = buildChain(chain, 3);
        assertTrue(chain.verifyChain(ballots));
        assertTrue("Kotak kosong dianggap utuh", chain.verifyChain(new ArrayList<>()));

        // isi diubah
        List<EncryptedBallot> modified = new ArrayList<>(ballots);
        EncryptedBallot b = modified.get(1);
        modified.set(1, new EncryptedBallot(2, "ff" + b.getEncryptedPayload().substring(2),
                b.getPreviousHash(), b.getReceiptHash()));
        assertFalse(chain.verifyChain(modified));

        // surat suara dihapus
        List<EncryptedBallot> deleted = new ArrayList<>(ballots);
        deleted.remove(1);
        assertFalse(chain.verifyChain(deleted));

        // urutan ditukar
        List<EncryptedBallot> swapped = new ArrayList<>(ballots);
        swapped.set(0, ballots.get(1));
        swapped.set(1, ballots.get(0));
        assertFalse(chain.verifyChain(swapped));
    }

    private static List<EncryptedBallot> buildChain(Sha256HashChainService chain, int n) {
        List<EncryptedBallot> list = new ArrayList<>();
        String prev = EncryptedBallot.GENESIS_HASH;
        for (int i = 1; i <= n; i++) {
            String payload = String.format("%032x", i);
            String receipt = chain.computeReceipt(prev, payload);
            list.add(new EncryptedBallot(i, payload, prev, receipt));
            prev = receipt;
        }
        return list;
    }
}
