package com.example.evotingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.service.ElectionException;
import com.example.evotingapp.service.VoterRegistryService;

import org.junit.Before;
import org.junit.Test;

public class VoterRegistryTest {
    private static final String NIK = "3201123456789012";

    private InMemoryRepositories.Election election;
    private VoterRegistryService registry;

    @Before
    public void setUp() {
        election = new InMemoryRepositories.Election();
        registry = new VoterRegistryService(new InMemoryRepositories.Users(), election);
    }

    @Test
    public void kpuRegistersVoter() {
        Voter v = registry.registerVoter(" " + NIK + " ", " Budi_01 ", "rahasia1");
        assertEquals("budi_01", v.getUsername());
        assertEquals(NIK, v.getNik());
        assertTrue(v.canCastVote());
        assertEquals(1, registry.getAllVoters().size());
    }

    @Test
    public void rejectsInvalidInput() {
        expectFailure("123", "budi01", "rahasia1");               // NIK bukan 16 digit
        expectFailure("32011234567890AB", "budi01", "rahasia1");  // NIK ada huruf
        expectFailure(NIK, "bu", "rahasia1");                      // username terlalu pendek
        expectFailure(NIK, "budi 01", "rahasia1");                 // username ada spasi
        expectFailure(NIK, "budi01", "123");                       // password terlalu pendek
        assertEquals(0, registry.getAllVoters().size());
    }

    @Test
    public void rejectsDuplicateNikOrUsername() {
        registry.registerVoter(NIK, "budi01", "rahasia1");
        expectFailure(NIK, "budi02", "rahasia1");                  // satu NIK satu hak pilih
        expectFailure("3201123456789013", "budi01", "rahasia1");   // username kembar
        assertEquals(1, registry.getAllVoters().size());
    }

    @Test
    public void allowedWhileOpenButLockedAfterClose() {
        election.updateState(ElectionState.OPEN);
        registry.registerVoter(NIK, "budi01", "rahasia1");
        election.updateState(ElectionState.CLOSED);
        expectFailure("3201123456789013", "sari01", "rahasia1");
    }

    private void expectFailure(String nik, String username, String password) {
        try {
            registry.registerVoter(nik, username, password);
            fail("Seharusnya ditolak: " + nik + " / " + username);
        } catch (ElectionException expected) {
            // ok
        }
    }
}
