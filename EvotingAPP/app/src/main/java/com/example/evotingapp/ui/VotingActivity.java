package com.example.evotingapp.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
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
    private TextView tvHeaderSubtitle;
    private View receiptCard;
    private TextView tvReceipt;
    private String receiptHash;

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
        setupHeader("Bilik suara", "");

        votingService = container().votingService();
        candidateService = container().candidateService();

        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        candidatesContainer = findViewById(R.id.candidatesContainer);
        receiptCard = findViewById(R.id.receiptCard);
        tvReceipt = findViewById(R.id.tvReceipt);

        findViewById(R.id.btnCopyReceipt).setOnClickListener(v -> copyReceipt());
        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());

        render();
    }

    private void setHint(String text) {
        tvHeaderSubtitle.setText(text);
        tvHeaderSubtitle.setVisibility(View.VISIBLE);
    }

    private void render() {
        ElectionState state = container().electionService().getState();
        boolean canVote = state == ElectionState.OPEN && voter.canCastVote();
        if (state != ElectionState.OPEN) {
            setHint("Bilik suara belum bisa dipakai. Status saat ini: " + state.getLabel().toLowerCase() + ".");
        } else if (!voter.canCastVote()) {
            setHint("Anda sudah menggunakan hak pilih. Terima kasih sudah ikut memilih.");
        } else {
            setHint("Coblos satu pasangan calon. Pilihan dienkripsi dan disimpan tanpa identitas Anda.");
        }

        candidatesContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Candidate c : candidateService.getAllCandidates()) {
            View card = UiRenderer.candidateCard(inflater, candidatesContainer, c);
            View choose = card.findViewById(R.id.btnChoose);
            choose.setVisibility(canVote ? View.VISIBLE : View.GONE);
            choose.setContentDescription("Coblos " + c.getFullName());
            choose.setOnClickListener(v -> confirmVote(card, c));
            candidatesContainer.addView(card);
        }
    }

    /** Menandai kartu dengan bekas coblosan, lalu meminta konfirmasi. */
    private void confirmVote(View card, Candidate candidate) {
        View punch = card.findViewById(R.id.punchMark);
        punch.setScaleX(0.4f);
        punch.setScaleY(0.4f);
        punch.setVisibility(View.VISIBLE);
        punch.animate().scaleX(1f).scaleY(1f).setDuration(180)
                .setInterpolator(new DecelerateInterpolator()).start();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Coblos pasangan ini?")
                .setMessage(candidate.getFullName() + "\n\nPilihan tidak bisa diubah setelah suara dikirim.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Kirim suara", (d, w) -> onCandidateSelected(candidate.getCandidateId()))
                .setOnDismissListener(d -> {
                    if (receiptHash == null) {
                        punch.setVisibility(View.GONE);
                    }
                })
                .show();
    }

    private void onCandidateSelected(String candidateId) {
        try {
            receiptHash = votingService.castVote(voter, candidateId);
            showReceipt(receiptHash);
        } catch (ElectionException e) {
            showMessage("Suara tidak terkirim", e.getMessage());
            render();
        }
    }

    private void showReceipt(String hash) {
        tvReceipt.setText(groupHash(hash));
        candidatesContainer.setVisibility(View.GONE);
        setHint("Terima kasih sudah menggunakan hak pilih Anda.");

        // Satu momen gerak: struk muncul dari bawah.
        receiptCard.setAlpha(0f);
        receiptCard.setTranslationY(UiRenderer.dp(this, 48));
        receiptCard.setVisibility(View.VISIBLE);
        receiptCard.animate().alpha(1f).translationY(0f).setDuration(380)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    /** Memecah hash 64 karakter menjadi 4 baris berisi 4 kelompok agar mudah dicocokkan. */
    private static String groupHash(String hash) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length(); i += 4) {
            if (i > 0) {
                sb.append(i % 16 == 0 ? '\n' : ' ');
            }
            sb.append(hash, i, Math.min(i + 4, hash.length()));
        }
        return sb.toString();
    }

    private void copyReceipt() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Receipt Hash", receiptHash));
        toast("Receipt Hash disalin");
    }
}
