package com.example.evotingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.TallyResult;
import com.example.evotingapp.model.ViolationReport;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.service.AesBallotCipher;
import com.example.evotingapp.service.AnonymousStrategy;
import com.example.evotingapp.service.BallotAuditService;
import com.example.evotingapp.service.ElectionException;
import com.example.evotingapp.service.ElectionService;
import com.example.evotingapp.service.IdentifiedStrategy;
import com.example.evotingapp.service.ReportService;
import com.example.evotingapp.service.Sha256HashChainService;
import com.example.evotingapp.service.TallyService;
import com.example.evotingapp.service.VotingService;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ElectionFlowTest {
    private InMemoryRepositories.Users users;
    private InMemoryRepositories.Ballots ballots;
    private InMemoryRepositories.Election election;

    private VotingService voting;
    private BallotAuditService audit;
    private TallyService tally;
    private ElectionService electionService;

    @Before
    public void setUp() {
        users = new InMemoryRepositories.Users();
        ballots = new InMemoryRepositories.Ballots();
        election = new InMemoryRepositories.Election();
        InMemoryRepositories.Candidates candidates = new InMemoryRepositories.Candidates();
        AesBallotCipher cipher = new AesBallotCipher("kunci-uji");
        Sha256HashChainService hash = new Sha256HashChainService();

        voting = new VotingService(users, ballots, election, cipher, hash);
        audit = new BallotAuditService(ballots, hash);
        tally = new TallyService(users, ballots, candidates, election, cipher, audit);
        electionService = new ElectionService(election);
    }

    private static Voter voter(int i) {
        return new Voter("U-V" + i, "pemilih" + i, "nik" + i, false);
    }

    @Test
    public void fullHappyPath() {
        electionService.openElection();
        String r1 = voting.castVote(voter(1), "C01");
        String r2 = voting.castVote(voter(2), "C02");
        voting.castVote(voter(3), "C02");
        voting.castVote(voter(4), "C03");

        assertEquals(64, r1.length());
        assertTrue(audit.verifyReceipt(r1));
        assertTrue("Verifikasi tidak peka huruf besar/spasi", audit.verifyReceipt("  " + r2.toUpperCase() + " "));
        assertFalse(audit.verifyReceipt("abc"));
        assertTrue(tally.getPublishedResults().isEmpty());

        electionService.closeElection();
        List<TallyResult> results = tally.unlockAndTally("1111", "2222");

        assertEquals(ElectionState.PUBLISHED, election.getState());
        assertEquals("C02", results.get(0).getCandidateId());
        assertEquals(2, results.get(0).getTotalVotes());
        assertEquals(50.0, results.get(0).getPercentage(), 0.001);
        int total = 0;
        for (TallyResult r : results) {
            total += r.getTotalVotes();
        }
        assertEquals(4, total);
        assertEquals(3, tally.getPublishedResults().size());
    }

    @Test
    public void voterCannotVoteTwice() {
        electionService.openElection();
        voting.castVote(voter(1), "C01");
        try {
            voting.castVote(voter(1), "C02"); // objek baru, tetapi repository sudah mencatat
            fail("Suara ganda harus ditolak");
        } catch (ElectionException expected) {
            // ok
        }
        assertEquals(1, ballots.ballots.size());
    }

    @Test(expected = ElectionException.class)
    public void cannotVoteWhenNotOpen() {
        voting.castVote(voter(1), "C01");
    }

    @Test
    public void ballotStoresNoIdentityOrPlainChoice() {
        electionService.openElection();
        voting.castVote(voter(1), "C01");
        String payload = ballots.ballots.get(0).getEncryptedPayload();
        assertFalse(payload.contains("C01"));
        assertFalse(payload.contains("U-V1"));
    }

    @Test
    public void unlockRequiresBothValidPins() {
        electionService.openElection();
        voting.castVote(voter(1), "C01");
        electionService.closeElection();
        expectFailure(() -> tally.unlockAndTally("1111", "0000"));
        expectFailure(() -> tally.unlockAndTally("9999", "2222"));
        expectFailure(() -> tally.unlockAndTally("2222", "1111")); // PIN tertukar
        assertEquals(ElectionState.CLOSED, election.getState());
    }

    @Test
    public void unlockRequiresClosedElection() {
        electionService.openElection();
        expectFailure(() -> tally.unlockAndTally("1111", "2222"));
    }

    @Test
    public void tamperedBoxIsFrozen() {
        electionService.openElection();
        voting.castVote(voter(1), "C01");
        voting.castVote(voter(2), "C02");
        electionService.closeElection();
        assertTrue(audit.runTamperCheck());

        ballots.tamperPayload(0, new AesBallotCipher("kunci-uji").encrypt("C02"));
        assertFalse(audit.runTamperCheck());

        expectFailure(() -> tally.unlockAndTally("1111", "2222"));
        assertEquals(ElectionState.TAMPERED, election.getState());
        assertTrue(tally.getPublishedResults().isEmpty());
    }

    @Test
    public void electionStateTransitions() {
        expectFailure(electionService::closeElection);
        electionService.openElection();
        expectFailure(electionService::openElection);
        electionService.closeElection();
        expectFailure(electionService::openElection);
        assertEquals(ElectionState.CLOSED, election.getState());
    }

    @Test
    public void reportsAnonymousAndIdentifiedWithTicketTracking() {
        InMemoryRepositories.Reports reports = new InMemoryRepositories.Reports();
        ReportService service = new ReportService(reports);

        String anonTicket = service.submitReport(voter(1), new AnonymousStrategy(),
                "Ada pembagian uang di dekat TPS 3 pagi ini.");
        String namedTicket = service.submitReport(voter(2), new IdentifiedStrategy(),
                "Kotak suara sempat ditinggal tanpa pengawas.");

        assertTrue(anonTicket.matches("LPR-[A-Z0-9]{6}"));
        ViolationReport anon = service.trackReport(anonTicket.toLowerCase());
        assertNotNull(anon);
        assertNull("Laporan anonim tidak menyimpan id pelapor", anon.getReporterId());
        assertEquals("Anonim", anon.getReporterDisplay());

        ViolationReport named = service.trackReport(namedTicket);
        assertEquals("U-V2", named.getReporterId());

        assertTrue(service.updateReportStatus(anon.getReportId(), ReportStatus.IN_REVIEW));
        assertEquals(ReportStatus.IN_REVIEW, service.trackReport(anonTicket).getStatus());
        assertNull(service.trackReport("LPR-XXXXXX"));
        expectFailure(() -> service.submitReport(voter(3), new AnonymousStrategy(), "pendek"));
    }

    private static void expectFailure(Runnable r) {
        try {
            r.run();
            fail("Seharusnya ElectionException");
        } catch (ElectionException expected) {
            // ok
        }
    }
}
