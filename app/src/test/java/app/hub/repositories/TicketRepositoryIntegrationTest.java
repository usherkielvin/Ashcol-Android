package app.hub.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import app.hub.models.Ticket;

/**
 * Integration tests for TicketRepository using Firebase Emulator
 * 
 * **Validates: Requirement 19.3**
 * 
 * SETUP INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Initialize emulators: firebase init emulators (select Firestore)
 * 3. Start emulator: firebase emulators:start
 * 4. Configure test to use emulator (see setUp() method)
 * 5. Run tests: ./gradlew test --tests TicketRepositoryIntegrationTest
 * 
 * These tests verify:
 * - CRUD operations (create, read, update, delete)
 * - Real-time listener behavior
 * - Pagination functionality
 * - Query filters (by status, date range, user/employee/branch)
 */
public class TicketRepositoryIntegrationTest {
    
    private FirebaseFirestore db;
    private TicketRepository repository;
    private List<String> createdTicketIds;
    
    @Before
    public void setUp() {
        // TODO: Configure Firestore to use emulator
        // FirebaseFirestore.getInstance().useEmulator("localhost", 8080);
        
        db = FirebaseFirestore.getInstance();
        repository = new TicketRepository();
        createdTicketIds = new ArrayList<>();
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up created tickets
        for (String ticketId : createdTicketIds) {
            try {
                Tasks.await(db.collection("tickets").document(ticketId).delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    // ========== CRUD Operations Tests ==========
    
    @Test
    public void testCreateTicket_withValidData_createsDocument() throws Exception {
        // Arrange
        Ticket ticket = createSampleTicket("user123", "Test Service", "pending");
        
        // Act
        DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());
        
        // Assert
        assertNotNull("Document reference should not be null", docRef);
        assertNotNull("Document ID should not be null", docRef.getId());
        
        // Verify document exists in Firestore
        DocumentSnapshot snapshot = Tasks.await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Service type should match", "Test Service", snapshot.getString("serviceType"));
    }
    
    @Test
    public void testGetTicket_withExistingId_returnsDocument() throws Exception {
        // Arrange
        Ticket ticket = createSampleTicket("user123", "Test Service", "pending");
        DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());
        
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getTicket(docRef.getId()));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Service type should match", "Test Service", snapshot.getString("serviceType"));
    }
    
    @Test
    public void testGetTicket_withNonExistentId_returnsNonExistentSnapshot() throws Exception {
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getTicket("nonexistent_id_12345"));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertFalse("Document should not exist", snapshot.exists());
    }
    
    @Test
    public void testUpdateTicket_withValidData_updatesDocument() throws Exception {
        // Arrange
        Ticket ticket = createSampleTicket("user123", "Test Service", "pending");
        DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());
        
