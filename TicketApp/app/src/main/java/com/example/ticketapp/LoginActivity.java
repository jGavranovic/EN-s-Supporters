package com.example.ticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

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

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

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
            UserSession session = UserSession.getInstance();
            session.setUserType(UserSession.UserType.GUEST);
            session.setIdentity("", "EMAIL");
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
        String email = loginEmailOrPhone.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (email.isEmpty()) {
            loginEmailOrPhone.setError("Email is required");
            loginEmailOrPhone.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmailOrPhone.setError("Enter a valid email");
            loginEmailOrPhone.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }

        setAuthUiEnabled(false);
        signInWithPassword(email, password);
    }

    private void signInWithPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setAuthUiEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            applyAuthenticatedSession(user.getEmail());
                            openHome();
                            return;
                        }

                        loginPassword.setError("Login failed, please try again");
                        Toast.makeText(LoginActivity.this, "Login failed, please try again", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String errorMessage = getFriendlyAuthError(task.getException(), false);
                    loginPassword.setError(errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
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
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            createEmail.setError("Enter a valid email");
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

        setAuthUiEnabled(false);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setAuthUiEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest updates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(updates);

                            if (user.getEmail() != null) {
                                applyAuthenticatedSession(user.getEmail());
                                openHome();
                                return;
                            }
                        }

                        createPassword.setError("Account created but login state is unavailable");
                        Toast.makeText(LoginActivity.this, "Please try logging in again", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String errorMessage = getFriendlyAuthError(task.getException(), true);
                    createPassword.setError(errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void applyAuthenticatedSession(String email) {
        UserSession session = UserSession.getInstance();
        session.setUserType(UserSession.UserType.USER);
        session.setIdentity(email, "EMAIL");
    }

    private void setAuthUiEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        guestButton.setEnabled(enabled);
        createAccountButton.setEnabled(enabled);
        loginTabButton.setEnabled(enabled);
        createAccountTabButton.setEnabled(enabled);
    }

    private String getFriendlyAuthError(Exception exception, boolean creatingAccount) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "An account with this email already exists";
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return creatingAccount ? "Please use a valid email and password" : "Email or password is incorrect";
        }
        if (exception instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) exception).getErrorCode();
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                return "Password must be at least 6 characters";
            }
            if ("ERROR_USER_NOT_FOUND".equals(code)) {
                return "No account found for this email";
            }
            if ("ERROR_WRONG_PASSWORD".equals(code)) {
                return "Email or password is incorrect";
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "Please enter a valid email";
            }
        }
        return creatingAccount ? "Could not create account right now" : "Could not log in right now";
    }

    private void openHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}