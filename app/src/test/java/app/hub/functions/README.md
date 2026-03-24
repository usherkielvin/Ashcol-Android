# Cloud Functions Integration Tests

This directory contains integration tests for Firebase Cloud Functions using the Firebase Emulator Suite.

## Quick Start

### Prerequisites
- Cloud Functions implemented (Tasks 8.1-8.5)
- Firebase CLI installed: `npm install -g firebase-tools`
- Firebase Emulators configured

### Run Tests

1. **Start Firebase Emulators:**
   ```bash
   firebase emulators:start
   ```

2. **Run all Cloud Functions tests:**
   ```bash
   ./gradlew test --tests CloudFunctionsIntegrationTest
   ```

3. **Run specific test:**
   ```bash
   ./gradlew test --tests CloudFunctionsIntegrationTest.testAssignTicketToBranch_nearestBranch
   ```

## Test Files

- **`CloudFunctionsIntegrationTest.java`** - Main integration test suite with 20+ test cases
- **`CLOUD_FUNCTIONS_TESTING_GUIDE.md`** - Comprehensive setup and troubleshooting guide
- **`README.md`** - This file (quick reference)

## Test Coverage

### 1. Branch Routing (`assignTicketToBranch`)
- ✓ Nearest branch assignment
- ✓ Multiple branches handling
- ✓ Coverage area constraints
- ✓ Error cases (no branches, invalid coordinates)
- ✓ Performance (< 5 seconds with 100 branches)

### 2. Dashboard Statistics (`getDashboardStats`)
- ✓ Admin role (all branches)
- ✓ Manager role (branch filtering)
- ✓ Ticket counts by status
- ✓ Revenue calculation
- ✓ Average completion time
- ✓ Employee workload
- ✓ Empty dataset handling
- ✓ Performance (< 3 seconds)

### 3. User Management
- ✓ Create user account (admin only)
- ✓ Set user role and custom claims
- ✓ Delete user account
- ✓ Role validation
- ✓ Required field validation

### 4. Firestore Triggers
- ✓ `onUserCreate` - Creates user document
- ✓ `onTicketStatusChange` - Sends FCM notification
- ✓ `onTicketDelete` - Cleans up photos
- ✓ `onUserNameChange` - Updates denormalized data

## Current Status

⚠️ **Tests are currently disabled with `@Ignore` annotation**

**Reason:** Cloud Functions not yet implemented (Tasks 8.1-8.5 are queued)

**To enable tests:**
1. Complete Cloud Functions implementation (Tasks 8.1-8.5)
2. Remove `@Ignore` annotation from `CloudFunctionsIntegrationTest.java`
3. Uncomment test code in each test method
4. Run tests to validate functionality

## Requirements Validated

These tests validate the following requirements:
- **Requirement 7.1-7.10:** Branch routing algorithm
- **Requirement 8.1-8.10:** Dashboard statistics aggregation
- **Requirement 12.1-12.10:** Role-based access control with custom claims
- **Requirement 15.1-15.10:** Admin user management
- **Requirement 19.3, 19.9:** Integration testing with Firebase Emulator

## Emulator Configuration

Default emulator ports (configured in `firebase.json`):
- **Functions:** `localhost:5001`
- **Firestore:** `localhost:8080`
- **Auth:** `localhost:9099`
- **Storage:** `localhost:9199`
- **Emulator UI:** `http://localhost:4000`

## Troubleshooting

### Emulator not starting?
```bash
# Check if ports are in use
lsof -i :5001

# Kill existing processes
pkill -f firebase
```

### Tests timing out?
- Increase timeout: `Tasks.await(task, 30, TimeUnit.SECONDS)`
- Check emulator logs for errors
- Verify emulator is running: `curl http://localhost:5001`

### Connection refused?
- Use `10.0.2.2` instead of `localhost` for Android emulator
- Verify emulator configuration in `setUp()` method

## Documentation

For detailed setup instructions, troubleshooting, and best practices, see:
- **[CLOUD_FUNCTIONS_TESTING_GUIDE.md](./CLOUD_FUNCTIONS_TESTING_GUIDE.md)**

## Related Tasks

- **Task 8.1:** Set up Cloud Functions project structure
- **Task 8.2:** Implement `assignTicketToBranch` callable function
- **Task 8.3:** Implement `getDashboardStats` callable function
- **Task 8.4:** Implement user management callable functions
- **Task 8.5:** Implement Firestore trigger functions
- **Task 8.6:** Write integration tests (THIS TASK)

## Next Steps

1. ✅ Integration test structure created
2. ⏳ Implement Cloud Functions (Tasks 8.1-8.5)
3. ⏳ Enable and run tests
4. ⏳ Fix any failing tests
5. ⏳ Add additional edge case tests as needed
6. ⏳ Integrate into CI/CD pipeline
