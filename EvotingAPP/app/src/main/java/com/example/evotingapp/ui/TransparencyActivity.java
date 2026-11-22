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
import com.example.evotingapp.role.IAuditCapable;
import com.example.evotingapp.role.IElectionManager;
import com.example.evotingapp.role.IVotable;
import com.example.evotingapp.role.User;
import com.example.evotingapp.service.BallotAuditService;
import com.example.evotingapp.service.CandidateService;
import com.example.evotingapp.service.ElectionService;
import com.example.evotingapp.service.TallyService;

import java.util.List;

/** Beranda: Papan Transparansi Pemilu (kandidat, dana, hasil, verifikasi receipt). */
public class TransparencyActivity extends BaseActivity {
    private CandidateService candidateService;
    private TallyService tallyService;
    private BallotAuditService auditService;
    private ElectionService electionService;

    private TextView tvElectionState;
    private TextView tvResultsInfo;
    private TextView tvBallotCount;
    private TextView tvVerifyResult;
    private LinearLayout resultsContainer;
    private LinearLayout candidatesContainer;
    private EditText inputReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = requireUser();
        if (user == null) {
            return;
        }
        setContentView(R.layout.activity_transparency);

        AppContainer c = container();
        candidateService = c.candidateService();
        tallyService = c.tallyService();
        auditService = c.auditService();
        electionService = c.electionService();

        ((TextView) findViewById(R.id.tvGreeting)).setText("Halo, " + user.getUsername());
        ((TextView) findViewById(R.id.tvRole)).setText("Masuk sebagai " + user.getRoleLabel());
        tvElectionState = findViewById(R.id.tvElectionState);
        tvResultsInfo = findViewById(R.id.tvResultsInfo);
        tvBallotCount = findViewById(R.id.tvBallotCount);
        tvVerifyResult = findViewById(R.id.tvVerifyResult);
        resultsContainer = findViewById(R.id.resultsContainer);
        candidatesContainer = findViewById(R.id.candidatesContainer);
        inputReceipt = findViewById(R.id.inputReceipt);

        // Menu tampil sesuai kemampuan peran (interface hak akses).
        setupMenu(R.id.btnVote, user instanceof IVotable, VotingActivity.class);
        setupMenu(R.id.btnPanwas, user instanceof IAuditCapable, PanwasActivity.class);
        setupMenu(R.id.btnKpu, user instanceof IElectionManager, KpuActivity.class);
        setupMenu(R.id.btnReport, true, ReportActivity.class);
        findViewById(R.id.btnLogout).setOnClickListener(v -> goToLogin());
        findViewById(R.id.btnVerify).setOnClickListener(v -> onVerifyReceipt());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Session.get() != null && candidatesContainer != null) {
            showCandidates();
            showResults();
        }
    }

    private void setupMenu(int buttonId, boolean visible, Class<?> target) {
        View button = findViewById(buttonId);
        button.setVisibility(visible ? View.VISIBLE : View.GONE);
        button.setOnClickListener(v -> startActivity(new Intent(this, target)));
    }

    private void showCandidates() {
        candidatesContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Candidate cand : candidateService.getAllCandidates()) {
            candidatesContainer.addView(UiRenderer.candidateCard(inflater, candidatesContainer, cand));
        }
    }

    private void showResults() {
        ElectionState state = electionService.getState();
        UiRenderer.setElectionBadge(tvElectionState, state);
        tvBallotCount.setText("Surat suara tercatat: " + auditService.countBallots());

        List<TallyResult> results = tallyService.getPublishedResults();
        if (results.isEmpty()) {
            resultsContainer.removeAllViews();
            if (state == ElectionState.TAMPERED) {
                tvResultsInfo.setText("Rekapitulasi dibekukan karena kotak suara terindikasi manipulasi.");
            } else {
                tvResultsInfo.setText("🔒 Hasil terkunci sampai kotak suara dibuka dengan PIN KPU dan PIN Panwas.");
            }
        } else {
            tvResultsInfo.setText("Hasil resmi setelah dekripsi dan Tamper Check.");
            UiRenderer.renderResults(getLayoutInflater(), resultsContainer, results);
        }
    }

    private void onVerifyReceipt() {
        String receipt = inputReceipt.getText().toString();
        if (receipt.trim().isEmpty()) {
            toast("Masukkan Receipt Hash terlebih dahulu.");
            return;
        }
        if (auditService.verifyReceipt(receipt)) {
            UiRenderer.setBadge(tvVerifyResult, "✓ Ditemukan: suara tercatat di kotak suara",
                    UiRenderer.Tone.SUCCESS);
        } else {
            UiRenderer.setBadge(tvVerifyResult, "✗ Tidak ditemukan", UiRenderer.Tone.DANGER);
        }
    }
}
