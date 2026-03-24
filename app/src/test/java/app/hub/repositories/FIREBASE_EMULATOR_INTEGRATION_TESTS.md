# Firebase Emulator Integration Tests Guide

## Overview

This guide explains how to set up and run integration tests for Firebase repositories using the Firebase Emulator Suite. These tests validate CRUD operations, real-time listeners, pagination, and query filters for all repository classes.

**Validates: Requirement 19.3**

## Test Coverage

The integration test suite includes:

### TicketRepositoryIntegrationTest
- ✅ CRUD operations (create, read, update, delete)
- ✅ Real-time listener behavior for user/employee/branch tickets
- ✅ Pagination with cursor-based navigation
- ✅ Query filters (by status, date range, user/employee/branch)

### UserRepositoryIntegrationTest
- ✅ User document creation and retrieval
- ✅ User profile updates
- ✅ FCM token management (add, update, duplicate handling)
- ✅ Real-time user data updates
- ✅ Role-based user creation (admin, manager, employee, user)

### BranchRepositoryIntegrationTest
- ✅ Branch CRUD operations
- ✅ Employee management in subcollections
- ✅ Real-time branch updates
- ✅ GeoPoint location handling
- ✅ Employee specializations and availability tracking

### PaymentRepositoryIntegrationTest
- ✅ Payment creation in ticket subcollections
- ✅ Payment confirmation and status updates
- ✅ Payment retrieval for tickets
- ✅ User payment history with real-time updates
- ✅ Multiple payment methods (cash, credit_card, digital_wallet)

## Prerequisites

1. **Node.js and npm**: Required for Firebase CLI
   ```bash
   # Check if installed
   node --version
   npm --version
   ```

2. **Firebase CLI**: Install globally
   ```bash
   npm install -g firebase-tools
   ```

3. **Java Development Kit (JDK)**: Version 17 or higher
   ```bash
   java -version
   ```

4. **Android SDK**: Configured in your development environment

## Setup Instructions

### Step 1: Initialize Firebase Emulators

Navigate to your project root directory and initialize the Firebase emulators:

```bash
# Login to Firebase (if not already logged in)
firebase login

# Initialize Firebase project (if not already initialized)
firebase init

# Select the following options:
# - Firestore: Configure security rules and indexes files
# - Emulators: Set up local emulators for Firebase products

# When prompted, select:
# - Firestore Emulator
# - Authentication Emulator (optional, for auth tests)
```

This creates:
- `firebase.json` - Firebase configuration
- `firestore.rules` - Security rules (for future use)
- `firestore.indexes.json` - Index definitions

### Step 2: Configure Emulator Ports

Edit `firebase.json` to configure emulator ports:

```json
{
  "emulators": {
    "firestore": {
      "port": 8080
    },
    "auth": {
      "port": 9099
    },
    "ui": {
      "enabled": true,
      "port": 4000
    }
  }
}
```

### Step 3: Start Firebase Emulators

Start the emulator suite:

```bash
# Start all configured emulators
firebase emulators:start

# Or start specific emulators
firebase emulators:start --only firestore,auth
```

You should see output like:
```
✔  firestore: Emulator started at http://localhost:8080
✔  auth: Emulator started at http://localhost:9099
✔  Emulator UI running at http://localhost:4000
```

**Keep this terminal window open** - the emulators must be running during test execution.

### Step 4: Configure Tests to Use Emulator

In each test class, uncomment the emulator configuration in the `setUp()` method:

```java
@Before
public void setUp() {
    // Configure Firestore to use emulator
    FirebaseFirestore.getInstance().useEmulator("localhost", 8080);
    
    db = FirebaseFirestore.getInstance();
    repository = new TicketRepository();
    createdTicketIds = new ArrayList<>();
}
```

**Important**: This configuration must be done BEFORE any Firestore operations.

### Step 5: Update Repository Classes

When implementing the repository classes, ensure they support emulator configuration:

```java
public class TicketRepository {
    private FirebaseFirestore db;
    
    public TicketRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    // For testing with emulator
    public TicketRepository(FirebaseFirestore db) {
        this.db = db;
    }
    
    // Repository methods...
}
```

## Running the Tests

### Run All Integration Tests

```bash
# From project root
./gradlew test --tests "app.hub.repositories.*IntegrationTest"
```

### Run Specific Test Class

```bash
# Ticket repository tests
./gradlew test --tests TicketRepositoryIntegrationTest

# User repository tests
./gradlew test --tests UserRepositoryIntegrationTest

# Branch repository tests
./gradlew test --tests BranchRepositoryIntegrationTest

# Payment repository tests
./gradlew test --tests PaymentRepositoryIntegrationTest
```

### Run Specific Test Method

```bash
./gradlew test --tests TicketRepositoryIntegrationTest.testCreateTicket_withValidData_createsDocument
```

### Run with Verbose Output

```bash
./gradlew test --tests "app.hub.repositories.*IntegrationTest" --info
```

## Test Execution Workflow

1. **Start Emulators**: `firebase emulators:start`
2. **Run Tests**: `./gradlew test --tests "app.hub.repositories.*IntegrationTest"`
3. **View Results**: Check console output or `app/build/reports/tests/test/index.html`
4. **Stop Emulators**: Press `Ctrl+C` in the emulator terminal

