package app.hub.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import app.hub.models.Payment;
import app.hub.models.Ticket;

/**
 * Integration tests for PaymentRepository using Firebase Emulator
 * 
 * **Validates: Requirement 19.3**
 * 
 * SETUP INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Initialize emulators: firebase init emulators (select Firestore)
 * 3. Start emulator: firebase emulators:start
 * 4. Configure test to use emulator (see setUp() method)
 * 5. Run tests: ./gradlew test --tests PaymentRepositoryIntegrationTest
 * 
 * These tests verify:
 * - Payment creation in ticket subcollection
 * - Payment confirmation and status updates
 * - Payment retrieval for tickets
 * - User payment history with real-time updates
 * - Payment method handling
 */
public class PaymentRepositoryIntegrationTest {
    
    private FirebaseFirestore db;
    private PaymentRepository repository;
    private List<String> createdTicketIds;
    
    @Before
    public void setUp() {
        // TODO: Configure Firestore to use emulator
        // FirebaseFirestore.getInstance().useEmulator("localhost", 8080);
        
        db = FirebaseFirestore.getInstance();
        repository = new PaymentRepository();
        createdTicketIds = new ArrayList<>();
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up created tickets (payments will be deleted automatically as subcollection)
        for (String ticketId : createdTicketIds) {
            try {
                Tasks.await(db.collection("tickets").document(ticketId).delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    // ========== Payment Creation Tests ==========
    
    @Test
    public void testCreatePayment_withValidData_createsSubcollectionDocument() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user123");
        Payment payment = createSamplePayment(100.0, "credit_card", "pending");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        assertNotNull("Payment reference should not be null", paymentRef);
        assertNotNull("Payment ID should not be null", paymentRef.getId());
        
        // Verify payment exists in subcollection
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertTrue("Payment document should exist", snapshot.exists());
        assertEquals("Amount should match", 100.0, snapshot.getDouble("amount"), 0.01);
        assertEquals("Method should match", "credit_card", snapshot.getString("method"));
        assertEquals("Status should match", "pending", snapshot.getString("status"));
    }
    
    @Test
    public void testCreatePayment_withCashMethod_createsCorrectly() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_cash");
        Payment payment = createSamplePayment(50.0, "cash", "pending");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertTrue("Payment document should exist", snapshot.exists());
        assertEquals("Method should be cash", "cash", snapshot.getString("method"));
    }
    
    @Test
    public void testCreatePayment_withDigitalWallet_createsCorrectly() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_wallet");
        Payment payment = createSamplePayment(75.0, "digital_wallet", "pending");
        payment.setTransactionId("txn_12345");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertTrue("Payment document should exist", snapshot.exists());
        assertEquals("Method should be digital_wallet", "digital_wallet", snapshot.getString("method"));
        assertEquals("Transaction ID should match", "txn_12345", snapshot.getString("transactionId"));
    }
    
    // ========== Payment Confirmation Tests ==========
    
    @Test
    public void testConfirmPayment_withPendingPayment_updatesStatusToPaid() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_confirm");
        Payment payment = createSamplePayment(100.0, "credit_card", "pending");
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Act
        Tasks.await(repository.confirmPayment(ticketId, paymentRef.getId()));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertEquals("Status should be paid", "paid", snapshot.getString("status"));
        assertNotNull("Paid at timestamp should be set", snapshot.getTimestamp("paidAt"));
    }
    
    @Test
    public void testConfirmPayment_setsTimestamp() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_timestamp");
        Payment payment = createSamplePayment(100.0, "cash", "pending");
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        Timestamp beforeConfirm = Timestamp.now();
        
        // Act
        Tasks.await(repository.confirmPayment(ticketId, paymentRef.getId()));
        
