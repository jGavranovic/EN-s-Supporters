package com.example.ticketapp.auth;

import com.example.ticketapp.UserSession;
import com.example.ticketapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirebaseAuthApi implements AuthApi {

    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public FirebaseAuthApi() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseAuthApi(FirebaseAuth firebaseAuth, FirebaseFirestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    @Override
    public void register(String fullName, String email, String phoneNumber, String password, AuthCallback callback) {
        String trimmedName = safeTrim(fullName);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phoneNumber);
        String safePassword = safeTrim(password);

        if (trimmedName.isEmpty()) {
            callback.onComplete(AuthResponse.failure("Name is required"));
            return;
        }
        if (!AuthValidator.isValidEmail(normalizedEmail)) {
            callback.onComplete(AuthResponse.failure("Please enter a valid email address"));
            return;
        }
        if (!AuthValidator.isValidPhoneNumber(normalizedPhone)) {
            callback.onComplete(AuthResponse.failure("Please enter a valid phone number"));
            return;
        }
        if (!AuthValidator.isValidPassword(safePassword)) {
            callback.onComplete(AuthResponse.failure("Password must be at least 8 characters"));
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("phoneNumber", normalizedPhone)
                .limit(1)
                .get()
                .addOnCompleteListener(phoneLookup -> {
                    if (!phoneLookup.isSuccessful()) {
                        callback.onComplete(AuthResponse.failure("Unable to verify phone number right now"));
                        return;
                    }
                    if (phoneLookup.getResult() != null && !phoneLookup.getResult().isEmpty()) {
                        callback.onComplete(AuthResponse.failure("An account already exists with this phone number"));
                        return;
                    }

                    firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, safePassword)
                            .addOnCompleteListener(createTask -> {
                                if (!createTask.isSuccessful()) {
                                    callback.onComplete(AuthResponse.failure(FirebaseAuthErrorMapper.mapCreateError(createTask.getException())));
                                    return;
                                }

                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                if (firebaseUser == null) {
                                    callback.onComplete(AuthResponse.failure("Registration succeeded, but user session is unavailable"));
                                    return;
                                }

                                Map<String, Object> profile = new HashMap<>();
                                profile.put("fullName", trimmedName);
                                profile.put("email", normalizedEmail);
                                profile.put("phoneNumber", normalizedPhone);
                                profile.put("userType", UserSession.UserType.USER.name());

                                firestore.collection(USERS_COLLECTION)
                                        .document(firebaseUser.getUid())
                                        .set(profile)
                                        .addOnCompleteListener(saveProfileTask -> {
                                            if (!saveProfileTask.isSuccessful()) {
                                                callback.onComplete(AuthResponse.failure("Account created, but profile save failed"));
                                                return;
                                            }

                                            buildAuthResponse(firebaseUser, trimmedName, normalizedEmail, normalizedPhone, UserSession.UserType.USER, "Account created", callback);
                                        });
                            });
                });
    }

    @Override
    public void login(String emailOrPhone, String password, AuthCallback callback) {
        String identifier = safeTrim(emailOrPhone);
        String safePassword = safeTrim(password);

        if (identifier.isEmpty()) {
            callback.onComplete(AuthResponse.failure("Email or phone is required"));
            return;
        }
        if (safePassword.isEmpty()) {
            callback.onComplete(AuthResponse.failure("Password is required"));
            return;
        }

        if (AuthValidator.isValidEmail(identifier)) {
            signInWithEmail(normalizeEmail(identifier), safePassword, callback);
            return;
        }

        String normalizedPhone = normalizePhone(identifier);
        if (!AuthValidator.isValidPhoneNumber(normalizedPhone)) {
            callback.onComplete(AuthResponse.failure("Please enter a valid email or phone number"));
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("phoneNumber", normalizedPhone)
                .limit(1)
                .get()
                .addOnCompleteListener(phoneLookup -> {
                    if (!phoneLookup.isSuccessful()) {
                        callback.onComplete(AuthResponse.failure("Unable to lookup account right now"));
                        return;
                    }
                    if (phoneLookup.getResult() == null || phoneLookup.getResult().isEmpty()) {
                        callback.onComplete(AuthResponse.failure("No account found for the provided credentials"));
                        return;
                    }

                    String mappedEmail = phoneLookup.getResult().getDocuments().get(0).getString("email");
                    if (mappedEmail == null || mappedEmail.trim().isEmpty()) {
                        callback.onComplete(AuthResponse.failure("Account is missing email mapping"));
                        return;
                    }

                    signInWithEmail(normalizeEmail(mappedEmail), safePassword, callback);
                });
    }

    @Override
    public void logout() {
        firebaseAuth.signOut();
    }

    private void signInWithEmail(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(signInTask -> {
                    if (!signInTask.isSuccessful()) {
                        callback.onComplete(AuthResponse.failure(FirebaseAuthErrorMapper.mapLoginError(signInTask.getException())));
                        return;
                    }

                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onComplete(AuthResponse.failure("Login succeeded, but user session is unavailable"));
                        return;
                    }

                    firestore.collection(USERS_COLLECTION)
                            .document(firebaseUser.getUid())
                            .get()
                            .addOnCompleteListener(profileTask -> {
                                if (!profileTask.isSuccessful()) {
                                    // Do not block login if Firestore profile is missing/unreachable.
                                    buildAuthResponse(firebaseUser, "", safeValue(firebaseUser.getEmail()), "", UserSession.UserType.USER, "Login successful", callback);
                                    return;
                                }

                                DocumentSnapshot profileDoc = profileTask.getResult();
                                String fullName = profileDoc != null ? safeValue(profileDoc.getString("fullName")) : "";
                                String profileEmail = profileDoc != null ? safeValue(profileDoc.getString("email")) : safeValue(firebaseUser.getEmail());
                                String phoneNumber = profileDoc != null ? safeValue(profileDoc.getString("phoneNumber")) : "";
                                UserSession.UserType userType = parseUserType(profileDoc != null ? profileDoc.getString("userType") : null);

                                buildAuthResponse(firebaseUser, fullName, profileEmail, phoneNumber, userType, "Login successful", callback);
                            });
                });
    }

    private void buildAuthResponse(
            FirebaseUser firebaseUser,
            String fullName,
            String email,
            String phoneNumber,
            UserSession.UserType userType,
            String successMessage,
            AuthCallback callback
    ) {
        firebaseUser.getIdToken(false)
                .addOnCompleteListener(tokenTask -> {
                    String token = null;
                    if (tokenTask.isSuccessful()) {
                        GetTokenResult tokenResult = tokenTask.getResult();
                        token = tokenResult != null ? tokenResult.getToken() : null;
                    }

                    User user = new User(
                            firebaseUser.getUid(),
                            fullName,
                            normalizeEmail(email),
                            normalizePhone(phoneNumber),
                            userType
                    );
                    callback.onComplete(AuthResponse.success(successMessage, user, token));
                });
    }

    private String normalizeEmail(String email) {
        return safeTrim(email).toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phoneNumber) {
        return safeTrim(phoneNumber).replaceAll("\\s+", "");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private UserSession.UserType parseUserType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UserSession.UserType.USER;
        }
        try {
            return UserSession.UserType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UserSession.UserType.USER;
        }
    }
}


