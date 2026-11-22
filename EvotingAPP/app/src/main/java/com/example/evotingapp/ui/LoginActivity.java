package com.example.evotingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.example.evotingapp.R;
import com.example.evotingapp.repository.ElectionDbHelper;
import com.example.evotingapp.role.User;
import com.example.evotingapp.service.AuthService;
import com.example.evotingapp.service.ElectionException;

public class LoginActivity extends BaseActivity {
    private AuthService authService;
    private EditText inputUsername;
    private EditText inputPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        authService = container().authService();

        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> onLoginClicked());
        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            onLoginClicked();
            return true;
        });
        findViewById(R.id.btnDemoVoter).setOnClickListener(v -> fillDemoAccount("pemilih1"));
        findViewById(R.id.btnDemoPanwas).setOnClickListener(v -> fillDemoAccount("panwas1"));
        findViewById(R.id.btnDemoKpu).setOnClickListener(v -> fillDemoAccount("kpu1"));
    }

    private void fillDemoAccount(String username) {
        inputUsername.setText(username);
        inputPassword.setText(ElectionDbHelper.DEMO_PASSWORD);
    }

    private void onLoginClicked() {
        try {
            User user = authService.login(inputUsername.getText().toString(),
                    inputPassword.getText().toString());
            Session.set(user);
            startActivity(new Intent(this, TransparencyActivity.class));
            finish();
        } catch (ElectionException e) {
            toast(e.getMessage());
        }
    }
}
