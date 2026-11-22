package com.example.evotingapp.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.evotingapp.R;
import com.example.evotingapp.role.IAuditCapable;
import com.example.evotingapp.role.User;
import com.example.evotingapp.service.BallotAuditService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Dashboard Pengawas: Tamper Check hash chain & akses tindak lanjut laporan. */
public class PanwasActivity extends BaseActivity {
    private BallotAuditService auditService;

    private TextView tvHeaderBadge;
    private ImageView ivIntegrity;
    private TextView tvIntegrityStatus;
    private TextView tvIntegrityDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = requireUser();
        if (user == null) {
            return;
        }
        if (!(user instanceof IAuditCapable) || !((IAuditCapable) user).canRunTamperCheck()) {
            toast("Halaman ini khusus pengawas.");
            finish();
            return;
        }
        setContentView(R.layout.activity_panwas);
        setupHeader("Pengawasan", "Pastikan tidak ada surat suara yang diubah, dihapus, atau disisipkan.");
        auditService = container().auditService();

        tvHeaderBadge = findViewById(R.id.tvHeaderBadge);
        ivIntegrity = findViewById(R.id.ivIntegrity);
        tvIntegrityStatus = findViewById(R.id.tvIntegrityStatus);
        tvIntegrityDetail = findViewById(R.id.tvIntegrityDetail);

        findViewById(R.id.btnTamperCheck).setOnClickListener(v -> onRunTamperCheck());
        findViewById(R.id.btnSimulateTamper).setOnClickListener(v -> onSimulateTamper());
        findViewById(R.id.btnReports).setOnClickListener(v ->
                startActivity(new Intent(this, ReportActivity.class)));
        showIdle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auditService != null) {
            UiRenderer.setElectionBadge(tvHeaderBadge, container().electionService().getState());
        }
    }

    private void paint(UiRenderer.Tone tone, int icon) {
        int[] c = UiRenderer.colorsOf(tone);
        ViewCompat.setBackgroundTintList(ivIntegrity,
                ColorStateList.valueOf(ContextCompat.getColor(this, c[0])));
        ivIntegrity.setImageResource(icon);
        tvIntegrityStatus.setTextColor(ContextCompat.getColor(this,
                tone == UiRenderer.Tone.NEUTRAL ? R.color.text_primary : c[0]));
    }

    private void showIdle() {
        paint(UiRenderer.Tone.NEUTRAL, R.drawable.ic_lock);
        tvIntegrityStatus.setText("Belum diperiksa");
        tvIntegrityDetail.setText(auditService.countBallots()
                + " surat suara di kotak. Jalankan pemeriksaan untuk memvalidasi setiap hash SHA-256.");
    }

    private void onRunTamperCheck() {
        boolean intact = auditService.runTamperCheck();
        int count = auditService.countBallots();
        if (intact) {
            paint(UiRenderer.Tone.SUCCESS, R.drawable.ic_check);
            tvIntegrityStatus.setText("Kotak suara utuh");
            tvIntegrityDetail.setText("Semua " + count + " surat suara cocok dengan hash-nya, dari awal sampai akhir rantai.");
        } else {
            paint(UiRenderer.Tone.DANGER, R.drawable.ic_alert);
            tvIntegrityStatus.setText("Terdeteksi manipulasi");
            tvIntegrityDetail.setText("Ada surat suara yang isinya tidak cocok dengan hash. Rekapitulasi akan dibekukan saat kotak dibuka.");
        }
    }

    private void onSimulateTamper() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Ubah data surat suara?")
                .setMessage("Isi surat suara pertama akan diubah langsung di database. Gunakan hanya untuk demonstrasi.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Ubah data", (d, w) -> {
                    if (container().dbHelper().simulateTamper()) {
                        showIdle();
                        toast("Data diubah. Jalankan pemeriksaan rantai hash.");
                    } else {
                        toast("Belum ada surat suara yang bisa diubah.");
                    }
                })
                .show();
    }
}
