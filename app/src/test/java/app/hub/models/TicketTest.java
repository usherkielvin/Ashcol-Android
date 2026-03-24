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
import com.google.firebase.firestore.GeoPoint;

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
 * Unit tests for Ticket model class
 * 
 * Tests the Ticket model's toMap() and fromSnapshot() methods,
 * as well as helper methods for status and priority checks.
 */
@RunWith(RobolectricTestRunner.class)
public class TicketTest {
    
    private Ticket ticket;
    private GeoPoint testLocation;
    private Timestamp testTimestamp;
    
    @Before
    public void setUp() {
        ticket = new Ticket();
        testLocation = new GeoPoint(37.7749, -122.4194); // San Francisco coordinates
        testTimestamp = new Timestamp(new Date());
    }
    
    @Test
    public void testDefaultConstructor_initializesPhotoUrls() {
        assertNotNull("Photo URLs list should be initialized", ticket.getPhotoUrls());
        assertTrue("Photo URLs list should be empty", ticket.getPhotoUrls().isEmpty());
    }
    
    @Test
    public void testFullConstructor_setsAllFields() {
        // Arrange
        List<String> photoUrls = Arrays.asList("url1", "url2");
        
        // Act
        Ticket fullTicket = new Ticket(
            "ticket123",
            "customer456",
            "John Doe",
            "john@example.com",
            "+1234567890",
            "Plumbing",
            "Fix leaking pipe",
            "pending",
            "high",
            testLocation,
            "123 Main St",
            "branch789",
            "Downtown Branch",
            "employee101",
            "Jane Smith",
            testTimestamp,
            null,
            photoUrls,
            150.0,
            0.0,
            testTimestamp,
            testTimestamp
        );
        
        // Assert
        assertEquals("ticket123", fullTicket.getId());
        assertEquals("customer456", fullTicket.getCustomerId());
        assertEquals("John Doe", fullTicket.getCustomerName());
        assertEquals("john@example.com", fullTicket.getCustomerEmail());
        assertEquals("+1234567890", fullTicket.getCustomerPhone());
        assertEquals("Plumbing", fullTicket.getServiceType());
        assertEquals("Fix leaking pipe", fullTicket.getDescription());
        assertEquals("pending", fullTicket.getStatus());
        assertEquals("high", fullTicket.getPriority());
        assertEquals(testLocation, fullTicket.getLocation());
        assertEquals("123 Main St", fullTicket.getAddress());
        assertEquals("branch789", fullTicket.getBranchId());
        assertEquals("Downtown Branch", fullTicket.getBranchName());
        assertEquals("employee101", fullTicket.getAssignedEmployeeId());
        assertEquals("Jane Smith", fullTicket.getAssignedEmployeeName());
        assertEquals(2, fullTicket.getPhotoUrls().size());
        assertEquals(150.0, fullTicket.getEstimatedCost(), 0.01);
    }
    
    @Test
    public void testToMap_convertsAllFieldsCorrectly() {
        // Arrange
        ticket.setId("ticket123");
        ticket.setCustomerId("customer456");
        ticket.setCustomerName("John Doe");
        ticket.setCustomerEmail("john@example.com");
        ticket.setCustomerPhone("+1234567890");
        ticket.setServiceType("Plumbing");
        ticket.setDescription("Fix leaking pipe");
        ticket.setStatus("pending");
        ticket.setPriority("high");
        ticket.setLocation(testLocation);
        ticket.setAddress("123 Main St");
        ticket.setBranchId("branch789");
        ticket.setBranchName("Downtown Branch");
        ticket.setEstimatedCost(150.0);
        ticket.setFinalCost(175.0);
        ticket.setCreatedAt(testTimestamp);
        ticket.setUpdatedAt(testTimestamp);
        
        // Act
        Map<String, Object> map = ticket.toMap();
        
        // Assert
        assertNotNull("Map should not be null", map);
        assertEquals("ticket123", map.get("id"));
        assertEquals("customer456", map.get("customerId"));
        assertEquals("John Doe", map.get("customerName"));
        assertEquals("john@example.com", map.get("customerEmail"));
        assertEquals("+1234567890", map.get("customerPhone"));
        assertEquals("Plumbing", map.get("serviceType"));
        assertEquals("Fix leaking pipe", map.get("description"));
        assertEquals("pending", map.get("status"));
        assertEquals("high", map.get("priority"));
        assertEquals(testLocation, map.get("location"));
        assertEquals("123 Main St", map.get("address"));
        assertEquals("branch789", map.get("branchId"));
        assertEquals("Downtown Branch", map.get("branchName"));
        assertEquals(150.0, map.get("estimatedCost"));
        assertEquals(175.0, map.get("finalCost"));
        assertEquals(testTimestamp, map.get("createdAt"));
        assertEquals(testTimestamp, map.get("updatedAt"));
    }
    
