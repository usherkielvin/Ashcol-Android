# Requirements Document

## Introduction

This document specifies the requirements for migrating an Android service ticket management application from a Laravel + MySQL backend architecture to a pure Firebase serverless architecture. The migration eliminates the need for server hosting while maintaining all existing functionality and adding real-time capabilities. The system supports four user roles (Admin, Manager, Employee, User) with features including authentication, ticket management, photo attachments, branch-based routing, dashboard statistics, payment processing, and push notifications.

## Glossary

- **Firebase_Auth_Module**: The Firebase Authentication service component that handles user authentication via email/password, Google Sign-In, and phone number
- **Firestore_Database**: The Firebase Firestore NoSQL document database that stores all application data in collections
- **Cloud_Storage**: The Firebase Cloud Storage service that stores user-uploaded images and attachments
- **Cloud_Functions**: Firebase Cloud Functions that execute server-side business logic in a serverless environment
- **Security_Rules**: Firestore Security Rules and Storage Rules that enforce access control at the database and storage level
- **Retrofit_Client**: The existing HTTP client library used for Laravel API communication that will be replaced
- **Firebase_SDK**: The Firebase Android SDK that provides direct access to Firebase services
- **Custom_Claims**: Firebase Authentication custom claims that store user role information (admin, manager, employee, user)
- **Service_Ticket**: A work order document containing customer request details, status, assigned employee, and payment information
- **Branch_Document**: A Firestore document representing a service branch with location, coverage area, and assigned staff
- **Real_Time_Listener**: A Firestore snapshot listener that provides live updates when data changes
- **FCM**: Firebase Cloud Messaging service for sending push notifications to Android devices
- **Data_Migration_Script**: A one-time script that transfers existing MySQL data to Firestore collections
- **Role_Based_Access**: Access control mechanism where users can only access data appropriate for their role
- **Compound_Index**: A Firestore index on multiple fields required for complex queries
- **Subcollection**: A nested collection within a Firestore document used for one-to-many relationships
- **Batch_Write**: A Firestore operation that performs multiple writes atomically
- **Transaction**: A Firestore operation that reads and writes data atomically with consistency guarantees
- **Payment_Document**: A Firestore document containing payment amount, method, status, and transaction details
- **Branch_Routing_Algorithm**: Business logic that assigns tickets to the nearest available branch based on location
- **Dashboard_Aggregation**: Cloud Function that calculates statistics by aggregating ticket and payment data
- **Photo_Upload_Handler**: Component that compresses images and uploads them to Cloud Storage with progress tracking

## Requirements

### Requirement 1: Firebase Authentication Integration

**User Story:** As a user, I want to authenticate using email/password, Google Sign-In, or phone number, so that I can securely access the application without relying on Laravel Sanctum tokens.

#### Acceptance Criteria

1. THE Firebase_Auth_Module SHALL support email and password authentication
2. THE Firebase_Auth_Module SHALL support Google Sign-In authentication
3. THE Firebase_Auth_Module SHALL support phone number authentication with SMS verification
4. WHEN a user successfully authenticates, THE Firebase_Auth_Module SHALL generate a Firebase ID token
5. WHEN a user registers, THE Cloud_Functions SHALL create a corresponding user document in Firestore with role set to "user"
6. THE Firebase_Auth_Module SHALL store user role information in Custom_Claims
7. WHEN a user logs out, THE Firebase_Auth_Module SHALL revoke the current session
8. THE Firebase_Auth_Module SHALL support password reset via email
9. WHEN authentication state changes, THE Firebase_SDK SHALL notify registered listeners
10. THE Firebase_Auth_Module SHALL persist authentication state across app restarts

### Requirement 2: Remove Retrofit Dependencies

**User Story:** As a developer, I want to remove all Retrofit HTTP client code, so that the app communicates directly with Firebase services instead of making REST API calls.

#### Acceptance Criteria