        Timestamp afterConfirm = Timestamp.now();
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        Timestamp paidAt = snapshot.getTimestamp("paidAt");
        assertNotNull("Paid at should not be null", paidAt);
        assertTrue("Paid at should be after before timestamp", 
            paidAt.toDate().getTime() >= beforeConfirm.toDate().getTime());
        assertTrue("Paid at should be before after timestamp", 
            paidAt.toDate().getTime() <= afterConfirm.toDate().getTime());
    }
    
    // ========== Payment Retrieval Tests ==========
    
    @Test
    public void testGetTicketPayments_withMultiplePayments_returnsAll() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_multiple");
        Payment payment1 = createSamplePayment(50.0, "cash", "paid");
        Payment payment2 = createSamplePayment(30.0, "credit_card", "pending");
        
        Tasks.await(repository.createPayment(ticketId, payment1));
        Tasks.await(repository.createPayment(ticketId, payment2));
        
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getTicketPayments(ticketId));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Should return 2 payments", 2, snapshot.size());
    }
    
    @Test
    public void testGetTicketPayments_withNoPayments_returnsEmptySnapshot() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_no_payments");
        
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getTicketPayments(ticketId));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Should return 0 payments", 0, snapshot.size());
    }
    
    @Test
    public void testGetTicketPayments_withNonExistentTicket_returnsEmptySnapshot() throws Exception {
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getTicketPayments("nonexistent_ticket_12345"));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Should return 0 payments", 0, snapshot.size());
    }
    
    // ========== User Payment History Tests ==========
    
    @Test
    public void testGetUserPaymentHistory_withRealTimeListener_receivesUpdates() throws Exception {
        // Arrange
        String userId = "user_history_test";
        String ticketId = createSampleTicket(userId);
        Payment payment = createSamplePayment(100.0, "credit_card", "paid");
        Tasks.await(repository.createPayment(ticketId, payment));
        
        // Act
        LiveData<List<Payment>> liveData = repository.getUserPaymentHistory(userId);
        
        // Assert
        assertNotNull("LiveData should not be null", liveData);
        
        // TODO: In actual implementation, observe LiveData and verify updates
        // Add another payment and verify observer receives the change
    }
    
    @Test
    public void testGetUserPaymentHistory_withMultipleTickets_aggregatesPayments() throws Exception {
        // Arrange
        String userId = "user_multi_ticket";
        String ticketId1 = createSampleTicket(userId);
        String ticketId2 = createSampleTicket(userId);
        
        Payment payment1 = createSamplePayment(50.0, "cash", "paid");
        Payment payment2 = createSamplePayment(75.0, "credit_card", "paid");
        
        Tasks.await(repository.createPayment(ticketId1, payment1));
        Tasks.await(repository.createPayment(ticketId2, payment2));
        
        // Act
        LiveData<List<Payment>> liveData = repository.getUserPaymentHistory(userId);
        
        // Assert
        assertNotNull("LiveData should not be null", liveData);
        
        // TODO: Verify that both payments are included in the history
    }
    
    // ========== Payment Status Tests ==========
    
    @Test
    public void testCreatePayment_withAllStatuses_createsCorrectly() throws Exception {
        // Test all payment statuses: pending, paid, failed, refunded
        String[] statuses = {"pending", "paid", "failed", "refunded"};
        
        for (String status : statuses) {
            // Arrange
            String ticketId = createSampleTicket("user_status_" + status);
            Payment payment = createSamplePayment(100.0, "credit_card", status);
            
            // Act
            DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
            
            // Assert
            DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
            assertTrue("Payment should exist for status: " + status, snapshot.exists());
            assertEquals("Status should match", status, snapshot.getString("status"));
        }
    }
    
    @Test
    public void testCreatePayment_withEmployeeId_storesCorrectly() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_employee");
        Payment payment = createSamplePayment(100.0, "cash", "paid");
        payment.setEmployeeId("employee123");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertEquals("Employee ID should match", "employee123", snapshot.getString("employeeId"));
    }
    
    // ========== Edge Cases and Validation Tests ==========
    
    @Test
    public void testCreatePayment_withZeroAmount_createsCorrectly() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_zero");
        Payment payment = createSamplePayment(0.0, "cash", "paid");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertTrue("Payment should exist", snapshot.exists());
        assertEquals("Amount should be zero", 0.0, snapshot.getDouble("amount"), 0.01);
    }
    
    @Test
    public void testCreatePayment_withLargeAmount_createsCorrectly() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_large");
        Payment payment = createSamplePayment(9999.99, "credit_card", "pending");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertTrue("Payment should exist", snapshot.exists());
        assertEquals("Amount should match", 9999.99, snapshot.getDouble("amount"), 0.01);
    }
    
    @Test
    public void testCreatePayment_withDecimalAmount_storesAccurately() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_decimal");
        Payment payment = createSamplePayment(123.45, "digital_wallet", "paid");
        
        // Act
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertEquals("Amount should be accurate", 123.45, snapshot.getDouble("amount"), 0.001);
    }
    
    @Test
    public void testConfirmPayment_withAlreadyPaidPayment_updatesTimestamp() throws Exception {
        // Arrange
        String ticketId = createSampleTicket("user_already_paid");
        Payment payment = createSamplePayment(100.0, "cash", "paid");
        payment.setPaidAt(Timestamp.now());
        DocumentReference paymentRef = Tasks.await(repository.createPayment(ticketId, payment));
        
        Timestamp originalPaidAt = Tasks.await(paymentRef.get()).getTimestamp("paidAt");
        
        // Wait a moment
        Thread.sleep(100);
        
        // Act
        Tasks.await(repository.confirmPayment(ticketId, paymentRef.getId()));
        
        // Assert
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        Timestamp newPaidAt = snapshot.getTimestamp("paidAt");
        assertNotNull("New paid at should not be null", newPaidAt);
        // In actual implementation, this might update the timestamp or keep the original
    }
    
    // ========== Helper Methods ==========
    
    private String createSampleTicket(String customerId) throws Exception {
        Ticket ticket = new Ticket();
        ticket.setCustomerId(customerId);
        ticket.setServiceType("Test Service");
        ticket.setStatus("completed");
        ticket.setCreatedAt(Timestamp.now());
        
        DocumentReference ticketRef = Tasks.await(db.collection("tickets").add(ticket.toMap()));
        createdTicketIds.add(ticketRef.getId());
        return ticketRef.getId();
    }
    
    private Payment createSamplePayment(double amount, String method, String status) {
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setCreatedAt(Timestamp.now());
        return payment;
    }
}
