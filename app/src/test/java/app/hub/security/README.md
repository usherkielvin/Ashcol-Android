# Firebase Security Rules Tests

## Overview

This directory contains unit tests for Firebase Security Rules validation using the Firebase Emulator Suite. This includes both Firestore Security Rules and Cloud Storage Security Rules.

**Tasks**: 
- 9.2 Write unit tests for Firestore Security Rules using Firebase Emulator
- 10.2 Write tests for Cloud Storage Security Rules

**Validates**: Requirements 19.2, 19.8

## Files

### FirestoreSecurityRulesTest.java
Comprehensive test suite with 35+ test cases covering:

1. **User Document Access Control** (8 tests)
   - Own document read/write access
   - Prevention of reading other users' documents
   - Role field protection
   - Admin access to all documents

2. **Ticket Access by Role** (9 tests)
   - Admin: full access to all tickets
   - Manager: access to tickets in their branch only
   - Employee: access to assigned tickets only
   - User: access to own tickets only
   - Ticket creation validation

3. **Status Transition Validation** (5 tests)
   - Valid transitions: assigned → in_progress, in_progress → completed/cancelled
   - Invalid transitions: pending → completed, completed → in_progress

4. **Payment Access Control** (6 tests)
   - Role-based payment document access
   - Payment confirmation by ticket owner
   - Prevention of accessing other users' payments

5. **Branch and Employee Access** (7 tests)
   - Read access for all authenticated users
   - Admin-only branch creation/updates
   - Manager employee management in their branch
   - Prevention of cross-branch operations

### SECURITY_RULES_TESTING_GUIDE.md
Detailed guide covering:
- Prerequisites and setup instructions
- Firebase Emulator configuration
- Authentication helper implementation
- Running tests and troubleshooting
- CI/CD integration examples
- Best practices

### StorageSecurityRulesTest.java
Comprehensive test suite with 19 test cases covering:

1. **Image Upload Size Limits** (3 tests)
   - Upload under 10MB succeeds
   - Upload at exactly 10MB succeeds
   - Upload over 10MB fails

2. **File Type Validation** (4 tests)
   - JPEG image upload succeeds
   - PNG image upload succeeds
   - Non-image file upload fails
   - Executable file upload fails

3. **Ticket Image Access Control** (5 tests)
   - Authenticated user can upload ticket images
   - Unauthenticated user cannot upload
   - Authenticated user can read ticket images
   - Only admin can delete ticket images
   - Invalid path structure rejected

4. **Profile Photo Access Control** (7 tests)
   - User can upload own profile photo
   - User cannot upload other user's profile photo
   - Anyone can read profile photos (public)
   - User can delete own profile photo
   - User cannot delete other user's profile photo
   - Profile photo must be valid image type
   - Profile photo must respect size limit

### STORAGE_RULES_TESTING_GUIDE.md
Detailed guide covering:
- Prerequisites and setup instructions
- Firebase Storage Emulator configuration
- Storage rules implementation
- Authentication helper implementation
- Running tests and troubleshooting
- CI/CD integration examples
- Best practices for storage testing

## Quick Start

### 1. Prerequisites
```bash
# Install Firebase CLI
npm install -g firebase-tools

# Verify installation
firebase --version
```

### 2. Setup
```bash
# Create firestore.rules and storage.rules files in project root (see design.md)
# Configure firebase.json

# Start emulators
firebase emulators:start
```

### 3. Run Tests
```bash
# Run all Firestore security rules tests
./gradlew test --tests FirestoreSecurityRulesTest

# Run all Storage security rules tests
./gradlew test --tests StorageSecurityRulesTest

# Run all security tests
./gradlew test --tests "app.hub.security.*"

# Run specific test
./gradlew test --tests FirestoreSecurityRulesTest.testUserCanReadOwnDocument
./gradlew test --tests StorageSecurityRulesTest.testUploadImageUnderSizeLimit
```

## Test Status

**Firestore Security Rules Tests**:
- Status: ✅ Implemented (Ready for execution once Task 9.1 is complete)
- Dependencies: Task 9.1 (Create firestore.rules file)

**Storage Security Rules Tests**:
- Status: ✅ Implemented (Ready for execution once Task 10.1 is complete)
- Dependencies: Task 10.1 (Create storage.rules file)

**Common Dependencies**:
- Firebase Emulator Suite running
- Authentication helper implementation

**Notes**:
- Tests are currently marked with TODO comments for emulator configuration
- Authentication helper method needs implementation (see guides)
- Tests will validate security rules once Tasks 9.1 and 10.1 are complete

## Test Coverage Summary

### Firestore Security Rules Tests

| Category | Test Count | Requirements |
|----------|-----------|--------------|
| User Access Control | 8 | 4.1, 4.8 |
| Ticket Access by Role | 9 | 4.2, 4.3, 4.4, 4.5 |
| Status Transitions | 5 | 4.7 |
| Payment Access | 6 | 9.7, 9.8 |
| Branch/Employee Access | 7 | 4.9 |
| **Subtotal** | **35** | **19.2, 19.8** |

### Storage Security Rules Tests

| Category | Test Count | Requirements |
|----------|-----------|--------------|
| Image Upload Size Limits | 3 | 5.5 |
| File Type Validation | 4 | 5.6 |
| Ticket Image Access Control | 5 | 5.8 |
| Profile Photo Access Control | 7 | 5.9 |
| **Subtotal** | **19** | **19.2** |

### Total Coverage

| Test Suite | Test Count | Requirements |
|-----------|-----------|--------------|
| Firestore Security Rules | 35 | 4.1-4.9, 9.7-9.8, 19.2, 19.8 |
| Storage Security Rules | 19 | 5.5, 5.6, 5.8, 5.9, 19.2 |
| **Grand Total** | **54** | **Multiple** |

## Next Steps

### For Firestore Security Rules Tests
1. Complete Task 9.1 (Create firestore.rules file)
2. Uncomment emulator configuration in test setUp() method
3. Implement authenticateAs() helper method
4. Run tests to validate security rules
5. Fix any failing tests by adjusting rules
6. Integrate into CI/CD pipeline

### For Storage Security Rules Tests
1. Complete Task 10.1 (Create storage.rules file)
2. Uncomment emulator configuration in test setUp() method
3. Implement authenticateAs() helper method
4. Run tests to validate storage rules
5. Fix any failing tests by adjusting rules
6. Integrate into CI/CD pipeline

## Related Documentation

- [SECURITY_RULES_TESTING_GUIDE.md](./SECURITY_RULES_TESTING_GUIDE.md) - Detailed Firestore rules setup and execution guide
- [STORAGE_RULES_TESTING_GUIDE.md](./STORAGE_RULES_TESTING_GUIDE.md) - Detailed Storage rules setup and execution guide
- [Design Document](../../../../.kiro/specs/laravel-to-firebase-migration/design.md) - Security rules specification
- [Requirements Document](../../../../.kiro/specs/laravel-to-firebase-migration/requirements.md) - Access control requirements
- [Firebase Emulator Integration Tests Guide](../../repositories/FIREBASE_EMULATOR_INTEGRATION_TESTS.md) - General emulator setup

## Support

For issues or questions:
1. Check SECURITY_RULES_TESTING_GUIDE.md or STORAGE_RULES_TESTING_GUIDE.md troubleshooting sections
2. Review Firebase Emulator Suite documentation
3. Verify firestore.rules and storage.rules file syntax
4. Check emulator console output for errors