1. THE Migration SHALL remove all Retrofit dependencies from build.gradle.kts
2. THE Migration SHALL remove the ApiClient class
3. THE Migration SHALL remove the ApiService interface
4. THE Migration SHALL remove all API request and response model classes in the api package
5. THE Migration SHALL replace all Retrofit Call objects with Firebase SDK method calls
6. THE Migration SHALL remove OkHttp logging interceptor dependencies
7. THE Migration SHALL remove Gson converter dependencies used for Retrofit
8. WHEN the migration is complete, THE Application SHALL compile without Retrofit references

### Requirement 3: Firestore Data Model Design

**User Story:** As a developer, I want a well-structured Firestore data model, so that I can efficiently query and maintain data without SQL joins.

#### Acceptance Criteria

1. THE Firestore_Database SHALL contain a "users" collection with documents keyed by Firebase UID
2. THE Firestore_Database SHALL contain a "tickets" collection with documents containing customer, service, location, status, and payment fields
3. THE Firestore_Database SHALL contain a "branches" collection with documents containing name, location, coverage area, and manager fields
4. THE Firestore_Database SHALL contain an "employees" subcollection under each Branch_Document
5. THE Firestore_Database SHALL contain a "payments" subcollection under each Service_Ticket document
6. THE Firestore_Database SHALL denormalize user name and email into Service_Ticket documents for efficient display
7. THE Firestore_Database SHALL denormalize branch name into employee documents for filtering
8. THE Firestore_Database SHALL store timestamps using Firestore ServerTimestamp for consistency
9. THE Firestore_Database SHALL use document references to link related entities where appropriate
10. THE Firestore_Database SHALL support Compound_Index definitions for multi-field queries

### Requirement 4: Firestore Security Rules Implementation

**User Story:** As a security-conscious developer, I want comprehensive Firestore Security Rules, so that users can only access data appropriate for their role without server-side authorization checks.

#### Acceptance Criteria

1. THE Security_Rules SHALL allow users to read and write only their own user document
2. THE Security_Rules SHALL allow users with role "admin" in Custom_Claims to read and write all documents
3. THE Security_Rules SHALL allow users with role "manager" to read and write tickets and employees in their assigned branch
4. THE Security_Rules SHALL allow users with role "employee" to read tickets assigned to them and update ticket status
5. THE Security_Rules SHALL allow users with role "user" to create tickets and read their own tickets
6. THE Security_Rules SHALL validate that required fields are present in document writes
7. THE Security_Rules SHALL validate that ticket status transitions follow allowed state machine rules
8. THE Security_Rules SHALL prevent users from modifying their own role field
9. THE Security_Rules SHALL allow authenticated users to read the branches collection
10. THE Security_Rules SHALL deny all access to unauthenticated users except for public data

### Requirement 5: Cloud Storage Integration for Photos

**User Story:** As a user, I want to upload photos with my service tickets, so that I can provide visual context for the service request.

#### Acceptance Criteria

1. THE Cloud_Storage SHALL store ticket photos in a "ticket-images/{ticketId}/{filename}" path structure
2. THE Cloud_Storage SHALL store user profile photos in a "profile-photos/{userId}/{filename}" path structure
3. WHEN a user uploads a photo, THE Photo_Upload_Handler SHALL compress the image to reduce file size
4. WHEN a photo upload completes, THE Photo_Upload_Handler SHALL store the download URL in the corresponding Firestore document
5. THE Cloud_Storage SHALL enforce a maximum file size of 10MB per image
6. THE Cloud_Storage SHALL support JPEG and PNG image formats
7. WHEN a ticket is deleted, THE Cloud_Functions SHALL delete associated photos from Cloud_Storage
8. THE Storage Rules SHALL allow users to upload photos only for their own tickets
9. THE Storage Rules SHALL allow employees and managers to read ticket photos for assigned tickets
10. THE Photo_Upload_Handler SHALL report upload progress to the UI

### Requirement 6: Real-Time Ticket Updates

**User Story:** As a user, I want to see live updates when my ticket status changes, so that I don't need to manually refresh the screen.

#### Acceptance Criteria

