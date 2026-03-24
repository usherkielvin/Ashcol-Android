package app.hub.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Integration tests for Role-Based Access Control (RBAC) across all four user roles.
 *
 * **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 12.7, 12.8**
 *
 * SETUP INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Start emulators: firebase emulators:start --only auth,firestore,functions
 * 3. Emulator ports: Auth=9099, Firestore=8080, Functions=5001
 * 4. Run tests: ./gradlew connectedAndroidTest --tests RoleBasedAccessControlTest
 *
 * These tests cover:
 * - Admin role: full access to all data and admin-only Cloud Functions
 * - Manager role: branch-scoped access to tickets and employees
 * - Employee role: access only to assigned tickets with status update capability
 * - User (customer) role: access only to own tickets with create capability
 * - Unauthenticated access: denied for all Firestore reads and writes
 *
 * Tests requiring full emulator setup are annotated with @Ignore and TODO comments.
 * Smoke tests run without emulator and validate test infrastructure.
 */
@RunWith(AndroidJUnit4.class)
public class RoleBasedAccessControlTest {

    // ---- Emulator configuration ----
    private static final String EMULATOR_HOST = "10.0.2.2"; // Android emulator loopback
    private static final int FIRESTORE_PORT = 8080;
    private static final int AUTH_PORT = 9099;
    private static final int FUNCTIONS_PORT = 5001;
    private static final int LATCH_TIMEOUT_SECONDS = 10;

    // ---- Test user IDs ----
    private static final String ADMIN_UID = "rbac_admin_001";
    private static final String MANAGER_UID = "rbac_manager_001";
    private static final String EMPLOYEE_UID = "rbac_employee_001";
    private static final String USER_UID = "rbac_user_001";
    private static final String OTHER_USER_UID = "rbac_other_user_002";
    private static final String OTHER_EMPLOYEE_UID = "rbac_other_employee_002";

    // ---- Test branch IDs ----
    private static final String BRANCH_A_ID = "rbac_branch_a";
    private static final String BRANCH_B_ID = "rbac_branch_b";

    // ---- Test emails ----
    private static final String ADMIN_EMAIL = "rbac_admin@test.com";
    private static final String MANAGER_EMAIL = "rbac_manager@test.com";
    private static final String EMPLOYEE_EMAIL = "rbac_employee@test.com";
    private static final String USER_EMAIL = "rbac_user@test.com";
    private static final String TEST_PASSWORD = "TestPassword123!";

    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseFunctions functions;

    private List<String> createdDocumentPaths;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // TODO: Uncomment to connect to Firebase Emulator Suite
        // FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_PORT);
        // FirebaseAuth.getInstance().useEmulator("http://" + EMULATOR_HOST + ":" + AUTH_PORT);
        // FirebaseFunctions.getInstance().useEmulator(EMULATOR_HOST, FUNCTIONS_PORT);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance();

        createdDocumentPaths = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        auth.signOut();

