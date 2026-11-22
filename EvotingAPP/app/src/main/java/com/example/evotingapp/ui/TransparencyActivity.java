package com.example.evotingapp.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

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

/** Beranda: Papan Transparansi Pemilu (siklus, hasil, cek struk, kandidat & dana). */
public class TransparencyActivity extends BaseActivity {
    private CandidateService candidateService;
    private TallyService tallyService;
    private BallotAuditService auditService;
    private ElectionService electionService;

    private LinearLayout lifecycleRow;
    private TextView tvStateHint;
    private View resultsLocked;
    private ImageView ivResultsLock;
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
        ((TextView) findViewById(R.id.tvRole)).setText(user.getRoleLabel());
        lifecycleRow = findViewById(R.id.lifecycleRow);
        tvStateHint = findViewById(R.id.tvStateHint);
        resultsLocked = findViewById(R.id.resultsLocked);
        ivResultsLock = findViewById(R.id.ivResultsLock);
        tvResultsInfo = findViewById(R.id.tvResultsInfo);
        tvBallotCount = findViewById(R.id.tvBallotCount);
        tvVerifyResult = findViewById(R.id.tvVerifyResult);
        resultsContainer = findViewById(R.id.resultsContainer);
        candidatesContainer = findViewById(R.id.candidatesContainer);
        inputReceipt = findViewById(R.id.inputReceipt);

        // Aksi utama tampil sesuai kemampuan peran (interface hak akses).
        setupMenu(R.id.btnVote, user instanceof IVotable, VotingActivity.class);
        setupMenu(R.id.btnPanwas, user instanceof IAuditCapable, PanwasActivity.class);
        setupMenu(R.id.btnKpu, user instanceof IElectionManager, KpuActivity.class);
        setupMenu(R.id.btnReport, true, ReportActivity.class);
        if (user instanceof IAuditCapable) {
            ((TextView) findViewById(R.id.tvReportDesc)).setText("Tinjau dan tindak lanjuti laporan masuk");
        }
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

    private void setupMenu(int viewId, boolean visible, Class<?> target) {
        View view = findViewById(viewId);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        view.setOnClickListener(v -> startActivity(new Intent(this, target)));
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
        UiRenderer.renderLifecycle(lifecycleRow, state);
        tvStateHint.setText(UiRenderer.describe(state));
        int count = auditService.countBallots();
        tvBallotCount.setText(count + " surat suara tercatat di kotak suara");

        List<TallyResult> results = tallyService.getPublishedResults();
        if (results.isEmpty()) {
            resultsContainer.removeAllViews();
            resultsLocked.setVisibility(View.VISIBLE);
            boolean frozen = state == ElectionState.TAMPERED;
            int tint = ContextCompat.getColor(this, frozen ? R.color.status_danger : R.color.brand_primary);
            ViewCompat.setBackgroundTintList(ivResultsLock, ColorStateList.valueOf(tint));
            ivResultsLock.setImageResource(frozen ? R.drawable.ic_alert : R.drawable.ic_lock);
            tvResultsInfo.setText(frozen
                    ? "Rekapitulasi dibekukan. Pengawas sedang mengaudit kotak suara."
                    : "Hasil masih terkunci. Kotak suara hanya bisa dibuka dengan PIN KPU dan PIN Panwas setelah pemungutan ditutup.");
        } else {
            resultsLocked.setVisibility(View.GONE);
            UiRenderer.renderResults(getLayoutInflater(), resultsContainer, results);
        }
    }

    private void onVerifyReceipt() {
        String receipt = inputReceipt.getText().toString();
        if (receipt.trim().isEmpty()) {
            toast("Tempel Receipt Hash dari struk Anda terlebih dahulu.");
            return;
        }
        if (auditService.verifyReceipt(receipt)) {
            UiRenderer.setBadge(tvVerifyResult, "Tercatat di kotak suara", UiRenderer.Tone.SUCCESS);
        } else {
            UiRenderer.setBadge(tvVerifyResult, "Tidak ditemukan", UiRenderer.Tone.DANGER);
        }
    }
}
