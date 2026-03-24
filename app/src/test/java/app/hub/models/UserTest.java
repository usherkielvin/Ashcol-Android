package app.hub.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for User model class
 * 
 * Tests the User model's toMap() and fromSnapshot() methods,
 * as well as helper methods for role checks and FCM token management.
 */
@RunWith(RobolectricTestRunner.class)
public class UserTest {
    
    private User user;
    private Timestamp testTimestamp;
    
    @Before
    public void setUp() {
        user = new User();
        testTimestamp = new Timestamp(new Date());
    }
    
    @Test
    public void testDefaultConstructor_initializesFcmTokens() {
        assertNotNull("FCM tokens list should be initialized", user.getFcmTokens());
        assertTrue("FCM tokens list should be empty", user.getFcmTokens().isEmpty());
    }
    
    @Test
    public void testFullConstructor_setsAllFields() {
        // Arrange
        List<String> fcmTokens = Arrays.asList("token1", "token2");
        
        // Act
        User fullUser = new User(
            "uid123",
            "john@example.com",
            "John Doe",
            "+1234567890",
            "manager",
            "branch789",
            fcmTokens,
            "https://example.com/photo.jpg",
            testTimestamp,
            testTimestamp
        );
        
        // Assert
        assertEquals("uid123", fullUser.getUid());
        assertEquals("john@example.com", fullUser.getEmail());
        assertEquals("John Doe", fullUser.getName());
        assertEquals("+1234567890", fullUser.getPhone());
        assertEquals("manager", fullUser.getRole());
        assertEquals("branch789", fullUser.getBranchId());
        assertEquals(2, fullUser.getFcmTokens().size());
        assertEquals("https://example.com/photo.jpg", fullUser.getProfilePhotoUrl());
        assertEquals(testTimestamp, fullUser.getCreatedAt());
        assertEquals(testTimestamp, fullUser.getUpdatedAt());
    }
    
    @Test
    public void testFullConstructor_handlesNullFcmTokens() {
        // Act
        User fullUser = new User(
            "uid123",
            "john@example.com",
            "John Doe",
            "+1234567890",
            "user",
            null,
            null,
            null,
            testTimestamp,
            testTimestamp
        );
        
        // Assert
        assertNotNull("FCM tokens should be initialized", fullUser.getFcmTokens());
        assertTrue("FCM tokens should be empty", fullUser.getFcmTokens().isEmpty());
    }
    
    @Test
    public void testToMap_convertsAllFieldsCorrectly() {
        // Arrange
        List<String> fcmTokens = Arrays.asList("token1", "token2");
        user.setUid("uid123");
        user.setEmail("john@example.com");
        user.setName("John Doe");
        user.setPhone("+1234567890");
        user.setRole("manager");
        user.setBranchId("branch789");
        user.setFcmTokens(fcmTokens);
        user.setProfilePhotoUrl("https://example.com/photo.jpg");
        user.setCreatedAt(testTimestamp);
        user.setUpdatedAt(testTimestamp);
        
        // Act
        Map<String, Object> map = user.toMap();
        
        // Assert
        assertNotNull("Map should not be null", map);
        assertEquals("uid123", map.get("uid"));
        assertEquals("john@example.com", map.get("email"));
        assertEquals("John Doe", map.get("name"));
        assertEquals("+1234567890", map.get("phone"));
        assertEquals("manager", map.get("role"));
        assertEquals("branch789", map.get("branchId"));
        assertEquals(fcmTokens, map.get("fcmTokens"));
        assertEquals("https://example.com/photo.jpg", map.get("profilePhotoUrl"));
        assertEquals(testTimestamp, map.get("createdAt"));
        assertEquals(testTimestamp, map.get("updatedAt"));
    }
    
