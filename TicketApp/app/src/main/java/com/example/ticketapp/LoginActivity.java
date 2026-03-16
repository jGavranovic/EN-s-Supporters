package com.example.ticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class LoginActivity extends AppCompatActivity {

    MaterialButtonToggleGroup toggleButtonGroup;
    Button loginTabButton;
    Button createAccountTabButton;
    Button loginButton;
    Button guestButton;
    Button createAccountButton;
    LinearLayout loginForm;
    LinearLayout createAccountForm;

    EditText loginEmailOrPhone;
    EditText loginPassword;
    EditText createName;
    EditText createEmail;
    EditText createPhoneNumber;
    EditText createPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize toggle group
        toggleButtonGroup = findViewById(R.id.toggleButtonGroup);
        loginTabButton = findViewById(R.id.loginTabButton);
        createAccountTabButton = findViewById(R.id.createAccountTabButton);

        // Initialize login form elements
        loginForm = findViewById(R.id.loginForm);
        loginEmailOrPhone = findViewById(R.id.loginEmailOrPhone);
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
            // change user ROLE here to see different views according to ROLE
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
        String emailOrPhone = loginEmailOrPhone.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (emailOrPhone.isEmpty()) {
            loginEmailOrPhone.setError("Email or phone is required");
            loginEmailOrPhone.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }

        // TODO: authenticate user
        UserSession.getInstance().setUserType(UserSession.UserType.USER);
        openHome();
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
        
        // TODO: Call account creation API

        // After account creation, set user as regular user
        UserSession.getInstance().setUserType(UserSession.UserType.USER);
        openHome();
    }

    private void openHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}