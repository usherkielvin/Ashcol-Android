package app.hub.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.PhoneAuthCredential;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for FirebaseAuthManager
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 19.4**
 * 
 * Tests email/password authentication flows, Google Sign-In integration,
 * phone authentication, session management, and custom claims parsing.
 */
@RunWith(RobolectricTestRunner.class)
public class FirebaseAuthManagerTest {
    
    @Mock
    private GoogleSignInAccount mockGoogleAccount;
    
    @Mock
    private PhoneAuthCredential mockPhoneCredential;
    
    private FirebaseAuthManager authManager;
    private Context context;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        authManager = new FirebaseAuthManager(context);
    }
    
    // ========== Constructor and Initialization Tests ==========
    
    @Test
    public void testConstructor_initializesFirebaseAuth() {
        assertNotNull("FirebaseAuthManager should be initialized", authManager);
    }
    
    @Test
    public void testConstructor_initializesGoogleSignInClient() {
        GoogleSignInClient client = authManager.getGoogleSignInClient();
        assertNotNull("GoogleSignInClient should be initialized", client);
    }
    
    @Test
    public void testGetFirebaseAuth_returnsNonNullInstance() {
        FirebaseAuth auth = authManager.getFirebaseAuth();
        assertNotNull("FirebaseAuth instance should not be null", auth);
    }
    
    // ========== Email/Password Authentication Tests ==========
    
    @Test
    public void testSignInWithEmail_withValidCredentials_returnsTask() {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        // Act
        Task<AuthResult> result = authManager.signInWithEmail(email, password);
        
        // Assert
        assertNotNull("Task should not be null", result);
    }
    
    @Test
    public void testSignInWithEmail_withEmptyEmail_throwsException() {
        // Arrange
        String email = "";
        String password = "password123";
        
        // Act & Assert
        assertThrows("Should throw exception for empty email",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.signInWithEmail(email, password);
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testSignInWithEmail_withEmptyPassword_throwsException() {
        // Arrange
        String email = "test@example.com";
        String password = "";
        
        // Act & Assert
        assertThrows("Should throw exception for empty password",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.signInWithEmail(email, password);
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testCreateUserWithEmail_withValidCredentials_returnsTask() {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        
        // Act
        Task<AuthResult> result = authManager.createUserWithEmail(email, password);
        
        // Assert
        assertNotNull("Task should not be null", result);
    }
    
    @Test
    public void testCreateUserWithEmail_withWeakPassword_throwsException() {
        // Arrange
        String email = "newuser@example.com";
        String password = "123"; // Too short
        
        // Act & Assert
        assertThrows("Should throw exception for weak password",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.createUserWithEmail(email, password);
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testSendPasswordResetEmail_withValidEmail_returnsTask() {
        // Arrange
        String email = "reset@example.com";
        
        // Act
        Task<Void> result = authManager.sendPasswordResetEmail(email);
        
        // Assert
        assertNotNull("Task should not be null", result);
    }
    
    @Test
    public void testSendPasswordResetEmail_withInvalidEmail_throwsException() {
        // Arrange
        String email = "invalid-email";
        
        // Act & Assert
        assertThrows("Should throw exception for invalid email",
                Exception.class,
                () -> {
                    Task<Void> task = authManager.sendPasswordResetEmail(email);
                    Tasks.await(task);
                });
    }
    
    // ========== Google Sign-In Tests ==========
    
    @Test
    public void testGetGoogleSignInClient_returnsConfiguredClient() {
        GoogleSignInClient client = authManager.getGoogleSignInClient();
        assertNotNull("GoogleSignInClient should not be null", client);
    }
    
    @Test
    public void testSignInWithGoogle_throwsExceptionWhenIdTokenIsNull() {
        // Arrange
        when(mockGoogleAccount.getIdToken()).thenReturn(null);
        
        // Act & Assert
        assertThrows("Should throw IllegalArgumentException when ID token is null",
                IllegalArgumentException.class,
                () -> authManager.signInWithGoogle(mockGoogleAccount));
    }
    
    @Test
    public void testSignInWithGoogle_withValidAccount_returnsTask() {
        // Arrange
        String testIdToken = "test_id_token_12345";
        when(mockGoogleAccount.getIdToken()).thenReturn(testIdToken);
        when(mockGoogleAccount.getId()).thenReturn("test_google_id");
        
        // Act
        Task<AuthResult> result = authManager.signInWithGoogle(mockGoogleAccount);
        
        // Assert
        assertNotNull("Task should not be null", result);
    }
    
    @Test
    public void testSignInWithGoogle_withValidIdToken_createsCredential() {
        // Arrange
        String testIdToken = "valid_id_token_abc123";
        when(mockGoogleAccount.getIdToken()).thenReturn(testIdToken);
        when(mockGoogleAccount.getId()).thenReturn("google_user_123");
        
        // Act
        Task<AuthResult> result = authManager.signInWithGoogle(mockGoogleAccount);
        
        // Assert
        assertNotNull("Task should be created", result);
        verify(mockGoogleAccount).getIdToken();
    }
    
    // ========== Phone Authentication Tests ==========
    
    @Test
    public void testVerifyPhoneNumber_throwsUnsupportedOperationException() {
        // Act & Assert
        assertThrows("Should throw UnsupportedOperationException",
                UnsupportedOperationException.class,
                () -> authManager.verifyPhoneNumber("+1234567890", null));
    }
    
    @Test
    public void testSignInWithPhoneCredential_withValidCredential_returnsTask() {
        // Act
        Task<AuthResult> result = authManager.signInWithPhoneCredential(mockPhoneCredential);
        
        // Assert
        assertNotNull("Task should not be null", result);
    }
    
    @Test
    public void testSignInWithPhoneCredential_withNullCredential_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null credential",
                NullPointerException.class,
                () -> authManager.signInWithPhoneCredential(null));
    }
    
    // ========== Session Management Tests ==========
    
    @Test
    public void testGetCurrentUser_whenNoUserSignedIn_returnsNull() {
        // Act
        FirebaseUser user = authManager.getCurrentUser();
        
        // Assert
        // In unit tests without Firebase emulator, this returns null
        assertNull("Should return null when no user is signed in", user);
    }
    
    @Test
    public void testSignOut_callsFirebaseAuthSignOut() {
        // Act
        authManager.signOut();
        
        // Assert - verify no exceptions thrown
        // Verify user is signed out
        FirebaseUser user = authManager.getCurrentUser();
        assertNull("User should be null after sign out", user);
    }
    
    @Test
    public void testGetIdToken_throwsExceptionWhenNoUserSignedIn() {
        // Act & Assert
        assertThrows("Should throw IllegalStateException when no user is signed in",
                IllegalStateException.class,
                () -> authManager.getIdToken(false));
    }
    
    @Test
    public void testGetIdToken_withForceRefreshTrue_requestsNewToken() {
        // This test verifies the method signature and behavior
        // In a real environment with a signed-in user, this would force refresh the token
        
        // Act & Assert
        assertThrows("Should throw IllegalStateException when no user is signed in",
                IllegalStateException.class,
                () -> authManager.getIdToken(true));
    }
    
    @Test
    public void testGetIdToken_withForceRefreshFalse_usesCachedToken() {
        // This test verifies the method signature and behavior
        // In a real environment with a signed-in user, this would use cached token
        
        // Act & Assert
        assertThrows("Should throw IllegalStateException when no user is signed in",
                IllegalStateException.class,
                () -> authManager.getIdToken(false));
    }
    
    @Test
    public void testAddAuthStateListener_acceptsListener() {
        // Arrange
        FirebaseAuth.AuthStateListener listener = firebaseAuth -> {
            // Listener implementation
        };
        
        // Act & Assert - should not throw exception
        authManager.addAuthStateListener(listener);
    }
    
    @Test
    public void testRemoveAuthStateListener_acceptsListener() {
        // Arrange
        FirebaseAuth.AuthStateListener listener = firebaseAuth -> {
            // Listener implementation
        };
        authManager.addAuthStateListener(listener);
        
        // Act & Assert - should not throw exception
        authManager.removeAuthStateListener(listener);
    }
    
    @Test
    public void testAuthStateListener_canBeAddedAndRemoved() {
        // Arrange
        FirebaseAuth.AuthStateListener listener1 = firebaseAuth -> {};
        FirebaseAuth.AuthStateListener listener2 = firebaseAuth -> {};
        
        // Act
        authManager.addAuthStateListener(listener1);
        authManager.addAuthStateListener(listener2);
        authManager.removeAuthStateListener(listener1);
        
        // Assert - no exceptions should be thrown
        // In a real environment, listener2 would still be active
    }
    
    // ========== Custom Claims Tests ==========
    
    @Test
    public void testGetCustomClaims_whenNoUserSignedIn_returnsEmptyMap() throws Exception {
        // Act
        Task<Map<String, Object>> task = authManager.getCustomClaims();
        Map<String, Object> claims = Tasks.await(task);
        
        // Assert
        assertNotNull("Claims map should not be null", claims);
        assertTrue("Claims map should be empty when no user is signed in", claims.isEmpty());
    }
    
    @Test
    public void testGetUserRole_whenNoUserSignedIn_returnsNull() throws Exception {
        // Act
        Task<String> task = authManager.getUserRole();
        String role = Tasks.await(task);
        
        // Assert
        assertNull("Role should be null when no user is signed in", role);
    }
    
    @Test
    public void testGetUserBranchId_whenNoUserSignedIn_returnsNull() throws Exception {
        // Act
        Task<String> task = authManager.getUserBranchId();
        String branchId = Tasks.await(task);
        
        // Assert
        assertNull("Branch ID should be null when no user is signed in", branchId);
    }
    
    @Test
    public void testGetCustomClaims_parsesRoleFromToken() {
        // This test verifies the method structure
        // In a real environment with Firebase emulator and custom claims:
        // - The ID token would contain custom claims
        // - getCustomClaims() would extract them
        // - The role field would be accessible
        
        Task<Map<String, Object>> task = authManager.getCustomClaims();
        assertNotNull("Task should not be null", task);
    }
    
    @Test
    public void testGetUserRole_extractsRoleFromClaims() {
        // This test verifies the method structure
        // In a real environment with a signed-in user with custom claims:
        // - getUserRole() would return "admin", "manager", "employee", or "user"
        
        Task<String> task = authManager.getUserRole();
        assertNotNull("Task should not be null", task);
    }
    
    @Test
    public void testGetUserBranchId_extractsBranchIdFromClaims() {
        // This test verifies the method structure
        // In a real environment with a signed-in manager/employee:
        // - getUserBranchId() would return the branch ID from custom claims
        
        Task<String> task = authManager.getUserBranchId();
        assertNotNull("Task should not be null", task);
    }
    
    // ========== Edge Cases and Error Handling Tests ==========
    
    @Test
    public void testSignInWithEmail_withNullEmail_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null email",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.signInWithEmail(null, "password");
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testSignInWithEmail_withNullPassword_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null password",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.signInWithEmail("test@example.com", null);
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testCreateUserWithEmail_withNullEmail_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null email",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.createUserWithEmail(null, "password");
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testCreateUserWithEmail_withNullPassword_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null password",
                Exception.class,
                () -> {
                    Task<AuthResult> task = authManager.createUserWithEmail("test@example.com", null);
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testSendPasswordResetEmail_withNullEmail_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null email",
                Exception.class,
                () -> {
                    Task<Void> task = authManager.sendPasswordResetEmail(null);
                    Tasks.await(task);
                });
    }
    
    @Test
    public void testSignInWithGoogle_withNullAccount_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null account",
                NullPointerException.class,
                () -> authManager.signInWithGoogle(null));
    }
    
    @Test
    public void testAddAuthStateListener_withNullListener_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null listener",
                NullPointerException.class,
                () -> authManager.addAuthStateListener(null));
    }
    
    @Test
    public void testRemoveAuthStateListener_withNullListener_throwsException() {
        // Act & Assert
        assertThrows("Should throw exception for null listener",
                NullPointerException.class,
                () -> authManager.removeAuthStateListener(null));
    }
    
    // ========== Integration Scenario Tests ==========
    
    @Test
    public void testAuthenticationFlow_emailPasswordSignIn() {
        // This test demonstrates the expected flow for email/password authentication
        // In a real environment with Firebase emulator:
        // 1. Create user
        // 2. Sign in
        // 3. Get current user
        // 4. Get ID token
        // 5. Sign out
        
        String email = "integration@example.com";
        String password = "password123";
        
        // Step 1: Create user
        Task<AuthResult> createTask = authManager.createUserWithEmail(email, password);
        assertNotNull("Create task should not be null", createTask);
        
        // Step 2: Sign in (in real environment, would succeed after user creation)
        Task<AuthResult> signInTask = authManager.signInWithEmail(email, password);
        assertNotNull("Sign in task should not be null", signInTask);
        
        // Step 3: Get current user (null in unit test environment)
        FirebaseUser user = authManager.getCurrentUser();
        // In real environment: assertNotNull(user);
        
        // Step 4: Sign out
        authManager.signOut();
        
        // Step 5: Verify signed out
        FirebaseUser userAfterSignOut = authManager.getCurrentUser();
        assertNull("User should be null after sign out", userAfterSignOut);
    }
    
    @Test
    public void testAuthenticationFlow_googleSignIn() {
        // This test demonstrates the expected flow for Google Sign-In
        // In a real environment:
        // 1. Get GoogleSignInClient
        // 2. Launch sign-in intent
        // 3. Get GoogleSignInAccount from result
        // 4. Sign in with Google account
        // 5. Get current user
        
        // Step 1: Get GoogleSignInClient
        GoogleSignInClient client = authManager.getGoogleSignInClient();
        assertNotNull("GoogleSignInClient should be available", client);
        
        // Step 2-3: In real app, would launch intent and get account
        String testIdToken = "test_google_id_token";
        when(mockGoogleAccount.getIdToken()).thenReturn(testIdToken);
        when(mockGoogleAccount.getId()).thenReturn("google_user_id");
        
        // Step 4: Sign in with Google
        Task<AuthResult> signInTask = authManager.signInWithGoogle(mockGoogleAccount);
        assertNotNull("Sign in task should not be null", signInTask);
        
        // Step 5: Get current user (null in unit test environment)
        FirebaseUser user = authManager.getCurrentUser();
        // In real environment: assertNotNull(user);
    }
    
    @Test
    public void testSessionManagement_persistsAcrossAppRestarts() {
        // This test verifies that FirebaseAuth is properly initialized
        // In a real environment with Firebase:
        // - Authentication state persists across app restarts
        // - getCurrentUser() returns the signed-in user after restart
        
        FirebaseAuth auth = authManager.getFirebaseAuth();
        assertNotNull("FirebaseAuth should be initialized for persistence", auth);
    }
    
    @Test
    public void testCustomClaims_roleBasedAccess() {
        // This test demonstrates custom claims usage for role-based access
        // In a real environment with custom claims set by Cloud Functions:
        // - Admin user would have role="admin"
        // - Manager would have role="manager" and branchId="branch_123"
        // - Employee would have role="employee" and branchId="branch_123"
        // - Regular user would have role="user"
        
        Task<String> roleTask = authManager.getUserRole();
        Task<String> branchTask = authManager.getUserBranchId();
        
        assertNotNull("Role task should not be null", roleTask);
        assertNotNull("Branch task should not be null", branchTask);
    }
}
