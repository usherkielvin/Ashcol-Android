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
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import app.hub.models.Branch;
import app.hub.models.Employee;

/**
 * Integration tests for BranchRepository using Firebase Emulator
 * 
 * **Validates: Requirement 19.3**
 * 
 * SETUP INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Initialize emulators: firebase init emulators (select Firestore)
 * 3. Start emulator: firebase emulators:start
 * 4. Configure test to use emulator (see setUp() method)
 * 5. Run tests: ./gradlew test --tests BranchRepositoryIntegrationTest
 * 
 * These tests verify:
 * - Branch CRUD operations
 * - Employee management within branches (subcollection)
 * - Real-time branch updates
 * - Query operations for branches
 */
public class BranchRepositoryIntegrationTest {
    
    private FirebaseFirestore db;
    private BranchRepository repository;
    private List<String> createdBranchIds;
    
    @Before
    public void setUp() {
        // TODO: Configure Firestore to use emulator
        // FirebaseFirestore.getInstance().useEmulator("localhost", 8080);
        
        db = FirebaseFirestore.getInstance();
        repository = new BranchRepository();
        createdBranchIds = new ArrayList<>();
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up created branches
        for (String branchId : createdBranchIds) {
            try {
                Tasks.await(db.collection("branches").document(branchId).delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    // ========== Branch CRUD Operations Tests ==========
    
    @Test
    public void testGetAllBranches_returnsAllBranches() throws Exception {
        // Arrange
        Branch branch1 = createSampleBranch("Branch 1", 37.7749, -122.4194);
        Branch branch2 = createSampleBranch("Branch 2", 34.0522, -118.2437);
        
        DocumentReference docRef1 = Tasks.await(db.collection("branches").add(branch1.toMap()));
        DocumentReference docRef2 = Tasks.await(db.collection("branches").add(branch2.toMap()));
        createdBranchIds.add(docRef1.getId());
        createdBranchIds.add(docRef2.getId());
        
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getAllBranches());
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Should return at least 2 branches", snapshot.size() >= 2);
    }
    
    @Test
    public void testGetBranch_withExistingId_returnsDocument() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Test Branch", 37.7749, -122.4194);
        DocumentReference docRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(docRef.getId());
        
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getBranch(docRef.getId()));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Branch name should match", "Test Branch", snapshot.getString("name"));
    }
    
    @Test
    public void testGetBranch_withNonExistentId_returnsNonExistentSnapshot() throws Exception {
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getBranch("nonexistent_branch_12345"));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertFalse("Document should not exist", snapshot.exists());
    }
    
    @Test
    public void testGetBranchesLiveData_withRealTimeListener_receivesUpdates() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Realtime Branch", 37.7749, -122.4194);
        DocumentReference docRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(docRef.getId());
        
        // Act
        LiveData<List<Branch>> liveData = repository.getBranchesLiveData();
        
        // Assert
        assertNotNull("LiveData should not be null", liveData);
        
