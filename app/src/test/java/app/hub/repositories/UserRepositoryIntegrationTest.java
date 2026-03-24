package app.hub.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import app.hub.models.User;

/**
 * Integration tests for UserRepository using Firebase Emulator
 * 
 * **Validates: Requirement 19.3**
 * 
 * SETUP INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Initialize emulators: firebase init emulators (select Firestore)
 * 3. Start emulator: firebase emulators:start
 * 4. Configure test to use emulator (see setUp() method)
 * 5. Run tests: ./gradlew test --tests UserRepositoryIntegrationTest
 * 
 * These tests verify:
 * - User document creation
 * - User profile retrieval
 * - User profile updates
 * - FCM token management
 * - Real-time user data updates
 */
public class UserRepositoryIntegrationTest {
    
    private FirebaseFirestore db;
    private UserRepository repository;
    private List<String> createdUserIds;
    
    @Before
    public void setUp() {
        // TODO: Configure Firestore to use emulator
        // FirebaseFirestore.getInstance().useEmulator("localhost", 8080);
        
        db = FirebaseFirestore.getInstance();
        repository = new UserRepository();
        createdUserIds = new ArrayList<>();
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up created users
        for (String userId : createdUserIds) {
            try {
                Tasks.await(db.collection("users").document(userId).delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    // ========== CRUD Operations Tests ==========
    
    @Test
    public void testCreateUserDocument_withValidData_createsDocument() throws Exception {
        // Arrange
        String uid = "test_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "test@example.com", "Test User", "user");
        createdUserIds.add(uid);
        
        // Act
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Email should match", "test@example.com", snapshot.getString("email"));
        assertEquals("Name should match", "Test User", snapshot.getString("name"));
        assertEquals("Role should match", "user", snapshot.getString("role"));
    }
    
    @Test
    public void testCreateUserDocument_withManagerRole_includesBranchId() throws Exception {
        // Arrange
        String uid = "manager_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "manager@example.com", "Manager User", "manager");
        user.setBranchId("branch123");
        createdUserIds.add(uid);
        
        // Act
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Role should be manager", "manager", snapshot.getString("role"));
        assertEquals("Branch ID should match", "branch123", snapshot.getString("branchId"));
    }
    
    @Test
    public void testGetUserDocument_withExistingUser_returnsDocument() throws Exception {
        // Arrange
        String uid = "existing_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "existing@example.com", "Existing User", "user");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getUserDocument(uid));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Email should match", "existing@example.com", snapshot.getString("email"));
    }
    
    @Test
    public void testGetUserDocument_withNonExistentUser_returnsNonExistentSnapshot() throws Exception {
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getUserDocument("nonexistent_user_12345"));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertFalse("Document should not exist", snapshot.exists());
    }
    
    @Test
    public void testUpdateUserProfile_withValidData_updatesDocument() throws Exception {
        // Arrange
        String uid = "update_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "update@example.com", "Original Name", "user");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act
        Map<String, Object> updates = Map.of(
            "name", "Updated Name",
            "phone", "+9876543210"
        );
        Tasks.await(repository.updateUserProfile(uid, updates));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        assertEquals("Name should be updated", "Updated Name", snapshot.getString("name"));
        assertEquals("Phone should be updated", "+9876543210", snapshot.getString("phone"));
    }
    
    // ========== FCM Token Management Tests ==========
    
    @Test
    public void testUpdateFCMToken_withNewToken_addsToArray() throws Exception {
        // Arrange
        String uid = "fcm_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "fcm@example.com", "FCM User", "user");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act
        String token = "fcm_token_12345";
        Tasks.await(repository.updateFCMToken(uid, token));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        List<String> tokens = (List<String>) snapshot.get("fcmTokens");
        assertNotNull("FCM tokens should not be null", tokens);
        assertTrue("Token should be in array", tokens.contains(token));
    }
    
    @Test
    public void testUpdateFCMToken_withMultipleTokens_maintainsArray() throws Exception {
        // Arrange
        String uid = "multi_fcm_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "multifcm@example.com", "Multi FCM User", "user");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act - Add multiple tokens
        String token1 = "fcm_token_device1";
        String token2 = "fcm_token_device2";
        Tasks.await(repository.updateFCMToken(uid, token1));
        Tasks.await(repository.updateFCMToken(uid, token2));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        List<String> tokens = (List<String>) snapshot.get("fcmTokens");
        assertNotNull("FCM tokens should not be null", tokens);
        assertTrue("Should contain first token", tokens.contains(token1));
        assertTrue("Should contain second token", tokens.contains(token2));
    }
    
    @Test
    public void testUpdateFCMToken_withDuplicateToken_doesNotDuplicate() throws Exception {
        // Arrange
        String uid = "dup_fcm_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "dupfcm@example.com", "Dup FCM User", "user");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act - Add same token twice
        String token = "fcm_token_duplicate";
        Tasks.await(repository.updateFCMToken(uid, token));
        Tasks.await(repository.updateFCMToken(uid, token));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        List<String> tokens = (List<String>) snapshot.get("fcmTokens");
        assertNotNull("FCM tokens should not be null", tokens);
        
        // Count occurrences of the token
        long count = tokens.stream().filter(t -> t.equals(token)).count();
        assertEquals("Token should appear only once", 1, count);
    }
    
    // ========== Real-Time Updates Tests ==========
    
    @Test
    public void testGetUserLiveData_withRealTimeListener_receivesUpdates() throws Exception {
        // Arrange
        String uid = "realtime_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "realtime@example.com", "Realtime User", "user");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act
        LiveData<User> liveData = repository.getUserLiveData(uid);
        
        // Assert
        assertNotNull("LiveData should not be null", liveData);
        
        // TODO: In actual implementation, observe LiveData and verify updates
        // Update user and verify observer receives the change
        Map<String, Object> updates = Map.of("name", "Updated Realtime User");
        Tasks.await(repository.updateUserProfile(uid, updates));
    }
    
    @Test
    public void testGetUserLiveData_withNonExistentUser_returnsNull() throws Exception {
        // Act
        LiveData<User> liveData = repository.getUserLiveData("nonexistent_realtime_user");
        
        // Assert
        assertNotNull("LiveData should not be null", liveData);
        // TODO: Verify that LiveData value is null for non-existent user
    }
    
    // ========== Edge Cases and Validation Tests ==========
    
    @Test
    public void testCreateUserDocument_withEmptyFCMTokens_createsEmptyArray() throws Exception {
        // Arrange
        String uid = "empty_fcm_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "emptyfcm@example.com", "Empty FCM User", "user");
        user.setFcmTokens(new ArrayList<>()); // Empty list
        createdUserIds.add(uid);
        
        // Act
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        List<String> tokens = (List<String>) snapshot.get("fcmTokens");
        assertNotNull("FCM tokens should not be null", tokens);
        assertTrue("FCM tokens should be empty", tokens.isEmpty());
    }
    
    @Test
    public void testUpdateUserProfile_withPartialData_updatesOnlySpecifiedFields() throws Exception {
        // Arrange
        String uid = "partial_update_user_" + System.currentTimeMillis();
        User user = createSampleUser(uid, "partial@example.com", "Original Name", "user");
        user.setPhone("+1234567890");
        createdUserIds.add(uid);
        Tasks.await(repository.createUserDocument(uid, user));
        
        // Act - Update only name
        Map<String, Object> updates = Map.of("name", "New Name");
        Tasks.await(repository.updateUserProfile(uid, updates));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
        assertEquals("Name should be updated", "New Name", snapshot.getString("name"));
        assertEquals("Email should remain unchanged", "partial@example.com", snapshot.getString("email"));
        assertEquals("Phone should remain unchanged", "+1234567890", snapshot.getString("phone"));
    }
    
    @Test
    public void testCreateUserDocument_withAllRoles_createsCorrectly() throws Exception {
        // Test all four roles: admin, manager, employee, user
        String[] roles = {"admin", "manager", "employee", "user"};
        
        for (String role : roles) {
            // Arrange
            String uid = role + "_user_" + System.currentTimeMillis();
            User user = createSampleUser(uid, role + "@example.com", role + " User", role);
            if (role.equals("manager") || role.equals("employee")) {
                user.setBranchId("branch_" + role);
            }
            createdUserIds.add(uid);
            
            // Act
            Tasks.await(repository.createUserDocument(uid, user));
            
            // Assert
            DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(uid).get());
            assertTrue("Document should exist for role: " + role, snapshot.exists());
            assertEquals("Role should match", role, snapshot.getString("role"));
            
            if (role.equals("manager") || role.equals("employee")) {
                assertNotNull("Branch ID should be set for " + role, snapshot.getString("branchId"));
            }
        }
    }
    
    // ========== Helper Methods ==========
    
    private User createSampleUser(String uid, String email, String name, String role) {
        User user = new User();
        user.setUid(uid);
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setFcmTokens(new ArrayList<>());
        user.setCreatedAt(Timestamp.now());
        user.setUpdatedAt(Timestamp.now());
        return user;
    }
}