## Emulator UI

The Firebase Emulator UI provides a web interface to inspect data:

1. Open browser to `http://localhost:4000`
2. Navigate to **Firestore** tab
3. View collections, documents, and subcollections created during tests
4. Manually inspect data structure and values

This is useful for:
- Debugging test failures
- Verifying data structure
- Understanding query results
- Inspecting subcollections (employees, payments)

## Troubleshooting

### Issue: Tests fail with "Connection refused"

**Solution**: Ensure Firebase emulators are running before executing tests.

```bash
# In a separate terminal
firebase emulators:start
```

### Issue: Tests fail with "FirebaseApp not initialized"

**Solution**: Ensure Firebase is properly initialized in your test setup. Add to `setUp()`:

```java
@Before
public void setUp() {
    // Initialize Firebase if not already initialized
    if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context);
    }
    
    FirebaseFirestore.getInstance().useEmulator("localhost", 8080);
    // ... rest of setup
}
```

### Issue: Data persists between test runs

**Solution**: The emulator clears data when restarted. To clear data without restarting:

```bash
# Clear all emulator data
firebase emulators:start --import=./emulator-data --export-on-exit
```

Or use the `@After` cleanup methods in tests (already implemented).

### Issue: Port already in use

**Solution**: Change the port in `firebase.json` or kill the process using the port:

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Issue: Tests timeout

**Solution**: Increase test timeout in `build.gradle.kts`:

```kotlin
testOptions {
    unitTests {
        all {
            it.testLogging {
                events("passed", "skipped", "failed")
            }
            it.timeout.set(Duration.ofMinutes(5))
        }
    }
}
```

## Best Practices

### 1. Test Isolation

Each test should:
- Create its own test data
- Clean up after execution (implemented in `@After` methods)
- Not depend on other tests

### 2. Descriptive Test Names

Use the pattern: `test<Method>_<Condition>_<ExpectedResult>`

Example: `testCreateTicket_withValidData_createsDocument`

### 3. Arrange-Act-Assert Pattern

```java
@Test
public void testExample() {
    // Arrange - Set up test data
    Ticket ticket = createSampleTicket();
    
    // Act - Execute the operation
    DocumentReference ref = repository.createTicket(ticket);
    
    // Assert - Verify the result
    assertNotNull(ref);
}
```

### 4. Use Helper Methods

Create helper methods for common operations:
- `createSampleTicket()`
- `createSampleUser()`
- `createSampleBranch()`

This reduces code duplication and improves readability.

### 5. Test Real-Time Listeners

For LiveData/real-time listener tests:
- Use `CountDownLatch` to wait for async updates
- Set reasonable timeouts (5 seconds)
- Verify both initial load and subsequent updates

### 6. Clean Up Resources

Always clean up created documents in `@After` methods to prevent data accumulation.

## CI/CD Integration

To run these tests in CI/CD pipelines:

### GitHub Actions Example

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          
      - name: Install Firebase CLI
        run: npm install -g firebase-tools
        
      - name: Start Firebase Emulators
        run: firebase emulators:start --only firestore,auth &
        
      - name: Wait for Emulators
        run: sleep 10
        
      - name: Run Integration Tests
        run: ./gradlew test --tests "app.hub.repositories.*IntegrationTest"
        
      - name: Upload Test Reports
        uses: actions/upload-artifact@v2
        if: always()
        with:
          name: test-reports
          path: app/build/reports/tests/
```

## Next Steps

Once repository classes are implemented:

1. **Uncomment emulator configuration** in all test `setUp()` methods
2. **Run tests** to verify repository implementations
3. **Fix any failing tests** by adjusting repository code
4. **Add additional test cases** for edge cases specific to your implementation
5. **Integrate into CI/CD** pipeline for automated testing

## Additional Resources

- [Firebase Emulator Suite Documentation](https://firebase.google.com/docs/emulator-suite)
- [Firestore Testing Guide](https://firebase.google.com/docs/firestore/security/test-rules-emulator)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)

## Test Execution Checklist

Before running integration tests:

- [ ] Firebase CLI installed (`firebase --version`)
- [ ] Emulators initialized (`firebase.json` exists)
- [ ] Emulators running (`firebase emulators:start`)
- [ ] Emulator configuration uncommented in test `setUp()` methods
- [ ] Repository classes implemented (or stubs created)
- [ ] Model classes (Ticket, User, Branch, Payment, Employee) implemented

After running tests:

- [ ] All tests pass
- [ ] Test coverage meets requirements (80%+)
- [ ] No data leaks (cleanup methods work correctly)
- [ ] Emulator UI shows expected data structure
- [ ] Test reports generated (`app/build/reports/tests/test/index.html`)

## Summary

These integration tests provide comprehensive coverage of Firebase repository operations using the Firebase Emulator Suite. They validate:

- **CRUD operations** for all repositories
- **Real-time listener behavior** for live data updates
- **Pagination functionality** with cursor-based navigation
- **Query filters** for status, date range, and role-based access
- **Subcollection management** for employees and payments
- **Edge cases** like empty data, null values, and boundary conditions

By running these tests against the Firebase Emulator, you can verify repository implementations without incurring Firebase costs or affecting production data.
