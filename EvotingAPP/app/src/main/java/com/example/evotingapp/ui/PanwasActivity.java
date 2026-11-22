package com.example.evotingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.evotingapp.R;
import com.example.evotingapp.role.IAuditCapable;
import com.example.evotingapp.role.User;
import com.example.evotingapp.service.BallotAuditService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Dashboard Pengawas: Tamper Check hash chain & akses tindak lanjut laporan. */
public class PanwasActivity extends BaseActivity {
    private BallotAuditService auditService;

    private TextView tvElectionState;
    private TextView tvIntegrityStatus;
    private TextView tvIntegrityDetail;
    private MaterialCardView integrityCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = requireUser();
        if (user == null) {
            return;
        }
        if (!(user instanceof IAuditCapable) || !((IAuditCapable) user).canRunTamperCheck()) {
            toast("Halaman ini khusus Pengawas.");
            finish();
            return;
        }
        setContentView(R.layout.activity_panwas);
        auditService = container().auditService();

        tvElectionState = findViewById(R.id.tvElectionState);
        tvIntegrityStatus = findViewById(R.id.tvIntegrityStatus);
        tvIntegrityDetail = findViewById(R.id.tvIntegrityDetail);
        integrityCard = findViewById(R.id.integrityCard);

        findViewById(R.id.btnTamperCheck).setOnClickListener(v -> onRunTamperCheck());
        findViewById(R.id.btnSimulateTamper).setOnClickListener(v -> onSimulateTamper());
        findViewById(R.id.btnReports).setOnClickListener(v ->
                startActivity(new Intent(this, ReportActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auditService != null) {
            UiRenderer.setElectionBadge(tvElectionState, container().electionService().getState());
            tvIntegrityDetail.setText("Jumlah surat suara: " + auditService.countBallots());
        }
    }

    private void onRunTamperCheck() {
        boolean intact = auditService.runTamperCheck();
        int count = auditService.countBallots();
        int fg = intact ? R.color.status_success : R.color.status_danger;
        int bg = intact ? R.color.status_success_bg : R.color.status_danger_bg;
        tvIntegrityStatus.setText(intact ? "✓ AMAN — rantai hash utuh" : "⚠ TAMPER ALERT");
        tvIntegrityStatus.setTextColor(ContextCompat.getColor(this, fg));
        integrityCard.setCardBackgroundColor(ContextCompat.getColor(this, bg));
        integrityCard.setStrokeColor(ContextCompat.getColor(this, fg));
        tvIntegrityDetail.setText(intact
                ? "Seluruh " + count + " surat suara tervalidasi SHA-256 dari awal hingga akhir rantai."
                : "Ditemukan surat suara yang isinya tidak cocok dengan hash. Rekapitulasi akan dibekukan saat kotak dibuka.");
    }

    private void onSimulateTamper() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Simulasi Manipulasi DB")
                .setMessage("Isi surat suara pertama akan diubah langsung di database. Lanjutkan?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Ubah Data", (d, w) -> {
                    if (container().dbHelper().simulateTamper()) {
                        toast("Data surat suara telah dimanipulasi. Jalankan Tamper Check.");
                    } else {
                        toast("Belum ada surat suara untuk dimanipulasi.");
                    }
                })
                .show();
    }
}
