package com.example.evotingapp.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.evotingapp.R;
import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.model.ReportStatus;
import com.example.evotingapp.model.TallyResult;
import com.example.evotingapp.model.ViolationReport;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper tampilan yang dipakai bersama oleh beberapa layar. */
final class UiRenderer {

    enum Tone { SUCCESS, DANGER, WARNING, NEUTRAL, INFO }

    private static final Pattern NUMBERED_NAME = Pattern.compile("^\\s*(\\d+)\\s*[-–.]\\s*(.+)$");
    private static final String[] LIFECYCLE_LABELS = {"Persiapan", "Pemungutan", "Ditutup", "Hasil"};

    private UiRenderer() {
    }

    // ---------------------------------------------------------------- badge

    static void setBadge(TextView badge, String text, Tone tone) {
        Context ctx = badge.getContext();
        int[] c = colorsOf(tone);
        badge.setText(text);
        badge.setTextColor(ContextCompat.getColor(ctx, c[0]));
        ViewCompat.setBackgroundTintList(badge, ColorStateList.valueOf(ContextCompat.getColor(ctx, c[1])));
        badge.setVisibility(View.VISIBLE);
    }

    /** @return {warna teks/ikon, warna latar} untuk sebuah nada status. */
    static int[] colorsOf(Tone tone) {
        switch (tone) {
            case SUCCESS:
                return new int[]{R.color.status_success, R.color.status_success_bg};
            case DANGER:
                return new int[]{R.color.status_danger, R.color.status_danger_bg};
            case WARNING:
                return new int[]{R.color.status_warning, R.color.status_warning_bg};
            case INFO:
                return new int[]{R.color.brand_primary, R.color.brand_primary_container};
            default:
                return new int[]{R.color.status_neutral, R.color.status_neutral_bg};
        }
    }

    static Tone toneOf(ElectionState state) {
        switch (state) {
            case OPEN:
                return Tone.SUCCESS;
            case CLOSED:
                return Tone.WARNING;
            case PUBLISHED:
                return Tone.INFO;
            case TAMPERED:
                return Tone.DANGER;
            default:
                return Tone.NEUTRAL;
        }
    }

    static void setElectionBadge(TextView badge, ElectionState state) {
        setBadge(badge, state.getLabel(), toneOf(state));
    }

    static Tone toneOf(ReportStatus status) {
        switch (status) {
            case RESOLVED:
                return Tone.SUCCESS;
            case REJECTED:
                return Tone.DANGER;
            case IN_REVIEW:
                return Tone.WARNING;
            default:
                return Tone.INFO;
        }
    }

    /** Kalimat penjelas tahap pemilu yang sedang berjalan, dari sudut pandang pengguna. */
    static String describe(ElectionState state) {
        switch (state) {
            case OPEN:
                return "Pemungutan suara sedang berlangsung.";
            case CLOSED:
                return "Pemungutan suara selesai. Kotak suara menunggu dibuka dengan PIN KPU dan PIN Panwas.";
            case PUBLISHED:
                return "Hasil resmi sudah diumumkan dan bisa dilihat di bawah.";
            case TAMPERED:
                return "Kotak suara dibekukan karena ada data surat suara yang berubah.";
            default:
                return "Panitia sedang menyiapkan kandidat dan daftar pemilih.";
        }
    }

    // ------------------------------------------------------------ lifecycle

