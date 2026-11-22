package com.example.evotingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

/** Kebutuhan umum semua layar: akses container, sesi login, inset layar, dan pesan. */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        View root = findViewById(R.id.root);
        if (root != null) {
            final int l = root.getPaddingLeft();
            final int t = root.getPaddingTop();
            final int r = root.getPaddingRight();
            final int b = root.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                        | WindowInsetsCompat.Type.ime());
                v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom);
                return insets;
            });
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
                .setPositiveButton("OK", null)
                .show();
    }
}
