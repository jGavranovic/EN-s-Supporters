package com.example.ticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketapp.auth.AuthApi;
import com.example.ticketapp.auth.AuthService;
public class LoginActivity extends AppCompatActivity {

    private Button loginButton;
    private Button guestButton;
    private Button createAccountButton;
    private LinearLayout loginForm;
    private LinearLayout createAccountForm;

    private EditText loginIdentifier;
    private EditText loginPassword;
    private EditText createName;
    private EditText createEmail;
    private EditText createPhoneNumber;
    private EditText createPassword;

    private AuthApi authApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authApi = AuthService.getAuthApi();

        // Initialize toggle group
        com.google.android.material.button.MaterialButtonToggleGroup toggleButtonGroup = findViewById(R.id.toggleButtonGroup);

        // Initialize login form elements
        loginForm = findViewById(R.id.loginForm);
        loginIdentifier = findViewById(R.id.loginIdentifier);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        guestButton = findViewById(R.id.guestButton);

        // Initialize create account form elements
        createAccountForm = findViewById(R.id.createAccountForm);
        createName = findViewById(R.id.createName);
        createEmail = findViewById(R.id.createEmail);
        createPhoneNumber = findViewById(R.id.createPhoneNumber);
        createPassword = findViewById(R.id.createPassword);
        createAccountButton = findViewById(R.id.createAccountButton);

        // Set up toggle group listener
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.loginTabButton) {
                    showLoginForm();
                } else if (checkedId == R.id.createAccountTabButton) {
                    showCreateAccountForm();
                }
            }
        });

        toggleButtonGroup.check(R.id.loginTabButton);

        loginButton.setOnClickListener(v -> handleLogin());
        guestButton.setOnClickListener(v -> {
            // Keep existing behavior: guest mode currently maps to USER role in this project.
            UserSession.getInstance().logout();
            UserSession.getInstance().setUserType(UserSession.UserType.USER);
            openHome();
        });
        createAccountButton.setOnClickListener(v -> handleCreateAccount());
    }

    private void showLoginForm() {
        loginForm.setVisibility(View.VISIBLE);
        createAccountForm.setVisibility(View.GONE);
    }

    private void showCreateAccountForm() {
        loginForm.setVisibility(View.GONE);
        createAccountForm.setVisibility(View.VISIBLE);
    }

    private void handleLogin() {
        String emailOrPhone = loginIdentifier.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (emailOrPhone.isEmpty()) {
            loginIdentifier.setError("Email or phone is required");
            loginIdentifier.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }

        setAuthButtonsEnabled(false);
        authApi.login(emailOrPhone, password, response -> runOnUiThread(() -> {
            setAuthButtonsEnabled(true);
            if (response.succeeded()) {
                UserSession.getInstance().setAuthenticatedUser(response.getUser(), response.getJwtToken());
                openHome();
                return;
            }

            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
            if (response.getMessage().toLowerCase().contains("password")) {
                loginPassword.setError(response.getMessage());
                loginPassword.requestFocus();
            } else {
                loginIdentifier.setError(response.getMessage());
                loginIdentifier.requestFocus();
            }
        }));
    }

    private void handleCreateAccount() {
        String name = createName.getText().toString().trim();
        String email = createEmail.getText().toString().trim();
        String phoneNumber = createPhoneNumber.getText().toString().trim();
        String password = createPassword.getText().toString().trim();

        if (name.isEmpty()) {
            createName.setError("Name is required");
            createName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            createEmail.setError("Email is required");
            createEmail.requestFocus();
            return;
        }
        if (phoneNumber.isEmpty()) {
            createPhoneNumber.setError("Phone number is required");
            createPhoneNumber.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            createPassword.setError("Password is required");
            createPassword.requestFocus();
            return;
        }

        setAuthButtonsEnabled(false);
        authApi.register(name, email, phoneNumber, password, response -> runOnUiThread(() -> {
            setAuthButtonsEnabled(true);
            if (response.succeeded()) {
                UserSession.getInstance().setAuthenticatedUser(response.getUser(), response.getJwtToken());
                openHome();
                return;
            }

            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
            applyCreateAccountError(response.getMessage());
        }));
    }

    private void setAuthButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        guestButton.setEnabled(enabled);
        createAccountButton.setEnabled(enabled);
    }

    private void applyCreateAccountError(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("name")) {
            createName.setError(message);
            createName.requestFocus();
        } else if (lowerMessage.contains("email")) {
            createEmail.setError(message);
            createEmail.requestFocus();
        } else if (lowerMessage.contains("phone")) {
            createPhoneNumber.setError(message);
            createPhoneNumber.requestFocus();
        } else if (lowerMessage.contains("password")) {
            createPassword.setError(message);
            createPassword.requestFocus();
        }
    }

    private void openHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}