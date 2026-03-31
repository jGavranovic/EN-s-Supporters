package com.example.ticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String ADMIN_PASSCODE = "ADMIN2026";

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
    CheckBox createAdminCheckbox;
    EditText createAdminPasscode;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

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
        createAdminCheckbox = findViewById(R.id.createAdminCheckbox);
        createAdminPasscode = findViewById(R.id.createAdminPasscode);
        createAccountButton = findViewById(R.id.createAccountButton);

        createAdminCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            createAdminPasscode.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                createAdminPasscode.setText("");
                createAdminPasscode.setError(null);
            }
        });

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
                            loadRoleAndOpenHome(user);
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
        boolean createAsAdmin = createAdminCheckbox.isChecked();
        String adminPasscode = createAdminPasscode.getText().toString().trim();

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
        if (createAsAdmin && !ADMIN_PASSCODE.equals(adminPasscode)) {
            createAdminPasscode.setError("Invalid admin passcode");
            createAdminPasscode.requestFocus();
            return;
        }

        setAuthUiEnabled(false);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest updates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(updates);

                            if (user.getEmail() != null) {
                                String role = createAsAdmin ? "ADMIN" : "USER";
                                saveUserProfile(user.getUid(), name, user.getEmail(), phoneNumber, role, success -> {
                                    setAuthUiEnabled(true);
                                    if (success) {
                                        applyAuthenticatedSession(user.getEmail(), role);
                                        openHome();
                                        return;
                                    }

                                    createPassword.setError("Account was created, but role setup failed");
                                    Toast.makeText(LoginActivity.this, "Please log in again", Toast.LENGTH_LONG).show();
                                });
                                return;
                            }
                        }

                        setAuthUiEnabled(true);
                        createPassword.setError("Account created but login state is unavailable");
                        Toast.makeText(LoginActivity.this, "Please try logging in again", Toast.LENGTH_LONG).show();
                        return;
                    }

                    setAuthUiEnabled(true);
                    String errorMessage = getFriendlyAuthError(task.getException(), true);
                    createPassword.setError(errorMessage);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void loadRoleAndOpenHome(FirebaseUser user) {
        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = "USER";
                    if (snapshot.exists()) {
                        String storedRole = snapshot.getString("role");
                        if (storedRole != null && storedRole.trim().equalsIgnoreCase("ADMIN")) {
                            role = "ADMIN";
                        }
                    }

                    applyAuthenticatedSession(user.getEmail(), role);
                    openHome();
                })
                .addOnFailureListener(e -> {
                    applyAuthenticatedSession(user.getEmail(), "USER");
                    openHome();
                });
    }

    private interface ProfileSaveCallback {
        void onComplete(boolean success);
    }

    private void saveUserProfile(String uid, String name, String email, String phoneNumber, String role, ProfileSaveCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phoneNumber", phoneNumber);
        userData.put("role", role);
        userData.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    private void applyAuthenticatedSession(String email, String role) {
        UserSession session = UserSession.getInstance();
        if (role != null && role.equalsIgnoreCase("ADMIN")) {
            session.setUserType(UserSession.UserType.ADMIN);
        } else {
            session.setUserType(UserSession.UserType.USER);
        }
        session.setIdentity(email, "EMAIL");
    }

    private void setAuthUiEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        guestButton.setEnabled(enabled);
        createAccountButton.setEnabled(enabled);
        loginTabButton.setEnabled(enabled);
        createAccountTabButton.setEnabled(enabled);
        createAdminCheckbox.setEnabled(enabled);
        createAdminPasscode.setEnabled(enabled);
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