    @Test
    public void testFromSnapshot_returnsNullForNonExistentDocument() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);
        
        // Act
        Ticket result = Ticket.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNull("Should return null for non-existent document", result);
    }
    
    @Test
    public void testFromSnapshot_convertsAllFieldsCorrectly() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getId()).thenReturn("ticket123");
        when(mockSnapshot.getString("customerId")).thenReturn("customer456");
        when(mockSnapshot.getString("customerName")).thenReturn("John Doe");
        when(mockSnapshot.getString("customerEmail")).thenReturn("john@example.com");
        when(mockSnapshot.getString("customerPhone")).thenReturn("+1234567890");
        when(mockSnapshot.getString("serviceType")).thenReturn("Plumbing");
        when(mockSnapshot.getString("description")).thenReturn("Fix leaking pipe");
        when(mockSnapshot.getString("status")).thenReturn("pending");
        when(mockSnapshot.getString("priority")).thenReturn("high");
        when(mockSnapshot.getGeoPoint("location")).thenReturn(testLocation);
        when(mockSnapshot.getString("address")).thenReturn("123 Main St");
        when(mockSnapshot.getString("branchId")).thenReturn("branch789");
        when(mockSnapshot.getString("branchName")).thenReturn("Downtown Branch");
        when(mockSnapshot.getString("assignedEmployeeId")).thenReturn("employee101");
        when(mockSnapshot.getString("assignedEmployeeName")).thenReturn("Jane Smith");
        when(mockSnapshot.getTimestamp("scheduledDate")).thenReturn(testTimestamp);
        when(mockSnapshot.getTimestamp("completedDate")).thenReturn(null);
        when(mockSnapshot.get("photoUrls")).thenReturn(Arrays.asList("url1", "url2"));
        when(mockSnapshot.getDouble("estimatedCost")).thenReturn(150.0);
        when(mockSnapshot.getDouble("finalCost")).thenReturn(175.0);
        when(mockSnapshot.getTimestamp("createdAt")).thenReturn(testTimestamp);
        when(mockSnapshot.getTimestamp("updatedAt")).thenReturn(testTimestamp);
        
        // Act
        Ticket result = Ticket.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNotNull("Ticket should not be null", result);
        assertEquals("ticket123", result.getId());
        assertEquals("customer456", result.getCustomerId());
        assertEquals("John Doe", result.getCustomerName());
        assertEquals("john@example.com", result.getCustomerEmail());
        assertEquals("+1234567890", result.getCustomerPhone());
        assertEquals("Plumbing", result.getServiceType());
        assertEquals("Fix leaking pipe", result.getDescription());
        assertEquals("pending", result.getStatus());
        assertEquals("high", result.getPriority());
        assertEquals(testLocation, result.getLocation());
        assertEquals("123 Main St", result.getAddress());
        assertEquals("branch789", result.getBranchId());
        assertEquals("Downtown Branch", result.getBranchName());
        assertEquals("employee101", result.getAssignedEmployeeId());
        assertEquals("Jane Smith", result.getAssignedEmployeeName());
        assertEquals(2, result.getPhotoUrls().size());
        assertEquals(150.0, result.getEstimatedCost(), 0.01);
        assertEquals(175.0, result.getFinalCost(), 0.01);
    }
    
    @Test
    public void testFromSnapshot_handlesNullPhotoUrls() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getId()).thenReturn("ticket123");
        when(mockSnapshot.get("photoUrls")).thenReturn(null);
        when(mockSnapshot.getDouble("estimatedCost")).thenReturn(0.0);
        when(mockSnapshot.getDouble("finalCost")).thenReturn(0.0);
        
        // Act
        Ticket result = Ticket.fromSnapshot(mockSnapshot);
        
        // Assert
        assertNotNull("Photo URLs should be initialized", result.getPhotoUrls());
        assertTrue("Photo URLs should be empty", result.getPhotoUrls().isEmpty());
    }
    
    @Test
    public void testFromSnapshot_handlesNullCosts() {
        // Arrange
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.getId()).thenReturn("ticket123");
        when(mockSnapshot.getDouble("estimatedCost")).thenReturn(null);
        when(mockSnapshot.getDouble("finalCost")).thenReturn(null);
        
        // Act
        Ticket result = Ticket.fromSnapshot(mockSnapshot);
        
        // Assert
        assertEquals(0.0, result.getEstimatedCost(), 0.01);
        assertEquals(0.0, result.getFinalCost(), 0.01);
    }
    
    @Test
    public void testAddPhotoUrl_addsNewUrl() {
        // Act
        ticket.addPhotoUrl("url1");
        ticket.addPhotoUrl("url2");
        
        // Assert
        assertEquals(2, ticket.getPhotoUrls().size());
        assertTrue(ticket.getPhotoUrls().contains("url1"));
        assertTrue(ticket.getPhotoUrls().contains("url2"));
    }
    
    @Test
    public void testAddPhotoUrl_doesNotAddDuplicates() {
        // Act
        ticket.addPhotoUrl("url1");
        ticket.addPhotoUrl("url1");
        
        // Assert
        assertEquals(1, ticket.getPhotoUrls().size());
    }
    
    @Test
    public void testRemovePhotoUrl_removesExistingUrl() {
        // Arrange
        ticket.addPhotoUrl("url1");
        ticket.addPhotoUrl("url2");
        
        // Act
        ticket.removePhotoUrl("url1");
        
        // Assert
        assertEquals(1, ticket.getPhotoUrls().size());
        assertFalse(ticket.getPhotoUrls().contains("url1"));
        assertTrue(ticket.getPhotoUrls().contains("url2"));
    }
    
    @Test
    public void testIsPending_returnsTrueForPendingStatus() {
        ticket.setStatus("pending");
        assertTrue(ticket.isPending());
    }
    
    @Test
    public void testIsPending_returnsFalseForOtherStatus() {
        ticket.setStatus("completed");
        assertFalse(ticket.isPending());
    }
    
    @Test
    public void testIsAssigned_returnsTrueForAssignedStatus() {
        ticket.setStatus("assigned");
        assertTrue(ticket.isAssigned());
    }
    
    @Test
    public void testIsInProgress_returnsTrueForInProgressStatus() {
        ticket.setStatus("in_progress");
        assertTrue(ticket.isInProgress());
    }
    
    @Test
    public void testIsCompleted_returnsTrueForCompletedStatus() {
        ticket.setStatus("completed");
        assertTrue(ticket.isCompleted());
    }
    
    @Test
    public void testIsCancelled_returnsTrueForCancelledStatus() {
        ticket.setStatus("cancelled");
        assertTrue(ticket.isCancelled());
    }
    
    @Test
    public void testIsHighPriority_returnsTrueForHighPriority() {
        ticket.setPriority("high");
        assertTrue(ticket.isHighPriority());
    }
    
    @Test
    public void testIsMediumPriority_returnsTrueForMediumPriority() {
        ticket.setPriority("medium");
        assertTrue(ticket.isMediumPriority());
    }
    
    @Test
    public void testIsLowPriority_returnsTrueForLowPriority() {
        ticket.setPriority("low");
        assertTrue(ticket.isLowPriority());
    }
    
    @Test
    public void testToString_containsKeyFields() {
        // Arrange
        ticket.setId("ticket123");
        ticket.setCustomerName("John Doe");
        ticket.setStatus("pending");
        
        // Act
        String result = ticket.toString();
        
        // Assert
        assertTrue(result.contains("ticket123"));
        assertTrue(result.contains("John Doe"));
        assertTrue(result.contains("pending"));
    }
    
    @Test
    public void testGeoPointLocation_storesLatitudeAndLongitude() {
        // Arrange
        GeoPoint location = new GeoPoint(37.7749, -122.4194);
        
        // Act
        ticket.setLocation(location);
        
        // Assert
        assertEquals(37.7749, ticket.getLocation().getLatitude(), 0.0001);
        assertEquals(-122.4194, ticket.getLocation().getLongitude(), 0.0001);
    }
}
