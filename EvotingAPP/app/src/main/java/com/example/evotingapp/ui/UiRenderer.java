package com.example.evotingapp.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/** Helper tampilan yang dipakai bersama oleh beberapa layar. */
final class UiRenderer {

    enum Tone { SUCCESS, DANGER, WARNING, NEUTRAL, INFO }

    private UiRenderer() {
    }

    static void setBadge(TextView badge, String text, Tone tone) {
        Context ctx = badge.getContext();
        int fg;
        int bg;
        switch (tone) {
            case SUCCESS:
                fg = R.color.status_success;
                bg = R.color.status_success_bg;
                break;
            case DANGER:
                fg = R.color.status_danger;
                bg = R.color.status_danger_bg;
                break;
            case WARNING:
                fg = R.color.status_warning;
                bg = R.color.status_warning_bg;
                break;
            case INFO:
                fg = R.color.brand_primary;
                bg = R.color.brand_primary_container;
                break;
            default:
                fg = R.color.status_neutral;
                bg = R.color.status_neutral_bg;
        }
        badge.setText(text);
        badge.setTextColor(ContextCompat.getColor(ctx, fg));
        ViewCompat.setBackgroundTintList(badge, ColorStateList.valueOf(ContextCompat.getColor(ctx, bg)));
        badge.setVisibility(View.VISIBLE);
    }

    static void setElectionBadge(TextView badge, ElectionState state) {
        Tone tone;
        switch (state) {
            case OPEN:
                tone = Tone.SUCCESS;
                break;
            case CLOSED:
                tone = Tone.WARNING;
                break;
            case PUBLISHED:
                tone = Tone.INFO;
                break;
            case TAMPERED:
                tone = Tone.DANGER;
                break;
            default:
                tone = Tone.NEUTRAL;
        }
        setBadge(badge, "Status: " + state.getLabel(), tone);
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

    /** Membuat kartu kandidat (tombol "Pilih" disembunyikan secara default). */
    static View candidateCard(LayoutInflater inflater, ViewGroup parent, Candidate c) {
        View card = inflater.inflate(R.layout.item_candidate, parent, false);
        ((TextView) card.findViewById(R.id.tvName)).setText(c.getFullName());
        ((TextView) card.findViewById(R.id.tvVision)).setText(c.getVisionMission());
        ((TextView) card.findViewById(R.id.tvFund)).setText("Dana kampanye: " + c.getFormattedFund());
        TextView summary = card.findViewById(R.id.tvFundSummary);
        String s = c.getFundSummary();
        summary.setText(s == null || s.isEmpty() ? "Rincian belum dilaporkan." : s);
        return card;
    }

    static void renderResults(LayoutInflater inflater, ViewGroup container, List<TallyResult> results) {
        container.removeAllViews();
        for (TallyResult r : results) {
            View row = inflater.inflate(R.layout.item_result, container, false);
            ((TextView) row.findViewById(R.id.tvName)).setText(r.getCandidateName());
            ((TextView) row.findViewById(R.id.tvVotes)).setText(String.format(Locale.US,
                    "%d suara · %.1f%%", r.getTotalVotes(), r.getPercentage()));
            LinearProgressIndicator bar = row.findViewById(R.id.progress);
            bar.setProgress((int) Math.round(r.getPercentage() * 10));
            container.addView(row);
        }
    }

    static View reportCard(LayoutInflater inflater, ViewGroup parent, ViolationReport r) {
        View card = inflater.inflate(R.layout.item_report, parent, false);
        ((TextView) card.findViewById(R.id.tvTicket)).setText(r.getTicketCode());
        setBadge(card.findViewById(R.id.tvStatus), r.getStatus().getLabel(), toneOf(r.getStatus()));
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                new Locale("id", "ID")).format(new Date(r.getCreatedAt()));
        ((TextView) card.findViewById(R.id.tvMeta)).setText("Pelapor: " + r.getReporterDisplay() + " · " + date);
        ((TextView) card.findViewById(R.id.tvDescription)).setText(r.getDescription());
        return card;
    }
}