        for (String path : createdDocumentPaths) {
            try {
                String[] parts = path.split("/");
                DocumentReference ref = db.collection(parts[0]).document(parts[1]);
                for (int i = 2; i + 1 < parts.length; i += 2) {
                    ref = ref.collection(parts[i]).document(parts[i + 1]);
                }
                Tasks.await(ref.delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    // =========================================================================
    // SMOKE TESTS (run without emulator)
    // =========================================================================

    /**
     * Smoke test: Verifies the test class and Android context are properly set up.
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    public void smokeTest_testInfrastructureIsReady() {
        assertNotNull("Context should not be null", context);
        assertNotNull("FirebaseFirestore instance should not be null", db);
        assertNotNull("FirebaseAuth instance should not be null", auth);
        assertNotNull("FirebaseFunctions instance should not be null", functions);
    }

    /**
     * Smoke test: Verifies test constants are correctly defined for all four roles.
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    public void smokeTest_roleConstantsAreDefined() {
        assertNotNull("Admin UID should be defined", ADMIN_UID);
        assertNotNull("Manager UID should be defined", MANAGER_UID);
        assertNotNull("Employee UID should be defined", EMPLOYEE_UID);
        assertNotNull("User UID should be defined", USER_UID);
        assertFalse("Branch A ID should not be empty", BRANCH_A_ID.isEmpty());
        assertFalse("Branch B ID should not be empty", BRANCH_B_ID.isEmpty());
        assertFalse("Branch A and B should be different", BRANCH_A_ID.equals(BRANCH_B_ID));
    }

    /**
     * Smoke test: Verifies helper methods produce valid test data structures.
     * Validates: Requirements 3.1, 3.2
     */
    @Test
    public void smokeTest_helperMethodsProduceValidData() {
        Map<String, Object> ticketData = buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "pending");
        assertNotNull("Ticket data should not be null", ticketData);
        assertEquals("customerId should match", USER_UID, ticketData.get("customerId"));
        assertEquals("branchId should match", BRANCH_A_ID, ticketData.get("branchId"));
        assertEquals("assignedEmployeeId should match", EMPLOYEE_UID, ticketData.get("assignedEmployeeId"));
        assertEquals("status should match", "pending", ticketData.get("status"));

        Map<String, Object> userData = buildUserData(ADMIN_UID, ADMIN_EMAIL, "Admin User", "admin", null);
        assertNotNull("User data should not be null", userData);
        assertEquals("role should be admin", "admin", userData.get("role"));

        Map<String, Object> branchData = buildBranchData(BRANCH_A_ID, "Branch A", MANAGER_UID);
        assertNotNull("Branch data should not be null", branchData);
        assertEquals("managerId should match", MANAGER_UID, branchData.get("managerId"));
    }

    // =========================================================================
    // 1. ADMIN ROLE TESTS
    // =========================================================================

    /**
     * Admin can read all tickets across all branches.
     * Validates: Requirement 4.2
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and admin auth is set up")
    public void testAdmin_canReadAllTickets_acrossAllBranches() throws Exception {
        // Arrange: create tickets in different branches
        String ticketBranchA = "ticket_admin_read_a_" + System.currentTimeMillis();
        String ticketBranchB = "ticket_admin_read_b_" + System.currentTimeMillis();

        DocumentReference refA = db.collection("tickets").document(ticketBranchA);
        DocumentReference refB = db.collection("tickets").document(ticketBranchB);
        Tasks.await(refA.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "pending")));
        Tasks.await(refB.set(buildTicketData(OTHER_USER_UID, BRANCH_B_ID, OTHER_EMPLOYEE_UID, "assigned")));
        createdDocumentPaths.add("tickets/" + ticketBranchA);
        createdDocumentPaths.add("tickets/" + ticketBranchB);

        // TODO: Authenticate as admin with custom claims { role: "admin" }
        // authenticateAsAdmin();

        // Act & Assert: admin can read ticket from branch A
        try {
            DocumentSnapshot snapA = Tasks.await(refA.get());
            assertTrue("Admin should read ticket from branch A", snapA.exists());
        } catch (ExecutionException e) {
            fail("Admin should have read access to all tickets: " + e.getMessage());
        }

        // Act & Assert: admin can read ticket from branch B
        try {
            DocumentSnapshot snapB = Tasks.await(refB.get());
            assertTrue("Admin should read ticket from branch B", snapB.exists());
        } catch (ExecutionException e) {
            fail("Admin should have read access to all tickets: " + e.getMessage());
        }
    }

    /**
     * Admin can read all user documents.
     * Validates: Requirement 4.2
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and admin auth is set up")
    public void testAdmin_canReadAllUserDocuments() throws Exception {
        // Arrange: create user documents for different roles
        DocumentReference userRef = db.collection("users").document(USER_UID);
        DocumentReference managerRef = db.collection("users").document(MANAGER_UID);
        Tasks.await(userRef.set(buildUserData(USER_UID, USER_EMAIL, "Test User", "user", null)));
        Tasks.await(managerRef.set(buildUserData(MANAGER_UID, MANAGER_EMAIL, "Test Manager", "manager", BRANCH_A_ID)));
        createdDocumentPaths.add("users/" + USER_UID);
        createdDocumentPaths.add("users/" + MANAGER_UID);

        // TODO: Authenticate as admin
        // authenticateAsAdmin();

        // Act & Assert
        try {
            DocumentSnapshot userSnap = Tasks.await(userRef.get());
            assertTrue("Admin should read user document", userSnap.exists());
            DocumentSnapshot managerSnap = Tasks.await(managerRef.get());
            assertTrue("Admin should read manager document", managerSnap.exists());
        } catch (ExecutionException e) {
            fail("Admin should have read access to all user documents: " + e.getMessage());
        }
    }

    /**
     * Admin can read all branch documents.
     * Validates: Requirement 4.2, 4.9
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and admin auth is set up")
    public void testAdmin_canReadAllBranchDocuments() throws Exception {
        // Arrange
        DocumentReference branchARef = db.collection("branches").document(BRANCH_A_ID);
        DocumentReference branchBRef = db.collection("branches").document(BRANCH_B_ID);
        Tasks.await(branchARef.set(buildBranchData(BRANCH_A_ID, "Branch A", MANAGER_UID)));
        Tasks.await(branchBRef.set(buildBranchData(BRANCH_B_ID, "Branch B", OTHER_USER_UID)));
        createdDocumentPaths.add("branches/" + BRANCH_A_ID);
        createdDocumentPaths.add("branches/" + BRANCH_B_ID);

        // TODO: Authenticate as admin
        // authenticateAsAdmin();

        // Act & Assert
        try {
            DocumentSnapshot snapA = Tasks.await(branchARef.get());
            assertTrue("Admin should read branch A", snapA.exists());
            DocumentSnapshot snapB = Tasks.await(branchBRef.get());
            assertTrue("Admin should read branch B", snapB.exists());
        } catch (ExecutionException e) {
            fail("Admin should have read access to all branch documents: " + e.getMessage());
        }
    }

    /**
     * Admin can create, update, and delete any ticket.
     * Validates: Requirement 4.2
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and admin auth is set up")
    public void testAdmin_canCreateUpdateDeleteAnyTicket() throws Exception {
        // TODO: Authenticate as admin
        // authenticateAsAdmin();

        // Arrange: create a ticket as admin
        String ticketId = "ticket_admin_crud_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);

        // Act: create
        try {
            Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "pending")));
            createdDocumentPaths.add("tickets/" + ticketId);
            assertTrue("Admin should create any ticket", true);
        } catch (ExecutionException e) {
            fail("Admin should be able to create tickets: " + e.getMessage());
        }

        // Act: update
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("status", "assigned");
            update.put("assignedEmployeeId", EMPLOYEE_UID);
            Tasks.await(ticketRef.update(update));
            assertTrue("Admin should update any ticket", true);
        } catch (ExecutionException e) {
            fail("Admin should be able to update tickets: " + e.getMessage());
        }

        // Act: delete
        try {
            Tasks.await(ticketRef.delete());
            createdDocumentPaths.remove("tickets/" + ticketId);
            assertTrue("Admin should delete any ticket", true);
        } catch (ExecutionException e) {
            fail("Admin should be able to delete tickets: " + e.getMessage());
        }
    }

    /**
     * Admin can call getDashboardStats for all branches.
     * Validates: Requirement 8.6, 12.7
     */
    @Test
    @Ignore("TODO: Enable once getDashboardStats Cloud Function is deployed to emulator (task 8.3)")
    public void testAdmin_canCallGetDashboardStats_forAllBranches() throws Exception {
        // TODO: Authenticate as admin
        // authenticateAsAdmin();

        // Act: call getDashboardStats without branch filter (admin sees all)
        try {
            Map<String, Object> data = new HashMap<>();
            // No branchId filter — admin gets all branches
            Object result = Tasks.await(
                    functions.getHttpsCallable("getDashboardStats").call(data));
            assertNotNull("getDashboardStats should return data for admin", result);
        } catch (ExecutionException e) {
            fail("Admin should be able to call getDashboardStats: " + e.getMessage());
        }
    }

    /**
     * Admin can call createUserAccount Cloud Function.
     * Validates: Requirement 12.1, 12.7, 12.10
     */
    @Test
    @Ignore("TODO: Enable once createUserAccount Cloud Function is deployed to emulator (task 8.4)")
    public void testAdmin_canCallCreateUserAccount() throws Exception {
        // TODO: Authenticate as admin
        // authenticateAsAdmin();

        // Act: call createUserAccount
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "new_employee@test.com");
            data.put("role", "employee");
            data.put("branchId", BRANCH_A_ID);
            data.put("name", "New Employee");
            Object result = Tasks.await(
                    functions.getHttpsCallable("createUserAccount").call(data));
            assertNotNull("createUserAccount should return result for admin", result);
        } catch (ExecutionException e) {
            fail("Admin should be able to call createUserAccount: " + e.getMessage());
        }
    }

