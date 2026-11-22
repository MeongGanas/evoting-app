package com.example.evotingapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.evotingapp.R;
import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.ViolationReport;
import com.example.evotingapp.role.IAuditCapable;
import com.example.evotingapp.role.User;
import com.example.evotingapp.service.AnonymousStrategy;
import com.example.evotingapp.service.ElectionException;
import com.example.evotingapp.service.IIdentityStrategy;
import com.example.evotingapp.service.IdentifiedStrategy;
import com.example.evotingapp.service.ReportService;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

/** Laporan dugaan pelanggaran: kirim (anonim/dengan nama), lacak, dan tindak lanjut Panwas. */
public class ReportActivity extends BaseActivity {
    private ReportService reportService;
    private User user;

    private EditText inputDescription;
    private MaterialButtonToggleGroup toggleIdentity;
    private TextView tvIdentityHint;
    private View ticketPanel;
    private TextView tvTicketCode;
    private EditText inputTicket;
    private LinearLayout trackResultContainer;
    private LinearLayout reportsContainer;
    private TextView tvNoReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = requireUser();
        if (user == null) {
            return;
        }
        setContentView(R.layout.activity_report);
        reportService = container().reportService();
        boolean isAuditor = user instanceof IAuditCapable && ((IAuditCapable) user).canUpdateReport();

        inputDescription = findViewById(R.id.inputDescription);
        toggleIdentity = findViewById(R.id.toggleIdentity);
        tvIdentityHint = findViewById(R.id.tvIdentityHint);
        ticketPanel = findViewById(R.id.ticketPanel);
        tvTicketCode = findViewById(R.id.tvTicketCode);
        inputTicket = findViewById(R.id.inputTicket);
        trackResultContainer = findViewById(R.id.trackResultContainer);
        reportsContainer = findViewById(R.id.reportsContainer);
        tvNoReports = findViewById(R.id.tvNoReports);

        toggleIdentity.addOnButtonCheckedListener((g, id, checked) -> updateIdentityHint());
        updateIdentityHint();
        findViewById(R.id.btnSubmitReport).setOnClickListener(v -> onSubmitReport());
        findViewById(R.id.btnTrack).setOnClickListener(v -> onTrackTicket());

        if (isAuditor) {
            setupHeader("Tindak lanjut laporan", "Tinjau laporan yang masuk dan perbarui statusnya agar pelapor bisa melacaknya.");
            findViewById(R.id.panwasSection).setVisibility(View.VISIBLE);
            findViewById(R.id.formCard).setVisibility(View.GONE);
            renderAllReports();
        } else {
            setupHeader("Laporan pelanggaran", "Laporkan dugaan kecurangan. Anda bisa melapor tanpa menyebutkan nama.");
        }
    }

    private boolean isAnonymous() {
        return toggleIdentity.getCheckedButtonId() == R.id.btnAnonymous;
    }

    private void updateIdentityHint() {
        tvIdentityHint.setText(isAnonymous()
                ? "Nama Anda tidak disimpan. Pengawas hanya melihat \"Anonim\"."
                : "Pengawas melihat username Anda dan dapat menghubungi Anda.");
    }

    private void onSubmitReport() {
        // Strategy Pattern: identitas pelapor ditentukan oleh strategi yang dipilih.
        IIdentityStrategy strategy = isAnonymous() ? new AnonymousStrategy() : new IdentifiedStrategy();
        try {
            String ticket = reportService.submitReport(user, strategy, inputDescription.getText().toString());
            inputDescription.setText("");
            tvTicketCode.setText(ticket);
            ticketPanel.setVisibility(View.VISIBLE);
            inputTicket.setText(ticket);
        } catch (ElectionException e) {
            toast(e.getMessage());
        }
    }

    private void onTrackTicket() {
        trackResultContainer.removeAllViews();
        ViolationReport report = reportService.trackReport(inputTicket.getText().toString());
        if (report == null) {
            toast("Kode tiket tidak ditemukan. Periksa kembali penulisannya.");
            return;
        }
        trackResultContainer.addView(UiRenderer.reportCard(getLayoutInflater(), trackResultContainer, report));
    }

    private void renderAllReports() {
        reportsContainer.removeAllViews();
        List<ViolationReport> reports = reportService.getAllReports();
        tvNoReports.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = getLayoutInflater();
        for (ViolationReport r : reports) {
            View card = UiRenderer.reportCard(inflater, reportsContainer, r);
            card.findViewById(R.id.actionRow).setVisibility(View.VISIBLE);
            card.findViewById(R.id.btnReview).setOnClickListener(v -> onUpdateReportStatus(r, ReportStatus.IN_REVIEW));
            card.findViewById(R.id.btnResolve).setOnClickListener(v -> onUpdateReportStatus(r, ReportStatus.RESOLVED));
            card.findViewById(R.id.btnReject).setOnClickListener(v -> onUpdateReportStatus(r, ReportStatus.REJECTED));
            reportsContainer.addView(card);
        }
    }

    private void onUpdateReportStatus(ViolationReport report, ReportStatus status) {
        if (reportService.updateReportStatus(report.getReportId(), status)) {
            toast(report.getTicketCode() + ": " + status.getLabel());
            renderAllReports();
        }
    }
}
