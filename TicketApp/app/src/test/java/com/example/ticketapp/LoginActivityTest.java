package com.example.ticketapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LoginActivityTest {

    @After
    public void tearDown() {
        TestSessionUtils.resetUserSessionSingleton();
    }

    @Test
    public void onCreate_showsLoginFormByDefault() {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            LinearLayout loginForm = activity.findViewById(R.id.loginForm);
            LinearLayout createForm = activity.findViewById(R.id.createAccountForm);

            assertEquals(View.VISIBLE, loginForm.getVisibility());
            assertEquals(View.GONE, createForm.getVisibility());
        }
    }

    @Test
    public void createAccountToggle_switchesVisibleForm() {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            activity.<Button>findViewById(R.id.createAccountTabButton).performClick();

            assertEquals(View.GONE, activity.<LinearLayout>findViewById(R.id.loginForm).getVisibility());
            assertEquals(View.VISIBLE, activity.<LinearLayout>findViewById(R.id.createAccountForm).getVisibility());
        }
    }

    @Test
    public void adminCheckbox_togglesAndClearsPasscodeField() {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            CheckBox adminCheck = activity.findViewById(R.id.createAdminCheckbox);
            EditText passcode = activity.findViewById(R.id.createAdminPasscode);

            adminCheck.setChecked(true);
            assertEquals(View.VISIBLE, passcode.getVisibility());

            passcode.setText("wrong-pass");
            passcode.setError("Invalid admin passcode");
            adminCheck.setChecked(false);

            assertEquals(View.GONE, passcode.getVisibility());
            assertEquals("", passcode.getText().toString());
            assertNull(passcode.getError());
        }
    }

    @Test
    public void guestButton_setsGuestSessionAndNavigatesToMain() {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            activity.<Button>findViewById(R.id.guestButton).performClick();

            UserSession session = UserSession.getInstance();
            assertTrue(session.isGuest());
            assertEquals("", session.getContactDestination());
            assertEquals("EMAIL", session.getPreferredConfirmationChannel());

            Intent startedIntent = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(MainActivity.class.getName(), startedIntent.getComponent().getClassName());
        }
    }

    @Test
    public void handleLogin_allBranches_validationAndSignInPath() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            FirebaseAuth auth = mock(FirebaseAuth.class);
            @SuppressWarnings("unchecked")
            Task<AuthResult> task = mock(Task.class);
            when(auth.signInWithEmailAndPassword("user@example.com", "secret123")).thenReturn(task);
            when(task.addOnCompleteListener(any(LoginActivity.class), any())).thenReturn(task);

            authMock.when(FirebaseAuth::getInstance).thenReturn(auth);
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            EditText email = activity.findViewById(R.id.loginEmailOrPhone);
            EditText password = activity.findViewById(R.id.loginPassword);

            email.setText(" ");
            password.setText("secret123");
            invokePrivate(activity, "handleLogin");
            assertEquals("Email is required", email.getError().toString());

            email.setText("bad");
            password.setText("secret123");
            invokePrivate(activity, "handleLogin");
            assertEquals("Enter a valid email", email.getError().toString());

            email.setText("user@example.com");
            password.setText(" ");
            invokePrivate(activity, "handleLogin");
            assertEquals("Password is required", password.getError().toString());

            email.setText(" user@example.com ");
            password.setText(" secret123 ");
            invokePrivate(activity, "handleLogin");
            verify(auth).signInWithEmailAndPassword("user@example.com", "secret123");
            assertAuthUiEnabled(activity, false);
        }
    }

    @Test
    public void handleCreateAccount_allBranches_validationAndCreatePath() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            FirebaseAuth auth = mock(FirebaseAuth.class);
            @SuppressWarnings("unchecked")
            Task<AuthResult> task = mock(Task.class);
            when(auth.createUserWithEmailAndPassword("alex@example.com", "secret123")).thenReturn(task);
            when(task.addOnCompleteListener(any(LoginActivity.class), any())).thenReturn(task);

            authMock.when(FirebaseAuth::getInstance).thenReturn(auth);
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            EditText name = activity.findViewById(R.id.createName);
            EditText email = activity.findViewById(R.id.createEmail);
            EditText phone = activity.findViewById(R.id.createPhoneNumber);
            EditText password = activity.findViewById(R.id.createPassword);
            CheckBox admin = activity.findViewById(R.id.createAdminCheckbox);
            EditText passcode = activity.findViewById(R.id.createAdminPasscode);

            name.setText(" ");
            email.setText("alex@example.com");
            phone.setText("5551231234");
            password.setText("secret123");
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Name is required", name.getError().toString());

            name.setText("Alex");
            email.setText(" ");
            phone.setText("5551231234");
            password.setText("secret123");
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Email is required", email.getError().toString());

            name.setText("Alex");
            email.setText("bad");
            phone.setText("5551231234");
            password.setText("secret123");
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Enter a valid email", email.getError().toString());

            name.setText("Alex");
            email.setText("alex@example.com");
            phone.setText(" ");
            password.setText("secret123");
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Phone number is required", phone.getError().toString());

            name.setText("Alex");
            email.setText("alex@example.com");
            phone.setText("5551231234");
            password.setText(" ");
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Password is required", password.getError().toString());

            name.setText("Alex");
            email.setText("alex@example.com");
            phone.setText("5551231234");
            password.setText("secret123");
            admin.setChecked(true);
            passcode.setText("WRONG");
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Invalid admin passcode", passcode.getError().toString());

            admin.setChecked(false);
            name.setText("Alex");
            email.setText(" alex@example.com ");
            phone.setText("5551231234");
            password.setText(" secret123 ");
            invokePrivate(activity, "handleCreateAccount");
            verify(auth).createUserWithEmailAndPassword("alex@example.com", "secret123");
            assertAuthUiEnabled(activity, false);
        }
    }

    @Test
    public void saveUserProfile_successAndFailureBranches() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference users = mock(CollectionReference.class);
            DocumentReference doc = mock(DocumentReference.class);
            @SuppressWarnings("unchecked")
            Task<Void> setTask = mock(Task.class);

            when(firestore.collection("users")).thenReturn(users);
            when(users.document("uid-1")).thenReturn(doc);
            when(doc.set(anyMap())).thenReturn(setTask);

            when(setTask.addOnSuccessListener(org.mockito.ArgumentMatchers.<OnSuccessListener<Void>>any())).thenAnswer(invocation -> {
                OnSuccessListener<Void> listener = invocation.getArgument(0);
                if (listener != null) {
                    listener.onSuccess(null);
                }
                return setTask;
            });
            when(setTask.addOnFailureListener(org.mockito.ArgumentMatchers.<OnFailureListener>any())).thenReturn(setTask);

            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            assertEquals(Boolean.TRUE, invokeSaveUserProfile(activity, "uid-1", "Alex", "alex@example.com", "5551231234", "USER"));

            when(setTask.addOnSuccessListener(org.mockito.ArgumentMatchers.<OnSuccessListener<Void>>any())).thenReturn(setTask);
            when(setTask.addOnFailureListener(org.mockito.ArgumentMatchers.<OnFailureListener>any())).thenAnswer(invocation -> {
                OnFailureListener listener = invocation.getArgument(0);
                if (listener != null) {
                    listener.onFailure(new RuntimeException("failure"));
                }
                return setTask;
            });

            assertEquals(Boolean.FALSE, invokeSaveUserProfile(activity, "uid-1", "Alex", "alex@example.com", "5551231234", "ADMIN"));
        }
    }

    @Test
    public void setAuthUiEnabled_togglesAllControls() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            invokePrivate(activity, "setAuthUiEnabled", new Class[]{boolean.class}, new Object[]{false});
            assertAuthUiEnabled(activity, false);

            invokePrivate(activity, "setAuthUiEnabled", new Class[]{boolean.class}, new Object[]{true});
            assertAuthUiEnabled(activity, true);
        }
    }

    @Test
    public void signInWithPassword_handlesSuccessMissingUserAndFailure() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            FirebaseAuth auth = mock(FirebaseAuth.class);
            @SuppressWarnings("unchecked")
            Task<AuthResult> signInTask = mock(Task.class);

            when(auth.signInWithEmailAndPassword("user@example.com", "secret123")).thenReturn(signInTask);
            when(signInTask.addOnCompleteListener(any(LoginActivity.class), any())).thenAnswer(invocation -> {
                OnCompleteListener<AuthResult> listener = invocation.getArgument(1);
                if (listener != null) {
                    listener.onComplete(signInTask);
                }
                return signInTask;
            });

            authMock.when(FirebaseAuth::getInstance).thenReturn(auth);
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();

            when(signInTask.isSuccessful()).thenReturn(true);
            when(auth.getCurrentUser()).thenReturn(null);
            invokePrivate(activity, "signInWithPassword", new Class[]{String.class, String.class}, new Object[]{"user@example.com", "secret123"});
            assertEquals("Login failed, please try again", activity.<EditText>findViewById(R.id.loginPassword).getError().toString());

            when(signInTask.isSuccessful()).thenReturn(false);
            when(signInTask.getException()).thenReturn(new RuntimeException("x"));
            invokePrivate(activity, "signInWithPassword", new Class[]{String.class, String.class}, new Object[]{"user@example.com", "secret123"});
            assertEquals("Could not log in right now", activity.<EditText>findViewById(R.id.loginPassword).getError().toString());
        }
    }

    @Test
    public void loadRoleAndOpenHome_successAndFailurePaths() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference users = mock(CollectionReference.class);
            DocumentReference userDoc = mock(DocumentReference.class);
            @SuppressWarnings("unchecked")
            Task<DocumentSnapshot> getTask = mock(Task.class);
            DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
            FirebaseUser user = mock(FirebaseUser.class);

            when(firestore.collection("users")).thenReturn(users);
            when(users.document("uid-1")).thenReturn(userDoc);
            when(userDoc.get()).thenReturn(getTask);
            when(user.getUid()).thenReturn("uid-1");
            when(user.getEmail()).thenReturn("admin@example.com");

            when(getTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
                OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
                if (listener != null) {
                    listener.onSuccess(snapshot);
                }
                return getTask;
            });
            when(getTask.addOnFailureListener(any())).thenReturn(getTask);

            when(snapshot.exists()).thenReturn(true);
            when(snapshot.getString("role")).thenReturn("ADMIN");

            authMock.when(FirebaseAuth::getInstance).thenReturn(mock(FirebaseAuth.class));
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            invokePrivate(activity, "loadRoleAndOpenHome", new Class[]{FirebaseUser.class}, new Object[]{user});
            assertTrue(UserSession.getInstance().isAdmin());
            assertNotNull(Shadows.shadowOf(activity).getNextStartedActivity());

            UserSession.getInstance().logout();
            when(getTask.addOnSuccessListener(any())).thenReturn(getTask);
            when(getTask.addOnFailureListener(any())).thenAnswer(invocation -> {
                OnFailureListener listener = invocation.getArgument(0);
                if (listener != null) {
                    listener.onFailure(new RuntimeException("offline"));
                }
                return getTask;
            });

            invokePrivate(activity, "loadRoleAndOpenHome", new Class[]{FirebaseUser.class}, new Object[]{user});
            assertTrue(UserSession.getInstance().isUser());
        }
    }

    @Test
    public void handleCreateAccount_asyncBranches_successFailureAndUnavailableUser() throws Exception {
        try (MockedStatic<FirebaseAuth> authMock = org.mockito.Mockito.mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> firestoreMock = org.mockito.Mockito.mockStatic(FirebaseFirestore.class)) {
            FirebaseAuth auth = mock(FirebaseAuth.class);
            FirebaseUser user = mock(FirebaseUser.class);
            FirebaseFirestore firestore = mock(FirebaseFirestore.class);
            CollectionReference users = mock(CollectionReference.class);
            DocumentReference doc = mock(DocumentReference.class);

            @SuppressWarnings("unchecked")
            Task<AuthResult> createTask = mock(Task.class);
            @SuppressWarnings("unchecked")
            Task<Void> setTask = mock(Task.class);
            @SuppressWarnings("unchecked")
            Task<Void> profileTask = mock(Task.class);

            when(auth.createUserWithEmailAndPassword("alex@example.com", "secret123")).thenReturn(createTask);
            when(createTask.addOnCompleteListener(any(LoginActivity.class), any())).thenAnswer(invocation -> {
                OnCompleteListener<AuthResult> listener = invocation.getArgument(1);
                if (listener != null) {
                    listener.onComplete(createTask);
                }
                return createTask;
            });

            when(firestore.collection("users")).thenReturn(users);
            when(users.document("uid-1")).thenReturn(doc);
            when(doc.set(anyMap())).thenReturn(setTask);
            when(setTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
                OnSuccessListener<Void> listener = invocation.getArgument(0);
                if (listener != null) {
                    listener.onSuccess(null);
                }
                return setTask;
            });
            when(setTask.addOnFailureListener(any())).thenReturn(setTask);

            when(auth.getCurrentUser()).thenReturn(user);
            when(user.getUid()).thenReturn("uid-1");
            when(user.getEmail()).thenReturn("alex@example.com");
            when(user.updateProfile(any(UserProfileChangeRequest.class))).thenReturn(profileTask);

            authMock.when(FirebaseAuth::getInstance).thenReturn(auth);
            firestoreMock.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            activity.<Button>findViewById(R.id.createAccountTabButton).performClick();
            activity.<EditText>findViewById(R.id.createName).setText("Alex");
            activity.<EditText>findViewById(R.id.createEmail).setText("alex@example.com");
            activity.<EditText>findViewById(R.id.createPhoneNumber).setText("5551231234");
            activity.<EditText>findViewById(R.id.createPassword).setText("secret123");

            when(createTask.isSuccessful()).thenReturn(true);
            invokePrivate(activity, "handleCreateAccount");
            assertTrue(UserSession.getInstance().isUser());

            when(setTask.addOnSuccessListener(any())).thenReturn(setTask);
            when(setTask.addOnFailureListener(any())).thenAnswer(invocation -> {
                OnFailureListener listener = invocation.getArgument(0);
                if (listener != null) {
                    listener.onFailure(new RuntimeException("write failed"));
                }
                return setTask;
            });
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Account was created, but role setup failed", activity.<EditText>findViewById(R.id.createPassword).getError().toString());

            when(createTask.isSuccessful()).thenReturn(false);
            when(createTask.getException()).thenReturn(new RuntimeException("failed"));
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Could not create account right now", activity.<EditText>findViewById(R.id.createPassword).getError().toString());

            when(createTask.isSuccessful()).thenReturn(true);
            when(auth.getCurrentUser()).thenReturn(null);
            invokePrivate(activity, "handleCreateAccount");
            assertEquals("Account created but login state is unavailable", activity.<EditText>findViewById(R.id.createPassword).getError().toString());
        }
    }

    private void assertAuthUiEnabled(LoginActivity activity, boolean enabled) {
        assertEquals(enabled, activity.findViewById(R.id.loginButton).isEnabled());
        assertEquals(enabled, activity.findViewById(R.id.guestButton).isEnabled());
        assertEquals(enabled, activity.findViewById(R.id.createAccountButton).isEnabled());
        assertEquals(enabled, activity.findViewById(R.id.loginTabButton).isEnabled());
        assertEquals(enabled, activity.findViewById(R.id.createAccountTabButton).isEnabled());
        assertEquals(enabled, activity.findViewById(R.id.createAdminCheckbox).isEnabled());
        assertEquals(enabled, activity.findViewById(R.id.createAdminPasscode).isEnabled());
    }

    private void invokePrivate(Object target, String methodName) throws Exception {
        invokePrivate(target, methodName, new Class[]{}, new Object[]{});
    }

    private void invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private Boolean invokeSaveUserProfile(LoginActivity activity,
                                          String uid,
                                          String name,
                                          String email,
                                          String phone,
                                          String role) throws Exception {
        Class<?> callbackType = Class.forName("com.example.ticketapp.LoginActivity$ProfileSaveCallback");
        AtomicReference<Boolean> callbackResult = new AtomicReference<>();

        Object callback = Proxy.newProxyInstance(
                callbackType.getClassLoader(),
                new Class[]{callbackType},
                (proxy, method, args) -> {
                    if ("onComplete".equals(method.getName())) {
                        callbackResult.set((Boolean) args[0]);
                    }
                    return null;
                });

        Method method = LoginActivity.class.getDeclaredMethod(
                "saveUserProfile",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                callbackType);
        method.setAccessible(true);
        method.invoke(activity, uid, name, email, phone, role, callback);
        return callbackResult.get();
    }
}


