package com.example.evotingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.evotingapp.R;
import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.model.ElectionState;
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
    private View candidateForm;
    private TextView btnToggleCandidateForm;
    private TextView tvStateHint;
    private VoterRegistryService voterRegistryService;
    private TextView tvDptSummary;

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
        setupHeader("Panitia pemilu", "Siapkan kandidat dan pemilih, jalankan pemungutan suara, lalu buka kotak suara bersama pengawas.");
        candidateForm = findViewById(R.id.candidateForm);
        btnToggleCandidateForm = findViewById(R.id.btnToggleCandidateForm);
        btnToggleCandidateForm.setOnClickListener(v -> setCandidateFormOpen(candidateForm.getVisibility() != View.VISIBLE));
        tvStateHint = findViewById(R.id.tvStateHint);

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
        tvDptSummary = findViewById(R.id.tvDptSummary);
        voterRegistryService = c.voterRegistryService();

        findViewById(R.id.btnOpenElection).setOnClickListener(v -> runAction(electionService::openElection,
                "Pemungutan suara dibuka"));
        findViewById(R.id.btnCloseElection).setOnClickListener(v -> runAction(electionService::closeElection,
                "Pemungutan suara ditutup"));
        findViewById(R.id.btnSaveCandidate).setOnClickListener(v -> onSaveCandidate());
        findViewById(R.id.btnUnlock).setOnClickListener(v -> onUnlockBallotBox());
        findViewById(R.id.btnResetDemo).setOnClickListener(v -> onResetDemo());
        findViewById(R.id.btnOpenVoters).setOnClickListener(v ->
                startActivity(new Intent(this, VoterRegistryActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvDptSummary != null) {
            refresh();
        }
    }

    private void renderVoterSummary() {
        List<Voter> voters = voterRegistryService.getAllVoters();
        int voted = 0;
        for (Voter v : voters) {
            if (!v.canCastVote()) {
                voted++;
            }
        }
        tvDptSummary.setText(voters.size() + " pemilih terdaftar, " + voted + " sudah memilih");
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
            showMessage("Status belum berubah", e.getMessage());
        }
        refresh();
    }

    private void setCandidateFormOpen(boolean open) {
        candidateForm.setVisibility(open ? View.VISIBLE : View.GONE);
        btnToggleCandidateForm.setText(open ? "Tutup formulir" : "Tambah kandidat");
    }

    private void refresh() {
        ElectionState state = electionService.getState();
        UiRenderer.setElectionBadge(tvElectionState, state);
        tvStateHint.setText(UiRenderer.describe(state));
        renderVoterSummary();
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
        setCandidateFormOpen(true);
        inputCandName.requestFocus();
        toast("Mengubah kandidat " + c.getCandidateId());
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
                toast("Kandidat disimpan");
                for (EditText e : new EditText[]{inputCandId, inputCandName, inputCandVision,
                        inputCandFund, inputCandFundSummary}) {
                    e.setText("");
                }
                setCandidateFormOpen(false);
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
            showMessage("Kotak suara dibuka",
                    "Rantai hash utuh. Hasil " + results.size() + " pasangan calon sudah diumumkan di papan transparansi.");
        } catch (ElectionException e) {
            showMessage("Kotak suara tidak dapat dibuka", e.getMessage());
        }
        refresh();
    }

    private void onResetDemo() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset data demo?")
                .setMessage("Semua suara, laporan, hasil, pemilih baru, dan perubahan kandidat akan dihapus. Data kembali seperti saat aplikasi pertama dipasang.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Reset data", (d, w) -> {
                    container().dbHelper().resetDemoData();
                    toast("Data demo dikembalikan ke awal");
                    goToLogin();
                })
                .show();
    }
}