1. WHEN a user views their ticket list, THE Firebase_SDK SHALL attach a Real_Time_Listener to the tickets query
2. WHEN a ticket document changes in Firestore, THE Real_Time_Listener SHALL notify the UI with the updated data
3. WHEN a ticket status changes, THE UI SHALL update the displayed status without user interaction
4. WHEN a new ticket is created, THE Real_Time_Listener SHALL add it to the displayed list automatically
5. WHEN a ticket is deleted, THE Real_Time_Listener SHALL remove it from the displayed list automatically
6. THE Real_Time_Listener SHALL handle network disconnections gracefully and resume when connectivity returns
7. WHEN the user navigates away from the ticket list, THE Firebase_SDK SHALL detach the Real_Time_Listener
8. THE Real_Time_Listener SHALL use Firestore's local cache to display data immediately while fetching updates
9. WHEN multiple fields change simultaneously, THE Real_Time_Listener SHALL deliver a single update event
10. THE Real_Time_Listener SHALL filter tickets based on user role and assigned branch

### Requirement 7: Branch Routing Algorithm Migration

**User Story:** As a system, I want to assign tickets to the nearest available branch, so that service requests are handled efficiently.

#### Acceptance Criteria

1. WHEN a user creates a ticket with location coordinates, THE Branch_Routing_Algorithm SHALL calculate distance to all branches
2. THE Branch_Routing_Algorithm SHALL use the Haversine formula for distance calculation
3. THE Branch_Routing_Algorithm SHALL assign the ticket to the branch with minimum distance
4. WHERE a branch has a defined coverage area, THE Branch_Routing_Algorithm SHALL only consider branches that cover the ticket location
5. IF no branch covers the ticket location, THEN THE Branch_Routing_Algorithm SHALL assign to the nearest branch regardless of coverage
6. THE Branch_Routing_Algorithm SHALL execute as a Cloud_Functions callable function
7. THE Branch_Routing_Algorithm SHALL return the assigned branch ID and distance
8. WHEN the algorithm completes, THE Cloud_Functions SHALL update the ticket document with the assigned branch
9. THE Branch_Routing_Algorithm SHALL handle cases where no branches exist by returning an error
10. THE Branch_Routing_Algorithm SHALL complete within 5 seconds for up to 100 branches

### Requirement 8: Dashboard Statistics Aggregation

**User Story:** As an admin or manager, I want to view dashboard statistics, so that I can monitor system performance and workload.

#### Acceptance Criteria

1. THE Dashboard_Aggregation SHALL calculate total ticket count by status (pending, in-progress, completed, cancelled)
2. THE Dashboard_Aggregation SHALL calculate total revenue from completed payments
3. THE Dashboard_Aggregation SHALL calculate average ticket completion time
4. THE Dashboard_Aggregation SHALL calculate employee workload (tickets per employee)
5. WHERE the user role is "manager", THE Dashboard_Aggregation SHALL filter statistics by the manager's assigned branch
6. WHERE the user role is "admin", THE Dashboard_Aggregation SHALL include statistics across all branches
7. THE Dashboard_Aggregation SHALL execute as a Cloud_Functions HTTPS callable function
8. THE Dashboard_Aggregation SHALL cache results for 5 minutes to reduce query costs
9. WHEN dashboard data is requested, THE Cloud_Functions SHALL return aggregated statistics within 3 seconds
10. THE Dashboard_Aggregation SHALL handle empty datasets by returning zero values

### Requirement 9: Payment Processing Integration

**User Story:** As a user, I want to complete payments for service tickets, so that I can finalize the transaction.

#### Acceptance Criteria

1. WHEN an employee completes work, THE Cloud_Functions SHALL create a Payment_Document in the ticket's payments subcollection
2. THE Payment_Document SHALL contain amount, payment method, status, and timestamp fields
3. THE Firebase_SDK SHALL support payment methods including cash, credit card, and digital wallet
4. WHEN a user confirms payment, THE Cloud_Functions SHALL update the payment status to "paid"
5. WHEN payment is confirmed, THE Cloud_Functions SHALL update the ticket status to "completed"
6. THE Cloud_Functions SHALL use a Transaction to ensure payment and ticket status update atomically
7. THE Security_Rules SHALL allow only the ticket owner to confirm payment
8. THE Security_Rules SHALL allow employees and managers to view payment details for their assigned tickets
9. WHEN payment fails, THE Cloud_Functions SHALL log the error and keep the ticket status as "pending_payment"
10. THE Payment_Document SHALL store a reference to the employee who completed the work

