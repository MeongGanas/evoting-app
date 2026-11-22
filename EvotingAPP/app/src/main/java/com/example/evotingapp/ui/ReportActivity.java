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
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

/** Laporan dugaan pelanggaran: kirim (anonim/teridentifikasi), lacak, dan tindak lanjut Panwas. */
public class ReportActivity extends BaseActivity {
    private ReportService reportService;
    private User user;
    private boolean isAuditor;

    private EditText inputDescription;
    private MaterialSwitch switchAnonymous;
    private TextView tvTicketResult;
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
        isAuditor = user instanceof IAuditCapable && ((IAuditCapable) user).canUpdateReport();

        inputDescription = findViewById(R.id.inputDescription);
        switchAnonymous = findViewById(R.id.switchAnonymous);
        tvTicketResult = findViewById(R.id.tvTicketResult);
        inputTicket = findViewById(R.id.inputTicket);
        trackResultContainer = findViewById(R.id.trackResultContainer);
        reportsContainer = findViewById(R.id.reportsContainer);
        tvNoReports = findViewById(R.id.tvNoReports);

        findViewById(R.id.btnSubmitReport).setOnClickListener(v -> onSubmitReport());
        findViewById(R.id.btnTrack).setOnClickListener(v -> onTrackTicket());

        if (isAuditor) {
            findViewById(R.id.panwasSection).setVisibility(View.VISIBLE);
            findViewById(R.id.formCard).setVisibility(View.GONE);
            renderAllReports();
        }
    }

    private void onSubmitReport() {
        // Strategy Pattern: identitas pelapor ditentukan oleh strategi yang dipilih.
        IIdentityStrategy strategy = switchAnonymous.isChecked()
                ? new AnonymousStrategy()
                : new IdentifiedStrategy();
        try {
            String ticket = reportService.submitReport(user, strategy, inputDescription.getText().toString());
            inputDescription.setText("");
            tvTicketResult.setText("Laporan terkirim. Simpan Kode Tiket Anda:\n" + ticket
                    + "\nGunakan kode ini untuk melacak status tindak lanjut.");
            tvTicketResult.setVisibility(View.VISIBLE);
            inputTicket.setText(ticket);
        } catch (ElectionException e) {
            toast(e.getMessage());
        }
    }

    private void onTrackTicket() {
        trackResultContainer.removeAllViews();
        ViolationReport report = reportService.trackReport(inputTicket.getText().toString());
        if (report == null) {
            toast("Kode Tiket tidak ditemukan.");
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
            toast(report.getTicketCode() + " → " + status.getLabel());
            renderAllReports();
        }
    }
}
