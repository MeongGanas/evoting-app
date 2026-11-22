package com.example.evotingapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.evotingapp.R;
import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.model.TallyResult;
import com.example.evotingapp.role.IElectionManager;
import com.example.evotingapp.role.User;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.service.CandidateService;
import com.example.evotingapp.service.VoterRegistryService;
import com.example.evotingapp.service.ElectionException;
import com.example.evotingapp.service.ElectionService;
import com.example.evotingapp.service.TallyService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Dashboard Panitia: kandidat & dana, buka/tutup pemungutan suara, buka kotak 2 PIN. */
public class KpuActivity extends BaseActivity {
    private CandidateService candidateService;
    private ElectionService electionService;
    private TallyService tallyService;

    private TextView tvElectionState;
    private EditText inputCandId;
    private EditText inputCandName;
    private EditText inputCandVision;
    private EditText inputCandFund;
    private EditText inputCandFundSummary;
    private EditText inputKpuPin;
    private EditText inputPanwasPin;
    private LinearLayout candidatesContainer;
    private LinearLayout resultsContainer;
    private VoterRegistryService voterRegistryService;
    private EditText inputVoterNik;
    private EditText inputVoterUsername;
    private EditText inputVoterPassword;
    private TextView tvDptSummary;
    private LinearLayout votersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = requireUser();
        if (user == null) {
            return;
        }
        if (!(user instanceof IElectionManager) || !((IElectionManager) user).canManageElection()) {
            toast("Halaman ini khusus Panitia (KPU).");
            finish();
            return;
        }
        setContentView(R.layout.activity_kpu);

        AppContainer c = container();
        candidateService = c.candidateService();
        electionService = c.electionService();
        tallyService = c.tallyService();

        tvElectionState = findViewById(R.id.tvElectionState);
        inputCandId = findViewById(R.id.inputCandId);
        inputCandName = findViewById(R.id.inputCandName);
        inputCandVision = findViewById(R.id.inputCandVision);
        inputCandFund = findViewById(R.id.inputCandFund);
        inputCandFundSummary = findViewById(R.id.inputCandFundSummary);
        inputKpuPin = findViewById(R.id.inputKpuPin);
        inputPanwasPin = findViewById(R.id.inputPanwasPin);
        candidatesContainer = findViewById(R.id.candidatesContainer);
        resultsContainer = findViewById(R.id.resultsContainer);
        inputVoterNik = findViewById(R.id.inputVoterNik);
        inputVoterUsername = findViewById(R.id.inputVoterUsername);
        inputVoterPassword = findViewById(R.id.inputVoterPassword);
        tvDptSummary = findViewById(R.id.tvDptSummary);
        votersContainer = findViewById(R.id.votersContainer);
        voterRegistryService = c.voterRegistryService();

        findViewById(R.id.btnOpenElection).setOnClickListener(v -> runAction(electionService::openElection,
                "Pemungutan suara dibuka."));
        findViewById(R.id.btnCloseElection).setOnClickListener(v -> runAction(electionService::closeElection,
                "Pemungutan suara ditutup."));
        findViewById(R.id.btnSaveCandidate).setOnClickListener(v -> onSaveCandidate());
        findViewById(R.id.btnUnlock).setOnClickListener(v -> onUnlockBallotBox());
        findViewById(R.id.btnResetDemo).setOnClickListener(v -> onResetDemo());
        findViewById(R.id.btnRegisterVoter).setOnClickListener(v -> onRegisterVoter());