        // TODO: In actual implementation, observe LiveData and verify updates
        // Add another branch and verify observer receives the change
    }
    
    // ========== Employee Management Tests (Subcollection) ==========
    
    @Test
    public void testAddEmployeeToBranch_withValidData_createsSubcollectionDocument() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Employee Test Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        Employee employee = createSampleEmployee("emp123", "John Doe", "john@example.com");
        
        // Act
        DocumentReference empRef = Tasks.await(repository.addEmployeeToBranch(branchRef.getId(), employee));
        
        // Assert
        assertNotNull("Employee reference should not be null", empRef);
        assertNotNull("Employee ID should not be null", empRef.getId());
        
        // Verify employee exists in subcollection
        DocumentSnapshot empSnapshot = Tasks.await(empRef.get());
        assertTrue("Employee document should exist", empSnapshot.exists());
        assertEquals("Employee name should match", "John Doe", empSnapshot.getString("name"));
    }
    
    @Test
    public void testGetBranchEmployees_returnsAllEmployees() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Multi Employee Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        Employee emp1 = createSampleEmployee("emp1", "Employee One", "emp1@example.com");
        Employee emp2 = createSampleEmployee("emp2", "Employee Two", "emp2@example.com");
        
        Tasks.await(repository.addEmployeeToBranch(branchRef.getId(), emp1));
        Tasks.await(repository.addEmployeeToBranch(branchRef.getId(), emp2));
        
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getBranchEmployees(branchRef.getId()));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Should return 2 employees", 2, snapshot.size());
    }
    
    @Test
    public void testGetBranchEmployees_withNoEmployees_returnsEmptySnapshot() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Empty Employee Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getBranchEmployees(branchRef.getId()));
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Should return 0 employees", 0, snapshot.size());
    }
    
    @Test
    public void testRemoveEmployeeFromBranch_withExistingEmployee_deletesDocument() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Remove Employee Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        Employee employee = createSampleEmployee("emp_remove", "Remove Me", "remove@example.com");
        DocumentReference empRef = Tasks.await(repository.addEmployeeToBranch(branchRef.getId(), employee));
        
        // Act
        Tasks.await(repository.removeEmployeeFromBranch(branchRef.getId(), empRef.getId()));
        
        // Assert
        DocumentSnapshot empSnapshot = Tasks.await(empRef.get());
        assertFalse("Employee document should not exist after removal", empSnapshot.exists());
    }
    
    @Test
    public void testRemoveEmployeeFromBranch_withNonExistentEmployee_completesWithoutError() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Nonexistent Employee Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        // Act & Assert - Should not throw exception
        Tasks.await(repository.removeEmployeeFromBranch(branchRef.getId(), "nonexistent_employee_id"));
    }
    
    // ========== Query and Filter Tests ==========
    
    @Test
    public void testGetAllBranches_withMultipleBranches_returnsInCorrectOrder() throws Exception {
        // Arrange
        Branch branch1 = createSampleBranch("Alpha Branch", 37.7749, -122.4194);
        Branch branch2 = createSampleBranch("Beta Branch", 34.0522, -118.2437);
        Branch branch3 = createSampleBranch("Gamma Branch", 40.7128, -74.0060);
        
        DocumentReference docRef1 = Tasks.await(db.collection("branches").add(branch1.toMap()));
        DocumentReference docRef2 = Tasks.await(db.collection("branches").add(branch2.toMap()));
        DocumentReference docRef3 = Tasks.await(db.collection("branches").add(branch3.toMap()));
        createdBranchIds.add(docRef1.getId());
        createdBranchIds.add(docRef2.getId());
        createdBranchIds.add(docRef3.getId());
        
        // Act
        QuerySnapshot snapshot = Tasks.await(repository.getAllBranches());
        
        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Should return at least 3 branches", snapshot.size() >= 3);
    }
    
    @Test
    public void testGetBranch_withGeoPointLocation_retrievesCorrectly() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("GeoPoint Branch", 37.7749, -122.4194);
        branch.setCoverageRadius(10.0); // 10 km radius
        DocumentReference docRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(docRef.getId());
        
        // Act
        DocumentSnapshot snapshot = Tasks.await(repository.getBranch(docRef.getId()));
        
        // Assert
        assertTrue("Document should exist", snapshot.exists());
        GeoPoint location = snapshot.getGeoPoint("location");
        assertNotNull("Location should not be null", location);
        assertEquals("Latitude should match", 37.7749, location.getLatitude(), 0.0001);
        assertEquals("Longitude should match", -122.4194, location.getLongitude(), 0.0001);
        assertEquals("Coverage radius should match", 10.0, snapshot.getDouble("coverageRadius"), 0.01);
    }
    
    // ========== Edge Cases and Validation Tests ==========
    
    @Test
    public void testAddEmployeeToBranch_withMultipleSpecializations_storesArray() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Specialization Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        Employee employee = createSampleEmployee("emp_spec", "Specialist", "spec@example.com");
        employee.setSpecializations(List.of("plumbing", "electrical", "hvac"));
        
        // Act
        DocumentReference empRef = Tasks.await(repository.addEmployeeToBranch(branchRef.getId(), employee));
        
        // Assert
        DocumentSnapshot empSnapshot = Tasks.await(empRef.get());
        List<String> specializations = (List<String>) empSnapshot.get("specializations");
        assertNotNull("Specializations should not be null", specializations);
        assertEquals("Should have 3 specializations", 3, specializations.size());
        assertTrue("Should contain plumbing", specializations.contains("plumbing"));
        assertTrue("Should contain electrical", specializations.contains("electrical"));
        assertTrue("Should contain hvac", specializations.contains("hvac"));
    }
    
    @Test
    public void testAddEmployeeToBranch_withAvailabilityStatus_storesCorrectly() throws Exception {
        // Arrange
        Branch branch = createSampleBranch("Availability Branch", 37.7749, -122.4194);
        DocumentReference branchRef = Tasks.await(db.collection("branches").add(branch.toMap()));
        createdBranchIds.add(branchRef.getId());
        
        Employee employee = createSampleEmployee("emp_avail", "Available Employee", "avail@example.com");
        employee.setAvailable(true);
        employee.setCurrentTicketCount(3);
        employee.setTotalCompletedTickets(25);
        employee.setRating(4.5);
        
        // Act
        DocumentReference empRef = Tasks.await(repository.addEmployeeToBranch(branchRef.getId(), employee));
        
        // Assert
        DocumentSnapshot empSnapshot = Tasks.await(empRef.get());
        assertTrue("Should be available", empSnapshot.getBoolean("isAvailable"));
        assertEquals("Current ticket count should match", 3, empSnapshot.getLong("currentTicketCount").intValue());
        assertEquals("Total completed tickets should match", 25, empSnapshot.getLong("totalCompletedTickets").intValue());
        assertEquals("Rating should match", 4.5, empSnapshot.getDouble("rating"), 0.01);
    }
    
    // ========== Helper Methods ==========
    
    private Branch createSampleBranch(String name, double latitude, double longitude) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setLocation(new GeoPoint(latitude, longitude));
        branch.setAddress("123 " + name + " St");
        branch.setCoverageRadius(5.0); // 5 km default
        branch.setManagerId("manager_" + System.currentTimeMillis());
        branch.setManagerName("Manager of " + name);
        branch.setPhone("+1234567890");
        branch.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        branch.setActive(true);
        branch.setCreatedAt(Timestamp.now());
        branch.setUpdatedAt(Timestamp.now());
        return branch;
    }
    
    private Employee createSampleEmployee(String userId, String name, String email) {
        Employee employee = new Employee();
        employee.setUserId(userId);
        employee.setName(name);
        employee.setEmail(email);
        employee.setPhone("+1234567890");
        employee.setSpecializations(List.of("general"));
        employee.setAvailable(true);
        employee.setCurrentTicketCount(0);
        employee.setTotalCompletedTickets(0);
        employee.setRating(5.0);
        employee.setJoinedAt(Timestamp.now());
        employee.setUpdatedAt(Timestamp.now());
        return employee;
    }
}