    /** Mengisi stepper siklus pemilu pada header ungu. */
    static void renderLifecycle(LinearLayout row, ElectionState state) {
        Context ctx = row.getContext();
        row.removeAllViews();
        int current;
        switch (state) {
            case OPEN:
                current = 1;
                break;
            case CLOSED:
                current = 2;
                break;
            case PUBLISHED:
            case TAMPERED:
                current = 3;
                break;
            default:
                current = 0;
        }
        for (int i = 0; i < LIFECYCLE_LABELS.length; i++) {
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            FrameLayout dotBox = new FrameLayout(ctx);
            dotBox.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 16), dp(ctx, 16)));
            View dot = new View(ctx);
            int size = i == current ? 16 : 10;
            FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(ctx, size), dp(ctx, size), Gravity.CENTER);
            dot.setLayoutParams(dlp);
            dot.setBackgroundResource(R.drawable.bg_dot);
            int dotColor;
            if (i == 3 && state == ElectionState.TAMPERED) {
                dotColor = R.color.status_danger;
            } else {
                dotColor = i <= current ? R.color.brand_primary : R.color.on_ink_faint;
            }
            ViewCompat.setBackgroundTintList(dot, ColorStateList.valueOf(ContextCompat.getColor(ctx, dotColor)));
            dotBox.addView(dot);

            TextView label = new TextView(ctx);
            label.setText(i == 3 && state == ElectionState.TAMPERED ? "Dibekukan" : LIFECYCLE_LABELS[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setGravity(Gravity.CENTER);
            label.setPadding(0, dp(ctx, 6), 0, 0);
            label.setTextColor(ContextCompat.getColor(ctx, i == current ? R.color.white : R.color.on_ink_muted));
            if (i == current) {
                label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
            }

            col.addView(dotBox);
            col.addView(label);
            col.setContentDescription("Tahap " + label.getText() + (i == current ? ", sedang berjalan" : ""));
            row.addView(col);
        }
    }

    // ------------------------------------------------------------ kandidat

    /** Membuat kartu kandidat bergaya surat suara (tombol "Coblos" disembunyikan secara default). */
    static View candidateCard(LayoutInflater inflater, ViewGroup parent, Candidate c) {
        View card = inflater.inflate(R.layout.item_candidate, parent, false);
        String number;
        String name;
        Matcher m = NUMBERED_NAME.matcher(c.getFullName());
        if (m.matches()) {
            number = m.group(1);
            name = m.group(2);
        } else {
            number = c.getCandidateId().replaceAll("\\D", "");
            name = c.getFullName();
        }
        if (number.isEmpty()) {
            number = "–";
        } else if (number.length() == 1) {
            number = "0" + number;
        }
        ((TextView) card.findViewById(R.id.tvNumber)).setText(number);
        ((TextView) card.findViewById(R.id.tvName)).setText(name);
        ((TextView) card.findViewById(R.id.tvVision)).setText(c.getVisionMission());
        ((TextView) card.findViewById(R.id.tvFund)).setText(c.getFormattedFund());
        TextView summary = card.findViewById(R.id.tvFundSummary);
        String s = c.getFundSummary();
        summary.setText(s == null || s.isEmpty() ? "Rincian sumber dana belum dilaporkan." : s);
        card.setContentDescription("Nomor urut " + number + ", " + name);
        return card;
    }

    // ---------------------------------------------------------------- hasil

    static void renderResults(LayoutInflater inflater, ViewGroup container, List<TallyResult> results) {
        container.removeAllViews();
        Context ctx = container.getContext();
        for (int i = 0; i < results.size(); i++) {
            TallyResult r = results.get(i);
            View row = inflater.inflate(R.layout.item_result, container, false);
            ((TextView) row.findViewById(R.id.tvName)).setText(r.getCandidateName());
            ((TextView) row.findViewById(R.id.tvPercent)).setText(
                    String.format(Locale.US, "%.1f%%", r.getPercentage()));
            ((TextView) row.findViewById(R.id.tvVotes)).setText(r.getTotalVotes() + " suara");
            LinearProgressIndicator bar = row.findViewById(R.id.progress);
            bar.setProgress((int) Math.round(r.getPercentage() * 10));
            // Perolehan terbanyak memakai ungu pekat, sisanya ungu redup.
            boolean leader = i == 0 && r.getTotalVotes() > 0;
            bar.setIndicatorColor(ContextCompat.getColor(ctx, leader ? R.color.brand_primary : R.color.on_ink_faint));
            container.addView(row);
        }
    }

    // -------------------------------------------------------------- laporan

    static View reportCard(LayoutInflater inflater, ViewGroup parent, ViolationReport r) {
        View card = inflater.inflate(R.layout.item_report, parent, false);
        ((TextView) card.findViewById(R.id.tvTicket)).setText(r.getTicketCode());
        setBadge(card.findViewById(R.id.tvStatus), r.getStatus().getLabel(), toneOf(r.getStatus()));
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                new Locale("id", "ID")).format(new Date(r.getCreatedAt()));
        ((TextView) card.findViewById(R.id.tvMeta)).setText(r.getReporterDisplay() + ", " + date);
        ((TextView) card.findViewById(R.id.tvDescription)).setText(r.getDescription());
        return card;
    }

    static int dp(Context ctx, int value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }
}