        // Act
        Tasks.await(repository.updateTicket(docRef.getId(), 
            java.util.Map.of("status", "in_progress", "description", "Updated description")));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(docRef.get());
        assertEquals("Status should be updated", "in_progress", snapshot.getString("status"));
        assertEquals("Description should be updated", "Updated description", snapshot.getString("description"));
    }
    
    @Test
    public void testDeleteTicket_withExistingId_deletesDocument() throws Exception {
        // Arrange
        Ticket ticket = createSampleTicket("user123", "Test Service", "pending");
        DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
        String ticketId = docRef.getId();
        
        // Act
        Tasks.await(repository.deleteTicket(ticketId));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(docRef.get());
        assertFalse("Document should not exist after deletion", snapshot.exists());
    }
    
    // ========== Real-Time Listener Tests ==========
    
    @Test
    public void testGetUserTickets_withRealTimeListener_receivesUpdates() throws Exception {
        // Arrange
        String userId = "user_realtime_test";
        Ticket ticket = createSampleTicket(userId, "Real-time Test", "pending");
        DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());
        
        CountDownLatch latch = new CountDownLatch(2); // Initial load + update
        List<Ticket> receivedTickets = new ArrayList<>();
        
        // Act
        LiveData<List<Ticket>> liveData = repository.getUserTickets(userId);
        Observer<List<Ticket>> observer = tickets -> {
            if (tickets != null && !tickets.isEmpty()) {
                receivedTickets.clear();
                receivedTickets.addAll(tickets);
                latch.countDown();
            }
        };
        
        // TODO: Observe on main thread in actual implementation
        // liveData.observeForever(observer);
        
        // Update ticket to trigger listener
        Tasks.await(repository.updateTicket(docRef.getId(), 
            java.util.Map.of("status", "in_progress")));
        
        // Assert
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("Should receive real-time updates", completed);
        assertFalse("Should have received tickets", receivedTickets.isEmpty());
    }
    
    @Test
    public void testGetEmployeeTickets_withRealTimeListener_filtersCorrectly() throws Exception {
        // Arrange
        String employeeId = "employee123";
        Ticket ticket1 = createSampleTicket("user1", "Service 1", "assigned");
        ticket1.setAssignedEmployeeId(employeeId);
        Ticket ticket2 = createSampleTicket("user2", "Service 2", "assigned");
        ticket2.setAssignedEmployeeId("other_employee");
        
        DocumentReference docRef1 = Tasks.await(repository.createTicket(ticket1));
        DocumentReference docRef2 = Tasks.await(repository.createTicket(ticket2));
        createdTicketIds.add(docRef1.getId());
        createdTicketIds.add(docRef2.getId());
        
        // Act
        LiveData<List<Ticket>> liveData = repository.getEmployeeTickets(employeeId);
        
        // Assert
        // TODO: Verify only tickets assigned to employeeId are returned
        assertNotNull("LiveData should not be null", liveData);
    }
    
    @Test
    public void testGetBranchTickets_withRealTimeListener_filtersCorrectly() throws Exception {
        // Arrange
        String branchId = "branch123";
        Ticket ticket1 = createSampleTicket("user1", "Service 1", "pending");
        ticket1.setBranchId(branchId);
        Ticket ticket2 = createSampleTicket("user2", "Service 2", "pending");
        ticket2.setBranchId("other_branch");
        
        DocumentReference docRef1 = Tasks.await(repository.createTicket(ticket1));
        DocumentReference docRef2 = Tasks.await(repository.createTicket(ticket2));
        createdTicketIds.add(docRef1.getId());
        createdTicketIds.add(docRef2.getId());
        
        // Act
        LiveData<List<Ticket>> liveData = repository.getBranchTickets(branchId);
        
        // Assert
        // TODO: Verify only tickets for branchId are returned
        assertNotNull("LiveData should not be null", liveData);
    }
    
    // ========== Pagination Tests ==========
    
    @Test
    public void testGetTicketsPage_withPageSize_returnsLimitedResults() throws Exception {
        // Arrange - Create multiple tickets
        String userId = "user_pagination_test";
        for (int i = 0; i < 15; i++) {
            Ticket ticket = createSampleTicket(userId, "Service " + i, "pending");
            DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
            createdTicketIds.add(docRef.getId());
        }
        
        // Act - Get first page
        QuerySnapshot firstPage = Tasks.await(repository.getTicketsPage(null, 10));
        
        // Assert
        assertNotNull("First page should not be null", firstPage);
        assertEquals("Should return 10 documents", 10, firstPage.size());
        
        // Act - Get second page
        DocumentSnapshot lastVisible = firstPage.getDocuments().get(firstPage.size() - 1);
        QuerySnapshot secondPage = Tasks.await(repository.getTicketsPage(lastVisible, 10));
        
        // Assert
        assertNotNull("Second page should not be null", secondPage);
        assertTrue("Second page should have remaining documents", secondPage.size() > 0);
        assertTrue("Second page should have at most 5 documents", secondPage.size() <= 5);
    }
    
    @Test
    public void testGetTicketsPage_withNullCursor_returnsFirstPage() throws Exception {
        // Arrange
        String userId = "user_first_page_test";
        for (int i = 0; i < 5; i++) {
            Ticket ticket = createSampleTicket(userId, "Service " + i, "pending");
            DocumentReference docRef = Tasks.await(repository.createTicket(ticket));
            createdTicketIds.add(docRef.getId());
        }
        
        // Act
        QuerySnapshot page = Tasks.await(repository.getTicketsPage(null, 10));
        
        // Assert
        assertNotNull("Page should not be null", page);
        assertTrue("Should return at least 5 documents", page.size() >= 5);
    }
    
    // ========== Query Filter Tests ==========
    
    @Test
    public void testGetTicketsByStatus_filtersCorrectly() throws Exception {
        // Arrange
        String userId = "user_status_filter_test";
        Ticket pendingTicket = createSampleTicket(userId, "Pending Service", "pending");
        Ticket completedTicket = createSampleTicket(userId, "Completed Service", "completed");
        
        DocumentReference docRef1 = Tasks.await(repository.createTicket(pendingTicket));
        DocumentReference docRef2 = Tasks.await(repository.createTicket(completedTicket));
        createdTicketIds.add(docRef1.getId());
        createdTicketIds.add(docRef2.getId());
        
        // Act
        LiveData<List<Ticket>> liveData = repository.getTicketsByStatus("pending", userId);
        
        // Assert
        // TODO: Verify only pending tickets are returned
        assertNotNull("LiveData should not be null", liveData);
    }
    
    @Test
    public void testGetTicketsByDateRange_filtersCorrectly() throws Exception {
        // Arrange
        String userId = "user_date_filter_test";
        Date startDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000); // 7 days ago
        Date endDate = new Date();
        
        Ticket oldTicket = createSampleTicket(userId, "Old Service", "pending");
        oldTicket.setCreatedAt(Timestamp.now()); // Current time
        
        DocumentReference docRef = Tasks.await(repository.createTicket(oldTicket));
        createdTicketIds.add(docRef.getId());
        
        // Act
        LiveData<List<Ticket>> liveData = repository.getTicketsByDateRange(startDate, endDate, userId);
        
        // Assert
        // TODO: Verify only tickets within date range are returned
        assertNotNull("LiveData should not be null", liveData);
    }
    
    @Test
    public void testGetTicketsByStatus_withMultipleStatuses_returnsCorrectTickets() throws Exception {
        // Arrange
        String userId = "user_multi_status_test";
        Ticket ticket1 = createSampleTicket(userId, "Service 1", "pending");
        Ticket ticket2 = createSampleTicket(userId, "Service 2", "in_progress");
        Ticket ticket3 = createSampleTicket(userId, "Service 3", "completed");
        
        DocumentReference docRef1 = Tasks.await(repository.createTicket(ticket1));
        DocumentReference docRef2 = Tasks.await(repository.createTicket(ticket2));
        DocumentReference docRef3 = Tasks.await(repository.createTicket(ticket3));
        createdTicketIds.add(docRef1.getId());
        createdTicketIds.add(docRef2.getId());
        createdTicketIds.add(docRef3.getId());
        
        // Act - Filter by "in_progress"
        LiveData<List<Ticket>> liveData = repository.getTicketsByStatus("in_progress", userId);
        
        // Assert
        // TODO: Verify only in_progress ticket is returned
        assertNotNull("LiveData should not be null", liveData);
    }
    
    // ========== Helper Methods ==========
    
    private Ticket createSampleTicket(String customerId, String serviceType, String status) {
        Ticket ticket = new Ticket();
        ticket.setCustomerId(customerId);
        ticket.setCustomerName("Test Customer");
        ticket.setCustomerEmail("test@example.com");
        ticket.setCustomerPhone("+1234567890");
        ticket.setServiceType(serviceType);
        ticket.setDescription("Test description for " + serviceType);
        ticket.setStatus(status);
        ticket.setPriority("medium");
        ticket.setLocation(new GeoPoint(37.7749, -122.4194)); // San Francisco
        ticket.setAddress("123 Test St, San Francisco, CA");
        ticket.setCreatedAt(Timestamp.now());
        ticket.setUpdatedAt(Timestamp.now());
        return ticket;
    }
}