### Requirement 10: Firebase Cloud Messaging for Notifications

**User Story:** As a user, I want to receive push notifications when my ticket status changes, so that I stay informed without opening the app.

#### Acceptance Criteria

1. WHEN a user installs the app, THE Firebase_SDK SHALL register for FCM and obtain a device token
2. WHEN a device token is obtained, THE Firebase_SDK SHALL store it in the user's Firestore document
3. WHEN a ticket status changes, THE Cloud_Functions SHALL send a push notification to the ticket owner's device
4. WHEN a ticket is assigned to an employee, THE Cloud_Functions SHALL send a push notification to the employee's device
5. THE FCM notification SHALL include the ticket ID, title, and new status in the payload
6. WHEN a user taps a notification, THE Application SHALL open the ticket detail screen for that ticket
7. THE Cloud_Functions SHALL use FCM Admin SDK to send notifications
8. THE Cloud_Functions SHALL handle cases where a user has multiple devices by sending to all registered tokens
9. IF a device token is invalid, THEN THE Cloud_Functions SHALL remove it from the user document
10. THE FCM notification SHALL display a localized message based on the ticket status

### Requirement 11: Data Migration from MySQL to Firestore

**User Story:** As a developer, I want to migrate existing data from MySQL to Firestore, so that users retain their historical tickets and account information.

#### Acceptance Criteria

1. THE Data_Migration_Script SHALL export all users from MySQL and create corresponding Firestore user documents
2. THE Data_Migration_Script SHALL export all tickets from MySQL and create corresponding Firestore ticket documents
3. THE Data_Migration_Script SHALL export all branches from MySQL and create corresponding Firestore branch documents
4. THE Data_Migration_Script SHALL export all employees and create documents in the appropriate branch subcollection
5. THE Data_Migration_Script SHALL export all payments and create documents in the appropriate ticket subcollection
6. THE Data_Migration_Script SHALL map MySQL user IDs to Firebase UIDs using email as the key
7. THE Data_Migration_Script SHALL preserve all timestamps by converting MySQL datetime to Firestore Timestamp
8. THE Data_Migration_Script SHALL use Batch_Write operations to improve migration performance
9. WHEN the migration encounters an error, THE Data_Migration_Script SHALL log the error and continue with remaining records
10. THE Data_Migration_Script SHALL generate a migration report showing success and failure counts

### Requirement 12: Role-Based Access Control with Custom Claims

**User Story:** As an admin, I want to assign roles to users, so that they have appropriate permissions in the application.

#### Acceptance Criteria

1. THE Cloud_Functions SHALL provide an admin-only function to set Custom_Claims for a user
2. THE Custom_Claims SHALL include a "role" field with values "admin", "manager", "employee", or "user"
3. WHERE the role is "manager", THE Custom_Claims SHALL include a "branchId" field
4. WHERE the role is "employee", THE Custom_Claims SHALL include a "branchId" field
5. WHEN Custom_Claims are updated, THE Cloud_Functions SHALL also update the role field in the user's Firestore document
6. THE Firebase_SDK SHALL refresh the ID token to obtain updated Custom_Claims
7. THE Application SHALL check Custom_Claims to determine which UI screens to display
8. THE Security_Rules SHALL use Custom_Claims to enforce access control at the database level
9. WHEN a user's role changes, THE Application SHALL redirect them to the appropriate dashboard
10. THE Cloud_Functions SHALL validate that only users with "admin" role can modify Custom_Claims

### Requirement 13: Offline Data Persistence

**User Story:** As a user, I want to access my tickets when offline, so that I can view information without an internet connection.

#### Acceptance Criteria

