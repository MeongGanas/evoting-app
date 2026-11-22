package com.example.evotingapp.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.evotingapp.R;
import com.example.evotingapp.model.Candidate;
import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.role.User;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.service.CandidateService;
import com.example.evotingapp.service.ElectionException;
import com.example.evotingapp.service.VotingService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Bilik suara & struk digital (khusus Pemilih). */
public class VotingActivity extends BaseActivity {
    private VotingService votingService;
    private CandidateService candidateService;
    private Voter voter;

    private LinearLayout candidatesContainer;
    private TextView tvVotingInfo;
    private View receiptCard;
    private TextView tvReceipt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = requireUser();
        if (user == null) {
            return;
        }
        if (!(user instanceof Voter)) {
            toast("Bilik suara hanya untuk pemilih.");
            finish();
            return;
        }
        voter = (Voter) user;
        setContentView(R.layout.activity_voting);

        votingService = container().votingService();
        candidateService = container().candidateService();

        candidatesContainer = findViewById(R.id.candidatesContainer);
        tvVotingInfo = findViewById(R.id.tvVotingInfo);
        receiptCard = findViewById(R.id.receiptCard);
        tvReceipt = findViewById(R.id.tvReceipt);

        findViewById(R.id.btnCopyReceipt).setOnClickListener(v -> copyReceipt());
        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());

        render();
    }

    private void render() {
        ElectionState state = container().electionService().getState();
        boolean canVote = state == ElectionState.OPEN && voter.canCastVote();
        if (state != ElectionState.OPEN) {
            tvVotingInfo.setText("Pemungutan suara belum/tidak dibuka (" + state.getLabel() + ").");
        } else if (!voter.canCastVote()) {
            tvVotingInfo.setText("Anda sudah menggunakan hak pilih. Terima kasih telah berpartisipasi.");
        } else {
            tvVotingInfo.setText("Pilih satu pasangan. Suara dienkripsi AES dan tidak disimpan bersama identitas Anda.");
        }

        candidatesContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Candidate c : candidateService.getAllCandidates()) {
            View card = UiRenderer.candidateCard(inflater, candidatesContainer, c);
            View choose = card.findViewById(R.id.btnChoose);
            choose.setVisibility(canVote ? View.VISIBLE : View.GONE);
            choose.setOnClickListener(v -> confirmVote(c));
            candidatesContainer.addView(card);
        }
    }

    private void confirmVote(Candidate candidate) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Konfirmasi Pilihan")
                .setMessage("Anda memilih:\n\n" + candidate.getFullName()
                        + "\n\nSuara tidak dapat diubah setelah dikirim.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Kirim Suara", (d, w) -> onCandidateSelected(candidate.getCandidateId()))
                .show();
    }

    private void onCandidateSelected(String candidateId) {
        try {
            String receipt = votingService.castVote(voter, candidateId);
            showReceipt(receipt);
        } catch (ElectionException e) {
            showMessage("Suara tidak terkirim", e.getMessage());
            render();
        }
    }

    private void showReceipt(String receiptHash) {
        tvReceipt.setText(receiptHash);
        receiptCard.setVisibility(View.VISIBLE);
        candidatesContainer.setVisibility(View.GONE);
        tvVotingInfo.setText("Terima kasih telah menggunakan hak pilih Anda.");
    }

    private void copyReceipt() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Receipt Hash", tvReceipt.getText()));
        toast("Receipt Hash disalin.");
    }
}
