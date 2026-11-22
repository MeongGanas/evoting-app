package com.example.evotingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.evotingapp.R;
import com.example.evotingapp.role.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Kebutuhan umum semua layar: akses container, sesi login, inset layar, header, dan pesan. */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        // Header berwarna ungu tinta: ikon status bar dibuat terang.
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        View root = findViewById(R.id.root);
        View header = findViewById(R.id.header);
        if (root == null) {
            return;
        }
        final int rl = root.getPaddingLeft();
        final int rt = root.getPaddingTop();
        final int rr = root.getPaddingRight();
        final int rb = root.getPaddingBottom();
        final int ht = header == null ? 0 : header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            if (header != null) {
                // Header ungu memanjang sampai di belakang status bar.
                header.setPadding(header.getPaddingLeft(), ht + bars.top,
                        header.getPaddingRight(), header.getPaddingBottom());
                v.setPadding(rl + bars.left, rt, rr + bars.right, rb + bars.bottom);
            } else {
                v.setPadding(rl + bars.left, rt + bars.top, rr + bars.right, rb + bars.bottom);
            }
            return insets;
        });
    }

    /** Mengisi header standar (view_header.xml) pada layar turunan. */
    protected void setupHeader(String title, String subtitle) {
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText(title);
        TextView sub = findViewById(R.id.tvHeaderSubtitle);
        sub.setText(subtitle);
        sub.setVisibility(subtitle == null || subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        View back = findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
    }

    protected AppContainer container() {
        return ((EvotingApp) getApplication()).container();
    }

    /** @return pengguna yang login, atau null (dan kembali ke Login) bila sesi habis. */
    protected User requireUser() {
        User user = Session.get();
        if (user == null) {
            goToLogin();
        }
        return user;
    }

    protected void goToLogin() {
        Session.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showMessage(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Mengerti", null)
                .show();
    }
}