1. THE Firebase_SDK SHALL enable Firestore offline persistence by default
2. WHEN the device is offline, THE Firebase_SDK SHALL serve queries from the local cache
3. WHEN the device is offline and a user creates a ticket, THE Firebase_SDK SHALL queue the write operation
4. WHEN connectivity is restored, THE Firebase_SDK SHALL synchronize queued writes to Firestore
5. THE Firebase_SDK SHALL indicate to the UI whether data is from cache or server
6. THE Firebase_SDK SHALL handle write conflicts using Firestore's last-write-wins strategy
7. THE Firebase_SDK SHALL cache documents that the user has recently accessed
8. THE Firebase_SDK SHALL limit cache size to 100MB to conserve device storage
9. WHEN cache size exceeds the limit, THE Firebase_SDK SHALL evict least recently used documents
10. THE Application SHALL display a visual indicator when operating in offline mode

### Requirement 14: Employee Schedule Management

**User Story:** As an employee, I want to view my assigned tickets by date, so that I can plan my work schedule.

#### Acceptance Criteria

1. THE Firestore_Database SHALL store a "scheduled_date" field in Service_Ticket documents
2. WHEN an employee views their schedule, THE Firebase_SDK SHALL query tickets assigned to them ordered by scheduled_date
3. THE Firebase_SDK SHALL support querying tickets for a specific date range
4. THE UI SHALL display tickets grouped by date in a calendar view
5. WHEN a manager assigns a scheduled date, THE Cloud_Functions SHALL validate that the date is in the future
6. THE Security_Rules SHALL allow employees to read tickets with their employee ID in the assigned_employee field
7. THE Security_Rules SHALL allow managers to update the scheduled_date field for tickets in their branch
8. WHEN a scheduled date changes, THE Real_Time_Listener SHALL update the calendar view automatically
9. THE Firebase_SDK SHALL create a Compound_Index on (assigned_employee, scheduled_date) for efficient queries
10. THE UI SHALL allow employees to filter tickets by status within their schedule

### Requirement 15: Admin User Management

**User Story:** As an admin, I want to manage user accounts, so that I can add managers and employees to the system.

#### Acceptance Criteria

1. THE Cloud_Functions SHALL provide an admin-only function to create a new user account
2. WHEN an admin creates a manager account, THE Cloud_Functions SHALL set the role Custom_Claims to "manager"
3. WHEN an admin creates an employee account, THE Cloud_Functions SHALL set the role Custom_Claims to "employee"
4. THE Cloud_Functions SHALL send a password reset email to newly created accounts
5. THE Cloud_Functions SHALL create a user document in Firestore with the specified role and branch assignment
6. THE Cloud_Functions SHALL provide an admin-only function to delete a user account
7. WHEN a user account is deleted, THE Cloud_Functions SHALL remove the user from Firebase Authentication
8. WHEN a user account is deleted, THE Cloud_Functions SHALL delete the user's Firestore document
9. THE Security_Rules SHALL prevent non-admin users from calling user management functions
10. THE Cloud_Functions SHALL validate that required fields (email, role, branch) are provided when creating users

### Requirement 16: Search and Filter Functionality

**User Story:** As a user, I want to search and filter tickets, so that I can quickly find specific service requests.

#### Acceptance Criteria

1. THE Firebase_SDK SHALL support querying tickets by status field
2. THE Firebase_SDK SHALL support querying tickets by service_type field
3. THE Firebase_SDK SHALL support querying tickets by date range using created_at field
4. WHERE the user role is "manager", THE Firebase_SDK SHALL filter tickets by the manager's branch
5. WHERE the user role is "employee", THE Firebase_SDK SHALL filter tickets by the employee's assigned tickets
6. WHERE the user role is "user", THE Firebase_SDK SHALL filter tickets by the user's UID
7. THE Firestore_Database SHALL define Compound_Index for (branch, status, created_at) queries
8. THE Firestore_Database SHALL define Compound_Index for (assigned_employee, status, created_at) queries
9. THE Firestore_Database SHALL define Compound_Index for (customer_id, status, created_at) queries
10. THE UI SHALL allow users to combine multiple filters simultaneously