        refresh();
    }

    private void onRegisterVoter() {
        try {
            Voter voter = voterRegistryService.registerVoter(
                    inputVoterNik.getText().toString(),
                    inputVoterUsername.getText().toString(),
                    inputVoterPassword.getText().toString());
            inputVoterNik.setText("");
            inputVoterUsername.setText("");
            inputVoterPassword.setText("");
            showMessage("Pemilih Terdaftar",
                    "Username: " + voter.getUsername() + "\nNIK: " + maskNik(voter.getNik())
                            + "\n\nSerahkan username dan password awal kepada pemilih.");
            refresh();
        } catch (ElectionException e) {
            showMessage("Pendaftaran Ditolak", e.getMessage());
        }
    }

    private void renderVoters() {
        List<Voter> voters = voterRegistryService.getAllVoters();
        int voted = 0;
        votersContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Voter v : voters) {
            View row = inflater.inflate(R.layout.item_voter, votersContainer, false);
            ((TextView) row.findViewById(R.id.tvUsername)).setText(v.getUsername());
            ((TextView) row.findViewById(R.id.tvNik)).setText("NIK " + maskNik(v.getNik()));
            boolean hasVoted = !v.canCastVote();
            if (hasVoted) {
                voted++;
            }
            UiRenderer.setBadge(row.findViewById(R.id.tvVoteStatus),
                    hasVoted ? "Sudah memilih" : "Belum memilih",
                    hasVoted ? UiRenderer.Tone.SUCCESS : UiRenderer.Tone.NEUTRAL);
            votersContainer.addView(row);
        }
        tvDptSummary.setText("Total DPT: " + voters.size() + " · Sudah memilih: " + voted);
    }

    /** Menyamarkan NIK agar data pribadi tidak tampil utuh di layar. */
    private static String maskNik(String nik) {
        if (nik == null || nik.length() < 8) {
            return "-";
        }
        return nik.substring(0, 4) + "********" + nik.substring(nik.length() - 4);
    }

    private interface Action {
        boolean run();
    }

    private void runAction(Action action, String successMessage) {
        try {
            if (action.run()) {
                toast(successMessage);
            }
        } catch (ElectionException e) {
            showMessage("Tidak dapat diproses", e.getMessage());
        }
        refresh();
    }

    private void refresh() {
        UiRenderer.setElectionBadge(tvElectionState, electionService.getState());
        renderVoters();
        List<TallyResult> results = tallyService.getPublishedResults();
        UiRenderer.renderResults(getLayoutInflater(), resultsContainer, results);

        candidatesContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Candidate c : candidateService.getAllCandidates()) {
            View card = UiRenderer.candidateCard(inflater, candidatesContainer, c);
            card.setOnClickListener(v -> fillForm(c));
            candidatesContainer.addView(card);
        }
    }

    private void fillForm(Candidate c) {
        inputCandId.setText(c.getCandidateId());
        inputCandName.setText(c.getFullName());
        inputCandVision.setText(c.getVisionMission());
        inputCandFund.setText(String.valueOf((long) c.getCampaignFundAmount()));
        inputCandFundSummary.setText(c.getFundSummary());
        inputCandId.requestFocus();
        toast("Mengubah " + c.getCandidateId());
    }

    private void onSaveCandidate() {
        double fund;
        try {
            String raw = inputCandFund.getText().toString().trim();
            fund = raw.isEmpty() ? 0 : Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            toast("Dana kampanye harus berupa angka.");
            return;
        }
        Candidate candidate = new Candidate(
                inputCandId.getText().toString().trim().toUpperCase(),
                inputCandName.getText().toString().trim(),
                inputCandVision.getText().toString().trim(),
                fund,
                inputCandFundSummary.getText().toString().trim());
        try {
            if (candidateService.saveCandidate(candidate)) {
                toast("Kandidat disimpan.");
                for (EditText e : new EditText[]{inputCandId, inputCandName, inputCandVision,
                        inputCandFund, inputCandFundSummary}) {
                    e.setText("");
                }
                refresh();
            }
        } catch (ElectionException e) {
            toast(e.getMessage());
        }
    }

    private void onUnlockBallotBox() {
        try {
            List<TallyResult> results = tallyService.unlockAndTally(
                    inputKpuPin.getText().toString(), inputPanwasPin.getText().toString());
            inputKpuPin.setText("");
            inputPanwasPin.setText("");
            showMessage("Kotak Suara Dibuka",
                    "Tamper Check lolos. " + results.size() + " hasil kandidat dipublikasikan ke Papan Transparansi.");
        } catch (ElectionException e) {
            showMessage("Buka Kotak Gagal", e.getMessage());
        }
        refresh();
    }

    private void onResetDemo() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Data Demo")
                .setMessage("Semua suara, laporan, hasil, dan perubahan kandidat akan dihapus dan dikembalikan ke data awal. Lanjutkan?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Reset", (d, w) -> {
                    container().dbHelper().resetDemoData();
                    toast("Data demo dikembalikan ke awal.");
                    goToLogin();
                })
                .show();
    }
}