    @Test
    public void testToMap_handlesNullFields() {
        // Arrange - only set required fields
        user.setUid("uid123");
        user.setEmail("john@example.com");
        
        // Act
        Map<String, Object> map = user.toMap();
        
        // Assert
        assertNotNull("Map should not be null", map);
        assertEquals("uid123", map.get("uid"));
        assertEquals("john@example.com", map.get("email"));
        assertNull(map.get("name"));
        assertNull(map.get("phone"));
        assertNull(map.get("role"));
        assertNull(map.get("branchId"));
        assertNull(map.get("profilePhotoUrl"));
    }
    
    @Test
    public void testFromSnapshot_returnsNullForNonExistentDocument() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);
        
        // Act
        User result = User.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNull("Should return null for non-existent document", result);
    }
    
    @Test
    public void testFromSnapshot_convertsAllFieldsCorrectly() {
        // Arrange
        List<String> fcmTokens = Arrays.asList("token1", "token2");
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getString("uid")).thenReturn("uid123");
        when(mockSnapshot.getString("email")).thenReturn("john@example.com");
        when(mockSnapshot.getString("name")).thenReturn("John Doe");
        when(mockSnapshot.getString("phone")).thenReturn("+1234567890");
        when(mockSnapshot.getString("role")).thenReturn("manager");
        when(mockSnapshot.getString("branchId")).thenReturn("branch789");
        when(mockSnapshot.get("fcmTokens")).thenReturn(fcmTokens);
        when(mockSnapshot.getString("profilePhotoUrl")).thenReturn("https://example.com/photo.jpg");
        when(mockSnapshot.getTimestamp("createdAt")).thenReturn(testTimestamp);
        when(mockSnapshot.getTimestamp("updatedAt")).thenReturn(testTimestamp);
        
        // Act
        User result = User.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNotNull("User should not be null", result);
        assertEquals("uid123", result.getUid());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John Doe", result.getName());
        assertEquals("+1234567890", result.getPhone());
        assertEquals("manager", result.getRole());
        assertEquals("branch789", result.getBranchId());
        assertEquals(2, result.getFcmTokens().size());
        assertEquals("https://example.com/photo.jpg", result.getProfilePhotoUrl());
        assertEquals(testTimestamp, result.getCreatedAt());
        assertEquals(testTimestamp, result.getUpdatedAt());
    }
    
    @Test
    public void testFromSnapshot_handlesNullFcmTokens() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getString("uid")).thenReturn("uid123");
        when(mockSnapshot.get("fcmTokens")).thenReturn(null);
        
        // Act
        User result = User.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNotNull("FCM tokens should be initialized", result.getFcmTokens());
        assertTrue("FCM tokens should be empty", result.getFcmTokens().isEmpty());
    }
    
    @Test
    public void testFromSnapshot_handlesNullOptionalFields() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getString("uid")).thenReturn("uid123");
        when(mockSnapshot.getString("email")).thenReturn("john@example.com");
        when(mockSnapshot.getString("name")).thenReturn(null);
        when(mockSnapshot.getString("phone")).thenReturn(null);
        when(mockSnapshot.getString("role")).thenReturn(null);
        when(mockSnapshot.getString("branchId")).thenReturn(null);
        when(mockSnapshot.getString("profilePhotoUrl")).thenReturn(null);
        when(mockSnapshot.getTimestamp("createdAt")).thenReturn(null);
        when(mockSnapshot.getTimestamp("updatedAt")).thenReturn(null);
        
        // Act
        User result = User.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNotNull("User should not be null", result);
        assertEquals("uid123", result.getUid());
        assertEquals("john@example.com", result.getEmail());
        assertNull(result.getName());
        assertNull(result.getPhone());
        assertNull(result.getRole());
        assertNull(result.getBranchId());
        assertNull(result.getProfilePhotoUrl());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
    }
    
    @Test
    public void testAddFcmToken_addsNewToken() {
        // Act
        user.addFcmToken("token1");
        user.addFcmToken("token2");
        
        // Assert
        assertEquals(2, user.getFcmTokens().size());
        assertTrue(user.getFcmTokens().contains("token1"));
        assertTrue(user.getFcmTokens().contains("token2"));
    }
    
    @Test
    public void testAddFcmToken_doesNotAddDuplicates() {
        // Act
        user.addFcmToken("token1");
        user.addFcmToken("token1");
        
        // Assert
        assertEquals(1, user.getFcmTokens().size());
    }
    
    @Test
    public void testAddFcmToken_initializesListIfNull() {
        // Arrange
        user.setFcmTokens(null);
        
        // Act
        user.addFcmToken("token1");
        
        // Assert
        assertNotNull("FCM tokens should be initialized", user.getFcmTokens());
        assertEquals(1, user.getFcmTokens().size());
        assertTrue(user.getFcmTokens().contains("token1"));
    }
    
    @Test
    public void testRemoveFcmToken_removesExistingToken() {
        // Arrange
        user.addFcmToken("token1");
        user.addFcmToken("token2");
        
        // Act
        user.removeFcmToken("token1");
        
        // Assert
        assertEquals(1, user.getFcmTokens().size());
        assertFalse(user.getFcmTokens().contains("token1"));
        assertTrue(user.getFcmTokens().contains("token2"));
    }
    
    @Test
    public void testRemoveFcmToken_handlesNullList() {
        // Arrange
        user.setFcmTokens(null);
        
        // Act - should not throw exception
        user.removeFcmToken("token1");
        
        // Assert
        assertNull(user.getFcmTokens());
    }
    
    @Test
    public void testIsAdmin_returnsTrueForAdminRole() {
        user.setRole("admin");
        assertTrue(user.isAdmin());
    }
    
    @Test
    public void testIsAdmin_returnsFalseForOtherRoles() {
        user.setRole("manager");
        assertFalse(user.isAdmin());
        
        user.setRole("employee");
        assertFalse(user.isAdmin());
        
        user.setRole("user");
        assertFalse(user.isAdmin());
    }
    
    @Test
    public void testIsManager_returnsTrueForManagerRole() {
        user.setRole("manager");
        assertTrue(user.isManager());
    }
    
    @Test
    public void testIsManager_returnsFalseForOtherRoles() {
        user.setRole("admin");
        assertFalse(user.isManager());
        
        user.setRole("employee");
        assertFalse(user.isManager());
        
        user.setRole("user");
        assertFalse(user.isManager());
    }
    
    @Test
    public void testIsEmployee_returnsTrueForEmployeeRole() {
        user.setRole("employee");
        assertTrue(user.isEmployee());
    }
    
    @Test
    public void testIsEmployee_returnsFalseForOtherRoles() {
        user.setRole("admin");
        assertFalse(user.isEmployee());
        
        user.setRole("manager");
        assertFalse(user.isEmployee());
        
        user.setRole("user");
        assertFalse(user.isEmployee());
    }
    
    @Test
    public void testIsRegularUser_returnsTrueForUserRole() {
        user.setRole("user");
        assertTrue(user.isRegularUser());
    }
    
    @Test
    public void testIsRegularUser_returnsFalseForOtherRoles() {
        user.setRole("admin");
        assertFalse(user.isRegularUser());
        
        user.setRole("manager");
        assertFalse(user.isRegularUser());
        
        user.setRole("employee");
        assertFalse(user.isRegularUser());
    }
    
    @Test
    public void testToString_containsKeyFields() {
        // Arrange
        user.setUid("uid123");
        user.setEmail("john@example.com");
        user.setName("John Doe");
        user.setRole("manager");
        
        // Act
        String result = user.toString();
        
        // Assert
        assertTrue(result.contains("uid123"));
        assertTrue(result.contains("john@example.com"));
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("manager"));
    }
    
    @Test
    public void testRoleHelpers_handleNullRole() {
        // Arrange
        user.setRole(null);
        
        // Act & Assert
        assertFalse(user.isAdmin());
        assertFalse(user.isManager());
        assertFalse(user.isEmployee());
        assertFalse(user.isRegularUser());
    }
    
    @Test
    public void testRoleHelpers_areCaseSensitive() {
        // Arrange
        user.setRole("ADMIN");
        
        // Act & Assert
        assertFalse("Role check should be case-sensitive", user.isAdmin());
    }
}