### Requirement 17: Error Handling and Logging

**User Story:** As a developer, I want comprehensive error handling and logging, so that I can diagnose issues in production.

#### Acceptance Criteria

1. WHEN a Firebase operation fails, THE Firebase_SDK SHALL provide a descriptive error message
2. THE Application SHALL display user-friendly error messages for common Firebase errors
3. THE Application SHALL log Firebase errors to Firebase Crashlytics
4. THE Cloud_Functions SHALL log all function invocations with input parameters
5. WHEN a Cloud_Functions execution fails, THE Cloud_Functions SHALL log the error with stack trace
6. THE Application SHALL handle FirebaseNetworkException by informing the user of connectivity issues
7. THE Application SHALL handle FirebaseAuthException by redirecting to the login screen
8. THE Application SHALL handle FirestoreException by retrying the operation up to 3 times
9. THE Cloud_Functions SHALL use structured logging with severity levels (INFO, WARNING, ERROR)
10. THE Application SHALL send non-fatal errors to Firebase Crashlytics for monitoring

### Requirement 18: Performance Optimization

**User Story:** As a user, I want fast app performance, so that I can complete tasks efficiently.

#### Acceptance Criteria

1. THE Firebase_SDK SHALL use Firestore's local cache to display data within 100ms
2. THE Firebase_SDK SHALL limit query results to 50 documents per page
3. THE Firebase_SDK SHALL implement pagination using Firestore's startAfter cursor
4. THE Photo_Upload_Handler SHALL compress images to maximum 1920x1080 resolution before upload
5. THE Cloud_Functions SHALL use connection pooling for Firestore Admin SDK
6. THE Cloud_Functions SHALL set appropriate memory allocation (256MB minimum) for functions
7. THE Application SHALL lazy-load images in list views using Picasso or Glide
8. THE Firestore_Database SHALL use document references instead of embedding large nested objects
9. THE Firebase_SDK SHALL detach Real_Time_Listener instances when views are destroyed to prevent memory leaks
10. THE Application SHALL use RecyclerView with ViewHolder pattern for efficient list rendering

### Requirement 19: Testing and Validation

**User Story:** As a developer, I want comprehensive tests for Firebase integration, so that I can ensure the migration works correctly.

#### Acceptance Criteria

1. THE Test Suite SHALL include unit tests for Firestore data model classes
2. THE Test Suite SHALL include unit tests for Security_Rules using Firebase Emulator Suite
3. THE Test Suite SHALL include integration tests for Cloud_Functions using Firebase Emulator Suite
4. THE Test Suite SHALL include UI tests for authentication flows
5. THE Test Suite SHALL include UI tests for ticket creation and status updates
6. THE Test Suite SHALL mock Firebase SDK calls in unit tests to avoid network dependencies
7. THE Test Suite SHALL validate that all Compound_Index definitions are deployed
8. THE Test Suite SHALL validate that Security_Rules prevent unauthorized access
9. THE Test Suite SHALL validate that Cloud_Functions handle edge cases (empty data, invalid input)
10. THE Test Suite SHALL achieve minimum 80% code coverage for Firebase integration code

### Requirement 20: Documentation and Deployment

**User Story:** As a developer, I want clear documentation for the Firebase architecture, so that future developers can maintain and extend the system.

#### Acceptance Criteria

1. THE Documentation SHALL include a Firestore data model diagram showing all collections and subcollections
2. THE Documentation SHALL include Security_Rules with inline comments explaining each rule
3. THE Documentation SHALL include Cloud_Functions deployment instructions
4. THE Documentation SHALL include instructions for setting up Firebase project configuration
5. THE Documentation SHALL include instructions for migrating data from MySQL to Firestore
6. THE Documentation SHALL include a comparison table showing Laravel endpoints and their Firebase equivalents
7. THE Documentation SHALL include troubleshooting guide for common Firebase errors
8. THE Documentation SHALL include cost estimation for Firebase services based on expected usage
9. THE Documentation SHALL include instructions for configuring Firebase indexes
10. THE Documentation SHALL include rollback procedures in case migration issues occur