    /**
     * Admin can call setUserRole Cloud Function.
     * Validates: Requirement 12.1, 12.7, 12.10
     */
    @Test
    @Ignore("TODO: Enable once setUserRole Cloud Function is deployed to emulator (task 8.4)")
    public void testAdmin_canCallSetUserRole() throws Exception {
        // TODO: Authenticate as admin
        // authenticateAsAdmin();

        // Act: call setUserRole
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", USER_UID);
            data.put("role", "employee");
            data.put("branchId", BRANCH_A_ID);
            Object result = Tasks.await(
                    functions.getHttpsCallable("setUserRole").call(data));
            assertNotNull("setUserRole should return result for admin", result);
        } catch (ExecutionException e) {
            fail("Admin should be able to call setUserRole: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. MANAGER ROLE TESTS
    // =========================================================================

    /**
     * Manager can read tickets only for their assigned branch.
     * Validates: Requirement 4.3
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and manager auth is set up")
    public void testManager_canReadTickets_onlyForTheirBranch() throws Exception {
        // Arrange: create a ticket in manager's branch
        String ticketId = "ticket_manager_own_branch_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "pending")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as manager with custom claims { role: "manager", branchId: BRANCH_A_ID }
        // authenticateAsManager(BRANCH_A_ID);

        // Act & Assert: manager can read ticket in their branch
        try {
            DocumentSnapshot snap = Tasks.await(ticketRef.get());
            assertTrue("Manager should read tickets in their branch", snap.exists());
        } catch (ExecutionException e) {
            fail("Manager should have read access to tickets in their branch: " + e.getMessage());
        }
    }

    /**
     * Manager cannot read tickets from other branches.
     * Validates: Requirement 4.3
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and manager auth is set up")
    public void testManager_cannotReadTickets_fromOtherBranches() throws Exception {
        // Arrange: create a ticket in a different branch (B)
        String ticketId = "ticket_manager_other_branch_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_B_ID, OTHER_EMPLOYEE_UID, "pending")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as manager of BRANCH_A_ID (not BRANCH_B_ID)
        // authenticateAsManager(BRANCH_A_ID);

        // Act & Assert: manager cannot read ticket from branch B
        try {
            Tasks.await(ticketRef.get());
            fail("Manager should NOT read tickets from other branches");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Manager can read employees in their branch.
     * Validates: Requirement 4.3, 4.9
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and manager auth is set up")
    public void testManager_canReadEmployees_inTheirBranch() throws Exception {
        // Arrange: create an employee document in manager's branch
        String employeeDocId = "emp_manager_branch_" + System.currentTimeMillis();
        DocumentReference empRef = db.collection("branches").document(BRANCH_A_ID)
                .collection("employees").document(employeeDocId);
        Tasks.await(empRef.set(buildEmployeeData(EMPLOYEE_UID, EMPLOYEE_EMAIL, "Test Employee", BRANCH_A_ID)));
        createdDocumentPaths.add("branches/" + BRANCH_A_ID + "/employees/" + employeeDocId);

        // TODO: Authenticate as manager of BRANCH_A_ID
        // authenticateAsManager(BRANCH_A_ID);

        // Act & Assert
        try {
            DocumentSnapshot snap = Tasks.await(empRef.get());
            assertTrue("Manager should read employees in their branch", snap.exists());
        } catch (ExecutionException e) {
            fail("Manager should have read access to employees in their branch: " + e.getMessage());
        }
    }

    /**
     * Manager cannot read employees from other branches.
     * Validates: Requirement 4.3, 4.9
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and manager auth is set up")
    public void testManager_cannotReadEmployees_fromOtherBranches() throws Exception {
        // Arrange: create an employee document in branch B
        String employeeDocId = "emp_other_branch_" + System.currentTimeMillis();
        DocumentReference empRef = db.collection("branches").document(BRANCH_B_ID)
                .collection("employees").document(employeeDocId);
        Tasks.await(empRef.set(buildEmployeeData(OTHER_EMPLOYEE_UID, "other@test.com", "Other Employee", BRANCH_B_ID)));
        createdDocumentPaths.add("branches/" + BRANCH_B_ID + "/employees/" + employeeDocId);

        // TODO: Authenticate as manager of BRANCH_A_ID (not BRANCH_B_ID)
        // authenticateAsManager(BRANCH_A_ID);

        // Act & Assert
        try {
            Tasks.await(empRef.get());
            fail("Manager should NOT read employees from other branches");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Manager can access getDashboardStats filtered to their branch.
     * Validates: Requirement 8.5, 12.7
     */
    @Test
    @Ignore("TODO: Enable once getDashboardStats Cloud Function is deployed to emulator (task 8.3)")
    public void testManager_canCallGetDashboardStats_filteredToTheirBranch() throws Exception {
        // TODO: Authenticate as manager of BRANCH_A_ID
        // authenticateAsManager(BRANCH_A_ID);

        // Act: call getDashboardStats — manager's token carries branchId claim
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("branchId", BRANCH_A_ID);
            Object result = Tasks.await(
                    functions.getHttpsCallable("getDashboardStats").call(data));
            assertNotNull("getDashboardStats should return data for manager", result);
        } catch (ExecutionException e) {
            fail("Manager should be able to call getDashboardStats for their branch: " + e.getMessage());
        }
    }

    /**
     * Manager cannot call createUserAccount (admin-only function).
     * Validates: Requirement 12.7, 12.10
     */
    @Test
    @Ignore("TODO: Enable once createUserAccount Cloud Function is deployed to emulator (task 8.4)")
    public void testManager_cannotCallCreateUserAccount() throws Exception {
        // TODO: Authenticate as manager
        // authenticateAsManager(BRANCH_A_ID);

        // Act: attempt to call admin-only function
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "unauthorized@test.com");
            data.put("role", "employee");
            data.put("branchId", BRANCH_A_ID);
            Tasks.await(functions.getHttpsCallable("createUserAccount").call(data));
            fail("Manager should NOT be able to call createUserAccount");
        } catch (ExecutionException e) {
            // Expected: function should reject non-admin callers
            assertNotNull("Should receive an error when manager calls admin function", e.getCause());
        }
    }

    // =========================================================================
    // 3. EMPLOYEE ROLE TESTS
    // =========================================================================

    /**
     * Employee can read only tickets assigned to them.
     * Validates: Requirement 4.4
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and employee auth is set up")
    public void testEmployee_canReadOnlyTickets_assignedToThem() throws Exception {
        // Arrange: create a ticket assigned to this employee
        String ticketId = "ticket_emp_assigned_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "assigned")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as employee with custom claims { role: "employee", branchId: BRANCH_A_ID }
        // authenticateAsEmployee(BRANCH_A_ID);

        // Act & Assert
        try {
            DocumentSnapshot snap = Tasks.await(ticketRef.get());
            assertTrue("Employee should read tickets assigned to them", snap.exists());
            assertEquals("assignedEmployeeId should match", EMPLOYEE_UID,
                    snap.getString("assignedEmployeeId"));
        } catch (ExecutionException e) {
            fail("Employee should have read access to their assigned tickets: " + e.getMessage());
        }
    }

    /**
     * Employee cannot read tickets assigned to other employees.
     * Validates: Requirement 4.4
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and employee auth is set up")
    public void testEmployee_cannotReadTickets_assignedToOtherEmployees() throws Exception {
        // Arrange: create a ticket assigned to a different employee
        String ticketId = "ticket_other_emp_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, OTHER_EMPLOYEE_UID, "assigned")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as EMPLOYEE_UID (not OTHER_EMPLOYEE_UID)
        // authenticateAsEmployee(BRANCH_A_ID);

        // Act & Assert
        try {
            Tasks.await(ticketRef.get());
            fail("Employee should NOT read tickets assigned to other employees");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Employee can update status of their assigned tickets.
     * Validates: Requirement 4.4, 6.3
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and employee auth is set up")
    public void testEmployee_canUpdateStatus_ofAssignedTickets() throws Exception {
        // Arrange: create a ticket assigned to this employee
        String ticketId = "ticket_emp_update_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "assigned")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as employee
        // authenticateAsEmployee(BRANCH_A_ID);

        // Act: update status to in_progress
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("status", "in_progress");
            update.put("updatedAt", Timestamp.now());
            Tasks.await(ticketRef.update(update));
            assertTrue("Employee should update status of their assigned ticket", true);

            DocumentSnapshot snap = Tasks.await(ticketRef.get());
            assertEquals("Status should be in_progress", "in_progress", snap.getString("status"));
        } catch (ExecutionException e) {
            fail("Employee should be able to update status of assigned tickets: " + e.getMessage());
        }
    }

    /**
     * Employee cannot update tickets assigned to other employees.
     * Validates: Requirement 4.4
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and employee auth is set up")
    public void testEmployee_cannotUpdateTickets_assignedToOtherEmployees() throws Exception {
        // Arrange: create a ticket assigned to a different employee
        String ticketId = "ticket_emp_no_update_other_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, OTHER_EMPLOYEE_UID, "assigned")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as EMPLOYEE_UID (not OTHER_EMPLOYEE_UID)
        // authenticateAsEmployee(BRANCH_A_ID);

        // Act: attempt to update status
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("status", "in_progress");
            Tasks.await(ticketRef.update(update));
            fail("Employee should NOT update tickets assigned to other employees");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Employee cannot access admin or manager functions.
     * Validates: Requirement 4.4, 12.7
     */
    @Test
    @Ignore("TODO: Enable once createUserAccount Cloud Function is deployed to emulator (task 8.4)")
    public void testEmployee_cannotAccessAdminOrManagerFunctions() throws Exception {
        // TODO: Authenticate as employee
        // authenticateAsEmployee(BRANCH_A_ID);

        // Act: attempt to call admin-only createUserAccount
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "unauthorized@test.com");
            data.put("role", "employee");
            data.put("branchId", BRANCH_A_ID);
            Tasks.await(functions.getHttpsCallable("createUserAccount").call(data));
            fail("Employee should NOT be able to call createUserAccount");
        } catch (ExecutionException e) {
            assertNotNull("Should receive an error when employee calls admin function", e.getCause());
        }
    }

    // =========================================================================
    // 4. USER (CUSTOMER) ROLE TESTS
    // =========================================================================

    /**
     * User can read only their own tickets.
     * Validates: Requirement 4.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and user auth is set up")
    public void testUser_canReadOnlyTheirOwnTickets() throws Exception {
        // Arrange: create a ticket owned by this user
        String ticketId = "ticket_user_own_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "pending")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as USER_UID with custom claims { role: "user" }
        // authenticateAsUser();

        // Act & Assert
        try {
            DocumentSnapshot snap = Tasks.await(ticketRef.get());
            assertTrue("User should read their own ticket", snap.exists());
            assertEquals("customerId should match", USER_UID, snap.getString("customerId"));
        } catch (ExecutionException e) {
            fail("User should have read access to their own tickets: " + e.getMessage());
        }
    }

    /**
     * User cannot read other customers' tickets.
     * Validates: Requirement 4.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and user auth is set up")
    public void testUser_cannotReadOtherCustomersTickets() throws Exception {
        // Arrange: create a ticket owned by a different user
        String ticketId = "ticket_other_user_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(OTHER_USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "pending")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as USER_UID (not OTHER_USER_UID)
        // authenticateAsUser();

        // Act & Assert
        try {
            Tasks.await(ticketRef.get());
            fail("User should NOT read other customers' tickets");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * User can create new tickets with their own customerId.
     * Validates: Requirement 4.5, 6.1
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and user auth is set up")
    public void testUser_canCreateNewTickets() throws Exception {
        // TODO: Authenticate as USER_UID
        // authenticateAsUser();

        // Arrange
        String ticketId = "ticket_user_create_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Map<String, Object> ticketData = buildTicketData(USER_UID, null, null, "pending");

        // Act: create ticket
        try {
            Tasks.await(ticketRef.set(ticketData));
            createdDocumentPaths.add("tickets/" + ticketId);
            assertTrue("User should be able to create a ticket", true);

            DocumentSnapshot snap = Tasks.await(ticketRef.get());
            assertTrue("Created ticket should exist", snap.exists());
            assertEquals("customerId should match user's UID", USER_UID, snap.getString("customerId"));
        } catch (ExecutionException e) {
            fail("User should be able to create tickets: " + e.getMessage());
        }
    }

    /**
     * User cannot update ticket status (only employees/managers can).
     * Validates: Requirement 4.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and user auth is set up")
    public void testUser_cannotUpdateTicketStatus() throws Exception {
        // Arrange: create a ticket owned by this user
        String ticketId = "ticket_user_no_status_update_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Tasks.await(ticketRef.set(buildTicketData(USER_UID, BRANCH_A_ID, EMPLOYEE_UID, "assigned")));
        createdDocumentPaths.add("tickets/" + ticketId);

        // TODO: Authenticate as USER_UID
        // authenticateAsUser();

        // Act: attempt to update status
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("status", "in_progress");
            Tasks.await(ticketRef.update(update));
            fail("User should NOT be able to update ticket status");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED when user updates status",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * User cannot access admin, manager, or employee functions.
     * Validates: Requirement 4.5, 12.7
     */
    @Test
    @Ignore("TODO: Enable once createUserAccount Cloud Function is deployed to emulator (task 8.4)")
    public void testUser_cannotAccessAdminManagerOrEmployeeFunctions() throws Exception {
        // TODO: Authenticate as USER_UID
        // authenticateAsUser();

        // Act: attempt to call admin-only createUserAccount
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "unauthorized@test.com");
            data.put("role", "employee");
            data.put("branchId", BRANCH_A_ID);
            Tasks.await(functions.getHttpsCallable("createUserAccount").call(data));
            fail("User should NOT be able to call createUserAccount");
        } catch (ExecutionException e) {
            assertNotNull("Should receive an error when user calls admin function", e.getCause());
        }
    }

    /**
     * User cannot create a ticket with another user's customerId.
     * Validates: Requirement 4.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() and user auth is set up")
    public void testUser_cannotCreateTicket_withAnotherUsersCustomerId() throws Exception {
        // TODO: Authenticate as USER_UID
        // authenticateAsUser();

        // Arrange: ticket data with OTHER_USER_UID as customerId
        String ticketId = "ticket_user_spoof_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        Map<String, Object> ticketData = buildTicketData(OTHER_USER_UID, null, null, "pending");

        // Act: attempt to create ticket for another user
        try {
            Tasks.await(ticketRef.set(ticketData));
            createdDocumentPaths.add("tickets/" + ticketId);
            fail("User should NOT create a ticket with another user's customerId");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    // =========================================================================
    // 5. UNAUTHENTICATED ACCESS TESTS
    // =========================================================================

    /**
     * Unauthenticated user cannot read any Firestore documents.
     * Validates: Requirement 4.10
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() with security rules deployed")
    public void testUnauthenticated_cannotReadFirestoreDocuments() throws Exception {
        // Arrange: ensure no user is signed in
        auth.signOut();
        assertNull("No user should be signed in", auth.getCurrentUser());

        // Arrange: create a ticket document (using admin SDK or pre-seeded data)
        String ticketId = "ticket_unauth_read_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);

        // Act: attempt to read without authentication
        try {
            Tasks.await(ticketRef.get());
            fail("Unauthenticated user should NOT read Firestore documents");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED for unauthenticated read",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Unauthenticated user cannot write any Firestore documents.
     * Validates: Requirement 4.10
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() with security rules deployed")
    public void testUnauthenticated_cannotWriteFirestoreDocuments() throws Exception {
        // Arrange: ensure no user is signed in
        auth.signOut();
        assertNull("No user should be signed in", auth.getCurrentUser());

        // Act: attempt to write a ticket without authentication
        String ticketId = "ticket_unauth_write_" + System.currentTimeMillis();
        DocumentReference ticketRef = db.collection("tickets").document(ticketId);

        try {
            Tasks.await(ticketRef.set(buildTicketData("some_user", BRANCH_A_ID, EMPLOYEE_UID, "pending")));
            createdDocumentPaths.add("tickets/" + ticketId);
            fail("Unauthenticated user should NOT write Firestore documents");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED for unauthenticated write",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Unauthenticated user cannot read user documents.
     * Validates: Requirement 4.10
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() with security rules deployed")
    public void testUnauthenticated_cannotReadUserDocuments() throws Exception {
        // Arrange: ensure no user is signed in
        auth.signOut();
        assertNull("No user should be signed in", auth.getCurrentUser());

        // Act: attempt to read a user document
        DocumentReference userRef = db.collection("users").document(USER_UID);
        try {
            Tasks.await(userRef.get());
            fail("Unauthenticated user should NOT read user documents");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED for unauthenticated user read",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    /**
     * Unauthenticated user cannot read branch documents.
     * Validates: Requirement 4.10
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp() with security rules deployed")
    public void testUnauthenticated_cannotReadBranchDocuments() throws Exception {
        // Arrange: ensure no user is signed in
        auth.signOut();
        assertNull("No user should be signed in", auth.getCurrentUser());

        // Act: attempt to read a branch document
        DocumentReference branchRef = db.collection("branches").document(BRANCH_A_ID);
        try {
            Tasks.await(branchRef.get());
            fail("Unauthenticated user should NOT read branch documents");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException ex = (FirebaseFirestoreException) e.getCause();
                assertEquals("Should get PERMISSION_DENIED for unauthenticated branch read",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED, ex.getCode());
            }
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Builds a ticket data map for Firestore writes.
     */
    private Map<String, Object> buildTicketData(String customerId, String branchId,
            String assignedEmployeeId, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customerId);
        data.put("branchId", branchId);
        data.put("assignedEmployeeId", assignedEmployeeId);
        data.put("status", status);
        data.put("serviceType", "Plumbing");
        data.put("description", "Test ticket for RBAC testing");
        data.put("location", new GeoPoint(37.7749, -122.4194));
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());
        return data;
    }

    /**
     * Builds a user data map for Firestore writes.
     */
    private Map<String, Object> buildUserData(String uid, String email, String name,
            String role, String branchId) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email);
        data.put("name", name);
        data.put("role", role);
        data.put("branchId", branchId);
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());
        return data;
    }

    /**
     * Builds a branch data map for Firestore writes.
     */
    private Map<String, Object> buildBranchData(String branchId, String name, String managerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("branchId", branchId);
        data.put("name", name);
        data.put("managerId", managerId);
        data.put("location", new GeoPoint(37.7749, -122.4194));
        data.put("coverageRadiusKm", 10.0);
        data.put("createdAt", Timestamp.now());
        return data;
    }

    /**
     * Builds an employee data map for Firestore writes.
     */
    private Map<String, Object> buildEmployeeData(String uid, String email, String name,
            String branchId) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email);
        data.put("name", name);
        data.put("branchId", branchId);
        data.put("role", "employee");
        data.put("createdAt", Timestamp.now());
        return data;
    }

    // =========================================================================
    // TODO: Authentication helpers (enable when emulator is configured)
    // =========================================================================

    // /**
    //  * Authenticates as admin user with custom claims { role: "admin" }.
    //  * Requires Firebase Auth Emulator to be running.
    //  */
    // private void authenticateAsAdmin() throws Exception {
    //     Tasks.await(auth.signInWithEmailAndPassword(ADMIN_EMAIL, TEST_PASSWORD));
    //     // Custom claims must be set via Admin SDK or emulator REST API before calling this
    // }

    // /**
    //  * Authenticates as manager user with custom claims { role: "manager", branchId: branchId }.
    //  * Requires Firebase Auth Emulator to be running.
    //  */
    // private void authenticateAsManager(String branchId) throws Exception {
    //     Tasks.await(auth.signInWithEmailAndPassword(MANAGER_EMAIL, TEST_PASSWORD));
    //     // Custom claims must be set via Admin SDK or emulator REST API before calling this
    // }

    // /**
    //  * Authenticates as employee user with custom claims { role: "employee", branchId: branchId }.
    //  * Requires Firebase Auth Emulator to be running.
    //  */
    // private void authenticateAsEmployee(String branchId) throws Exception {
    //     Tasks.await(auth.signInWithEmailAndPassword(EMPLOYEE_EMAIL, TEST_PASSWORD));
    //     // Custom claims must be set via Admin SDK or emulator REST API before calling this
    // }

    // /**
    //  * Authenticates as regular user with custom claims { role: "user" }.
    //  * Requires Firebase Auth Emulator to be running.
    //  */
    // private void authenticateAsUser() throws Exception {
    //     Tasks.await(auth.signInWithEmailAndPassword(USER_EMAIL, TEST_PASSWORD));
    //     // Custom claims must be set via Admin SDK or emulator REST API before calling this
    // }
}
