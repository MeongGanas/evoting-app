package com.example.evotingapp.ui;

import android.content.Context;

import com.example.evotingapp.repository.ElectionDbHelper;
import com.example.evotingapp.repository.IBallotRepository;
import com.example.evotingapp.repository.ICandidateRepository;
import com.example.evotingapp.repository.IElectionRepository;
import com.example.evotingapp.repository.IReportRepository;
import com.example.evotingapp.repository.IUserRepository;
import com.example.evotingapp.repository.SQLiteBallotRepository;
import com.example.evotingapp.repository.SQLiteCandidateRepository;
import com.example.evotingapp.repository.SQLiteElectionRepository;
import com.example.evotingapp.repository.SQLiteReportRepository;
import com.example.evotingapp.repository.SQLiteUserRepository;
import com.example.evotingapp.service.AesBallotCipher;
import com.example.evotingapp.service.AuthService;
import com.example.evotingapp.service.BallotAuditService;
import com.example.evotingapp.service.CandidateService;
import com.example.evotingapp.service.ElectionService;
import com.example.evotingapp.service.IBallotCipher;
import com.example.evotingapp.service.IHashChainService;
import com.example.evotingapp.service.ReportService;
import com.example.evotingapp.service.Sha256HashChainService;
import com.example.evotingapp.service.TallyService;
import com.example.evotingapp.service.VoterRegistryService;
import com.example.evotingapp.service.VotingService;

/**
 * Composition root: satu-satunya tempat yang mengenal kelas konkret (SQLite, AES, SHA-256).
 * Semua service menerima dependensinya sebagai interface lewat constructor (DIP).
 */
public class AppContainer {
    /** Kunci simulasi; pada sistem nyata disimpan terpisah (mis. HSM / dibagi KPU & Panwas). */
    private static final String BALLOT_KEY = "EvotingAPP-SDG16-Demo-Key";

    private final ElectionDbHelper dbHelper;
    private final AuthService authService;
    private final VotingService votingService;
    private final BallotAuditService auditService;
    private final TallyService tallyService;
    private final ElectionService electionService;
    private final CandidateService candidateService;
    private final ReportService reportService;
    private final VoterRegistryService voterRegistryService;

    public AppContainer(Context context) {
        dbHelper = new ElectionDbHelper(context.getApplicationContext());

        IUserRepository userRepo = new SQLiteUserRepository(dbHelper);
        IBallotRepository ballotRepo = new SQLiteBallotRepository(dbHelper);
        ICandidateRepository candidateRepo = new SQLiteCandidateRepository(dbHelper);
        IElectionRepository electionRepo = new SQLiteElectionRepository(dbHelper);
        IReportRepository reportRepo = new SQLiteReportRepository(dbHelper);

        IBallotCipher cipher = new AesBallotCipher(BALLOT_KEY);
        IHashChainService hashService = new Sha256HashChainService();

        authService = new AuthService(userRepo);
        votingService = new VotingService(userRepo, ballotRepo, electionRepo, cipher, hashService);
        auditService = new BallotAuditService(ballotRepo, hashService);
        tallyService = new TallyService(userRepo, ballotRepo, candidateRepo, electionRepo, cipher, auditService);
        electionService = new ElectionService(electionRepo);
        candidateService = new CandidateService(candidateRepo);
        reportService = new ReportService(reportRepo);
        voterRegistryService = new VoterRegistryService(userRepo, electionRepo);
    }

    public VoterRegistryService voterRegistryService() {
        return voterRegistryService;
    }

    public ElectionDbHelper dbHelper() {
        return dbHelper;
    }

    public AuthService authService() {
        return authService;
    }

    public VotingService votingService() {
        return votingService;
    }

    public BallotAuditService auditService() {
        return auditService;
    }

    public TallyService tallyService() {
        return tallyService;
    }

    public ElectionService electionService() {
        return electionService;
    }

    public CandidateService candidateService() {
        return candidateService;
    }

    public ReportService reportService() {
        return reportService;
    }
}
