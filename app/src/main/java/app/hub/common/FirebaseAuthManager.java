package app.hub.common;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseAuthManager handles all Firebase Authentication operations including
 * email/password authentication, Google Sign-In, and phone authentication.
 * 
 * This class replaces Laravel Sanctum token management and provides direct
 * Firebase authentication integration.
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    
    // Web client ID from google-services.json for Google Sign-In
    private static final String WEB_CLIENT_ID = "927228841081-p3q144ul75esbuagua8vvjdbsa1mroa2.apps.googleusercontent.com";
    
    private final FirebaseAuth auth;
    private final Context context;
    private GoogleSignInClient googleSignInClient;
    
    public FirebaseAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.auth = FirebaseAuth.getInstance();
        configureGoogleSignIn();
    }
    
    /**
     * Configure Google Sign-In with web client ID from google-services.json
     */
    private void configureGoogleSignIn() {
        String webClientId = context.getString(app.hub.R.string.default_web_client_id);
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }
    
    /**
     * Get the configured GoogleSignInClient for launching the sign-in intent
     * 
     * @return GoogleSignInClient instance
     */
    public GoogleSignInClient getGoogleSignInClient() {
        return googleSignInClient;
    }
    
    /**
     * Sign in with Google using GoogleSignInAccount
     * 
     * This method exchanges the Google Sign-In account for Firebase authentication.
     * It extracts the ID token from the GoogleSignInAccount and creates a Firebase
     * credential to authenticate with Firebase.
     * 
     * @param account GoogleSignInAccount obtained from Google Sign-In flow
     * @return Task<AuthResult> for async handling
     */
    public Task<AuthResult> signInWithGoogle(@NonNull GoogleSignInAccount account) {
        Log.d(TAG, "signInWithGoogle: " + account.getId());
        
        // Get the ID token from the GoogleSignInAccount
        String idToken = account.getIdToken();
        
        if (idToken == null) {
            throw new IllegalArgumentException("Google Sign-In account does not contain ID token");
        }
        
        // Create Firebase credential with the Google ID token
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        // Sign in to Firebase with the Google credential
        return auth.signInWithCredential(credential);
    }
    
    /**
     * Sign in with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @return Task<AuthResult> for async handling
     */
    public Task<AuthResult> signInWithEmail(@NonNull String email, @NonNull String password) {
        Log.d(TAG, "signInWithEmail: " + email);
        return auth.signInWithEmailAndPassword(email, password);
    }
    
    /**
     * Create a new user account with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @return Task<AuthResult> for async handling
     */
    public Task<AuthResult> createUserWithEmail(@NonNull String email, @NonNull String password) {
        Log.d(TAG, "createUserWithEmail: " + email);
        return auth.createUserWithEmailAndPassword(email, password);
    }
    
    /**
     * Send password reset email
     * 
     * @param email User's email address
     * @return Task<Void> for async handling
     */
    public Task<Void> sendPasswordResetEmail(@NonNull String email) {
        Log.d(TAG, "sendPasswordResetEmail: " + email);
        return auth.sendPasswordResetEmail(email);
    }
    
    /**
     * Verify phone number for authentication
     * 
     * Note: This method requires an Activity context for SMS retrieval.
     * Call this from your Activity/Fragment with the appropriate context.
     * 
     * Example usage:
     * <pre>
     * PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
     *     .setPhoneNumber(phoneNumber)
     *     .setTimeout(60L, TimeUnit.SECONDS)
     *     .setActivity(activity)
     *     .setCallbacks(callbacks)
     *     .build();
     * PhoneAuthProvider.verifyPhoneNumber(options);
     * </pre>
     * 
     * @param phoneNumber Phone number in E.164 format (e.g., +1234567890)
     * @param callbacks Callbacks for verification state changes
     * @deprecated Use PhoneAuthOptions directly from your Activity for proper SMS retrieval
     */
    @Deprecated
    public void verifyPhoneNumber(@NonNull String phoneNumber, 
                                  @NonNull PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        Log.d(TAG, "verifyPhoneNumber: " + phoneNumber);
        // Note: This requires additional setup with PhoneAuthOptions
        // Implementation depends on the specific activity/fragment context
        throw new UnsupportedOperationException("Phone authentication requires activity context and timeout configuration. Use PhoneAuthOptions directly from your Activity.");
    }
    
    /**
     * Get the FirebaseAuth instance for advanced operations like phone authentication
     * 
     * Use this to create PhoneAuthOptions from your Activity:
     * <pre>
     * FirebaseAuth auth = authManager.getFirebaseAuth();
     * PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
     *     .setPhoneNumber(phoneNumber)
     *     .setTimeout(60L, TimeUnit.SECONDS)
     *     .setActivity(activity)
     *     .setCallbacks(callbacks)
     *     .build();
     * PhoneAuthProvider.verifyPhoneNumber(options);
     * </pre>
     * 
     * @return FirebaseAuth instance
     */
    public FirebaseAuth getFirebaseAuth() {
        return auth;
    }
    
    /**
     * Sign in with phone authentication credential
     * 
     * @param credential PhoneAuthCredential obtained from verification
     * @return Task<AuthResult> for async handling
     */
    public Task<AuthResult> signInWithPhoneCredential(@NonNull PhoneAuthCredential credential) {
        Log.d(TAG, "signInWithPhoneCredential");
        return auth.signInWithCredential(credential);
    }
    
    /**
     * Get the currently signed-in user
     * 
     * @return FirebaseUser or null if not signed in
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    /**
     * Get the Firebase ID token for the current user
     * 
     * @param forceRefresh Whether to force refresh the token
     * @return Task<GetTokenResult> containing the ID token
     */
    public Task<GetTokenResult> getIdToken(boolean forceRefresh) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No user is currently signed in");
        }
        return user.getIdToken(forceRefresh);
    }
    
    /**
     * Sign out the current user
     */
    public void signOut() {
        Log.d(TAG, "signOut");
        auth.signOut();
        
        // Also sign out from Google Sign-In
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }
    
    /**
     * Get custom claims from the current user's ID token
     * 
     * Custom claims include role and branchId set by Cloud Functions
     * 
     * @return Task<Map<String, Object>> containing custom claims
     */
    public Task<Map<String, Object>> getCustomClaims() {
        return getIdToken(false).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Map<String, Object> claims = task.getResult().getClaims();
                return claims != null ? claims : new HashMap<>();
            }
            return new HashMap<>();
        });
    }
    
    /**
     * Get the user's role from custom claims (async)
     * 
     * This method fetches the ID token and extracts the role claim.
     * For cached claims, the token is not force-refreshed.
     * 
     * @return Task<String> containing user role (admin, manager, employee, user) or null if not set
     */
    public Task<String> getUserRole() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }
        
        return user.getIdToken(false).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Map<String, Object> claims = task.getResult().getClaims();
                Object role = claims.get("role");
                return role != null ? role.toString() : null;
            }
            return null;
        });
    }
    
    /**
     * Get the user's branch ID from custom claims (async)
     * 
     * This method fetches the ID token and extracts the branchId claim.
     * For cached claims, the token is not force-refreshed.
     * 
     * @return Task<String> containing branch ID or null if not set (only for manager/employee roles)
     */
    public Task<String> getUserBranchId() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }
        
        return user.getIdToken(false).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Map<String, Object> claims = task.getResult().getClaims();
                Object branchId = claims.get("branchId");
                return branchId != null ? branchId.toString() : null;
            }
            return null;
        });
    }
    
    /**
     * Add an authentication state listener
     * 
     * @param listener FirebaseAuth.AuthStateListener to be notified of auth state changes
     */
    public void addAuthStateListener(@NonNull FirebaseAuth.AuthStateListener listener) {
        auth.addAuthStateListener(listener);
    }
    
    /**
     * Remove an authentication state listener
     * 
     * @param listener FirebaseAuth.AuthStateListener to be removed
     */
    public void removeAuthStateListener(@NonNull FirebaseAuth.AuthStateListener listener) {
        auth.removeAuthStateListener(listener);
    }
}
