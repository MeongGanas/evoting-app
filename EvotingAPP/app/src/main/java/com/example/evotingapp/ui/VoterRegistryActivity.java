package com.example.evotingapp.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.evotingapp.R;
import com.example.evotingapp.model.ElectionState;
import com.example.evotingapp.role.IElectionManager;
import com.example.evotingapp.role.User;
import com.example.evotingapp.role.Voter;
import com.example.evotingapp.service.ElectionException;
import com.example.evotingapp.service.VoterRegistryService;

import java.util.List;

/** Halaman terpisah untuk mengelola Daftar Pemilih Tetap (khusus KPU). */
public class VoterRegistryActivity extends BaseActivity {
    private VoterRegistryService voterRegistryService;

    private EditText inputVoterNik;
    private EditText inputVoterUsername;
    private EditText inputVoterPassword;
    private EditText inputSearch;
    private TextView tvTotalVoters;
    private TextView tvVotedVoters;
    private TextView tvRegistryHint;
    private TextView tvEmptyVoters;
    private LinearLayout votersContainer;
    private View btnRegisterVoter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = requireUser();
        if (user == null) {
            return;
        }
        if (!(user instanceof IElectionManager) || !((IElectionManager) user).canManageElection()) {
            toast("Halaman ini khusus panitia (KPU).");
            finish();
            return;
        }
        setContentView(R.layout.activity_voters);
        setupHeader("Daftar pemilih tetap", "Hanya panitia yang dapat mendaftarkan pemilih.");
        voterRegistryService = container().voterRegistryService();

        inputVoterNik = findViewById(R.id.inputVoterNik);
        inputVoterUsername = findViewById(R.id.inputVoterUsername);
        inputVoterPassword = findViewById(R.id.inputVoterPassword);
        inputSearch = findViewById(R.id.inputSearch);
        tvTotalVoters = findViewById(R.id.tvTotalVoters);
        tvVotedVoters = findViewById(R.id.tvVotedVoters);
        tvRegistryHint = findViewById(R.id.tvRegistryHint);
        tvEmptyVoters = findViewById(R.id.tvEmptyVoters);
        votersContainer = findViewById(R.id.votersContainer);
        btnRegisterVoter = findViewById(R.id.btnRegisterVoter);

        btnRegisterVoter.setOnClickListener(v -> onRegisterVoter());
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                renderVoters();
            }
        });

        ElectionState state = container().electionService().getState();
        if (state != ElectionState.NOT_STARTED && state != ElectionState.OPEN) {
            btnRegisterVoter.setEnabled(false);
            tvRegistryHint.setText("Daftar pemilih sudah dikunci karena pemungutan suara telah ditutup.");
        }
        renderVoters();
    }

    private void onRegisterVoter() {
        try {
            Voter voter = voterRegistryService.registerVoter(
                    inputVoterNik.getText().toString(),
                    inputVoterUsername.getText().toString(),
                    inputVoterPassword.getText().toString());
            inputVoterNik.setText("");
            inputVoterUsername.setText("");
            inputVoterPassword.setText("");
            showMessage("Pemilih terdaftar",
                    "Username: " + voter.getUsername() + "\nNIK: " + maskNik(voter.getNik())
                            + "\n\nBerikan username dan kata sandi awal kepada pemilih.");
            renderVoters();
        } catch (ElectionException e) {
            showMessage("Pemilih belum terdaftar", e.getMessage());
        }
    }

    private void renderVoters() {
        List<Voter> voters = voterRegistryService.getAllVoters();
        String query = inputSearch.getText().toString().trim().toLowerCase();
        int voted = 0;
        int shown = 0;
        votersContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Voter v : voters) {
            boolean hasVoted = !v.canCastVote();
            if (hasVoted) {
                voted++;
            }
            if (!query.isEmpty() && !v.getUsername().contains(query)) {
                continue;
            }
            if (shown > 0) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, UiRenderer.dp(this, 1)));
                divider.setBackgroundResource(R.color.outline_soft);
                votersContainer.addView(divider);
            }
            View row = inflater.inflate(R.layout.item_voter, votersContainer, false);
            ((TextView) row.findViewById(R.id.tvUsername)).setText(v.getUsername());
            ((TextView) row.findViewById(R.id.tvNik)).setText("NIK " + maskNik(v.getNik()));
            UiRenderer.setBadge(row.findViewById(R.id.tvVoteStatus),
                    hasVoted ? "Sudah memilih" : "Belum memilih",
                    hasVoted ? UiRenderer.Tone.SUCCESS : UiRenderer.Tone.NEUTRAL);
            votersContainer.addView(row);
            shown++;
        }
        tvTotalVoters.setText(String.valueOf(voters.size()));
        tvVotedVoters.setText(String.valueOf(voted));
        ((View) votersContainer.getParent()).setVisibility(shown == 0 ? View.GONE : View.VISIBLE);
        tvEmptyVoters.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        tvEmptyVoters.setText(voters.isEmpty()
                ? "Belum ada pemilih. Daftarkan pemilih pertama lewat formulir di atas."
                : "Tidak ada username yang cocok dengan \"" + query + "\".");
    }

    /** Menyamarkan NIK agar data pribadi tidak tampil utuh di layar. */
    static String maskNik(String nik) {
        if (nik == null || nik.length() < 8) {
            return "-";
        }
        return nik.substring(0, 4) + "********" + nik.substring(nik.length() - 4);
    }
}
