package com.example.ticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

        LoginAuthLogic.ValidationError validationError = LoginAuthLogic.validateLoginInputs(email, password);
        if (validationError != null) {
            if (validationError.getField() == LoginAuthLogic.InputField.LOGIN_PASSWORD) {
                loginPassword.setError(validationError.getMessage());
                loginPassword.requestFocus();
            } else {
                loginEmailOrPhone.setError(validationError.getMessage());
                loginEmailOrPhone.requestFocus();
            }
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

        LoginAuthLogic.ValidationError validationError = LoginAuthLogic.validateCreateAccountInputs(
                name, email, phoneNumber, password, createAsAdmin, adminPasscode);
        if (validationError != null) {
            switch (validationError.getField()) {
                case CREATE_NAME:
                    createName.setError(validationError.getMessage());
                    createName.requestFocus();
                    break;
                case CREATE_EMAIL:
                    createEmail.setError(validationError.getMessage());
                    createEmail.requestFocus();
                    break;
                case CREATE_PHONE:
                    createPhoneNumber.setError(validationError.getMessage());
                    createPhoneNumber.requestFocus();
                    break;
                case CREATE_PASSWORD:
                    createPassword.setError(validationError.getMessage());
                    createPassword.requestFocus();
                    break;
                case CREATE_ADMIN_PASSCODE:
                    createAdminPasscode.setError(validationError.getMessage());
                    createAdminPasscode.requestFocus();
                    break;
                default:
                    break;
            }
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
                    String role = snapshot.exists() ? LoginAuthLogic.resolveRole(snapshot.getString("role")) : "USER";

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
        return LoginAuthLogic.getFriendlyAuthError(exception, creatingAccount);
    }

    private void openHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}