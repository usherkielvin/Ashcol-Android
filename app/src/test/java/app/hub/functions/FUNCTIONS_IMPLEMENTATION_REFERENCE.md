# Cloud Functions Implementation Reference

This document provides reference implementations and specifications for the Cloud Functions that need to be implemented in Tasks 8.1-8.5.

## Overview

Cloud Functions replace Laravel controller logic with serverless functions. They handle:
- Branch routing algorithm
- Dashboard statistics aggregation
- User management (admin operations)
- Firestore triggers (automatic responses to data changes)

## Function Specifications

### 1. assignTicketToBranch (Callable Function)

**Purpose:** Calculate the nearest branch to a ticket location and assign the ticket.

**Input:**
```javascript
{
  ticketId: string,      // Firestore ticket document ID
  latitude: number,      // Ticket location latitude (-90 to 90)
  longitude: number      // Ticket location longitude (-180 to 180)
}
```

**Output:**
```javascript
{
  branchId: string,      // Assigned branch document ID
  distance: number       // Distance in kilometers
}
```

**Algorithm:**
1. Query all branches from Firestore
2. For each branch, calculate distance using Haversine formula
3. Filter branches by coverage area (if defined)
4. If no branches within coverage, use nearest branch
5. Update ticket document with assigned branchId
6. Return branchId and distance

**Error Cases:**
- No branches exist: Throw error "No branches available"
- Invalid coordinates: Throw error "Invalid coordinates"
- Ticket not found: Throw error "Ticket not found"

**Performance Requirement:** Complete within 5 seconds for up to 100 branches

**Implementation Location:** `functions/src/index.ts` or `functions/index.js`

**Example Implementation (TypeScript):**
```typescript
export const assignTicketToBranch = functions.https.onCall(async (data, context) => {
  const { ticketId, latitude, longitude } = data;
  
  // Validate input
  if (!ticketId || !latitude || !longitude) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing required fields');
  }
  
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid coordinates');
  }
  
  // Query all branches
  const branchesSnapshot = await admin.firestore().collection('branches').get();
  
  if (branchesSnapshot.empty) {
    throw new functions.https.HttpsError('not-found', 'No branches available');
  }
  
  // Calculate distances using Haversine formula
  let nearestBranch = null;
  let minDistance = Infinity;
  
  branchesSnapshot.forEach(doc => {
    const branch = doc.data();
    const distance = calculateHaversineDistance(
      latitude, longitude,
      branch.location.latitude, branch.location.longitude
    );
    
    // Check coverage area
    if (branch.coverageRadius && distance <= branch.coverageRadius) {
      if (distance < minDistance) {
        minDistance = distance;
        nearestBranch = { id: doc.id, ...branch };
      }
    }
  });
  
  // If no branch within coverage, use nearest
  if (!nearestBranch) {
    branchesSnapshot.forEach(doc => {
      const branch = doc.data();
      const distance = calculateHaversineDistance(
        latitude, longitude,
        branch.location.latitude, branch.location.longitude
      );
      
      if (distance < minDistance) {
        minDistance = distance;
        nearestBranch = { id: doc.id, ...branch };
      }
    });
  }
  
  // Update ticket with assigned branch
  await admin.firestore().collection('tickets').doc(ticketId).update({
    branchId: nearestBranch.id,
    branchName: nearestBranch.name,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  return {
    branchId: nearestBranch.id,
    distance: minDistance
  };
});

function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in kilometers
  const dLat = toRadians(lat2 - lat1);
  const dLon = toRadians(lon2 - lon1);
  
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function toRadians(degrees) {
  return degrees * (Math.PI / 180);
}
```

---

### 2. getDashboardStats (Callable Function)

**Purpose:** Aggregate statistics for dashboard display based on user role.

**Input:**
```javascript
{
  role: string,          // User role: "admin" or "manager"
  branchId?: string      // Required for manager role
}
```

**Output:**
```javascript
{
  totalTickets: number,
  completedTickets: number,
  revenue: number,
  avgCompletionTime: number,        // In hours
  ticketsByStatus: {
    pending: number,
    assigned: number,
    in_progress: number,
    completed: number,
    cancelled: number
  },
  employeeWorkload: {
    [employeeId: string]: number    // Tickets per employee
  }
}
```

**Algorithm:**
1. Validate role and branchId (if manager)
2. Query tickets (filtered by branch for managers)
3. Aggregate ticket counts by status
4. Calculate total revenue from completed payments
5. Calculate average completion time (completedDate - createdAt)
6. Calculate employee workload (tickets per employee)
7. Cache results for 5 minutes
8. Return aggregated statistics

**Caching Strategy:**
- Use in-memory cache with 5-minute TTL
- Cache key: `stats_${role}_${branchId || 'all'}`

**Performance Requirement:** Complete within 3 seconds

**Example Implementation (TypeScript):**
```typescript
const statsCache = new Map();
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

export const getDashboardStats = functions.https.onCall(async (data, context) => {
  const { role, branchId } = data;
  
  // Validate input
  if (!role || !['admin', 'manager'].includes(role)) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid role');
  }
  
  if (role === 'manager' && !branchId) {
    throw new functions.https.HttpsError('invalid-argument', 'Manager role requires branchId');
  }
  
  // Check cache
  const cacheKey = `stats_${role}_${branchId || 'all'}`;
  const cached = statsCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.data;
  }
  
  // Query tickets
  let ticketsQuery = admin.firestore().collection('tickets');
  if (role === 'manager') {
    ticketsQuery = ticketsQuery.where('branchId', '==', branchId);
  }
  
  const ticketsSnapshot = await ticketsQuery.get();
  
  // Initialize stats
  const stats = {
    totalTickets: 0,
    completedTickets: 0,
    revenue: 0,
    avgCompletionTime: 0,
    ticketsByStatus: {
      pending: 0,
      assigned: 0,
      in_progress: 0,
      completed: 0,
      cancelled: 0
    },
    employeeWorkload: {}
  };
  
  let totalCompletionTime = 0;
  let completedCount = 0;
  
  // Aggregate data
  for (const doc of ticketsSnapshot.docs) {
    const ticket = doc.data();
    stats.totalTickets++;
    
    // Count by status
    if (stats.ticketsByStatus[ticket.status] !== undefined) {
      stats.ticketsByStatus[ticket.status]++;
    }
    
    // Count completed tickets
    if (ticket.status === 'completed') {
      stats.completedTickets++;
      
      // Calculate completion time
      if (ticket.completedDate && ticket.createdAt) {
        const completionTime = ticket.completedDate.toMillis() - ticket.createdAt.toMillis();
        totalCompletionTime += completionTime;
        completedCount++;
      }
      
      // Calculate revenue from payments
      const paymentsSnapshot = await doc.ref.collection('payments')
        .where('status', '==', 'paid')
        .get();
      
      paymentsSnapshot.forEach(paymentDoc => {
        const payment = paymentDoc.data();
        stats.revenue += payment.amount || 0;
      });
    }
    
    // Employee workload
    if (ticket.assignedEmployeeId) {
      stats.employeeWorkload[ticket.assignedEmployeeId] = 
        (stats.employeeWorkload[ticket.assignedEmployeeId] || 0) + 1;
    }
  }
  
  // Calculate average completion time in hours
  if (completedCount > 0) {
    stats.avgCompletionTime = (totalCompletionTime / completedCount) / (1000 * 60 * 60);
  }
  
  // Cache results
  statsCache.set(cacheKey, {
    data: stats,
    timestamp: Date.now()
  });
  
  return stats;
});
```

---

### 3. User Management Functions (Callable Functions)

#### 3.1 createUserAccount

**Purpose:** Create a new user account with specified role (admin only).

**Input:**
```javascript
{
  email: string,
  role: string,          // "admin", "manager", "employee", or "user"
  branchId?: string      // Required for manager and employee roles
}
```

**Output:**
```javascript
{
  uid: string,
  success: boolean
}
```

**Algorithm:**
1. Verify caller is admin (check context.auth.token.role)
2. Validate required fields
3. Create user in Firebase Auth
4. Set custom claims (role, branchId)
5. Create user document in Firestore
6. Send password reset email
7. Return uid and success status

**Example Implementation:**
```typescript
export const createUserAccount = functions.https.onCall(async (data, context) => {
  // Verify admin role
  if (!context.auth || context.auth.token.role !== 'admin') {
    throw new functions.https.HttpsError('permission-denied', 'Admin access required');
  }
  
  const { email, role, branchId } = data;
  
  // Validate input
  if (!email || !role) {
    throw new functions.https.HttpsError('invalid-argument', 'Email and role are required');
  }
  
  if (['manager', 'employee'].includes(role) && !branchId) {
    throw new functions.https.HttpsError('invalid-argument', 'BranchId required for manager/employee');
  }
  
  // Create user in Auth
  const userRecord = await admin.auth().createUser({
    email: email,
    emailVerified: false
  });
  
  // Set custom claims
  const customClaims = { role };
  if (branchId) {
    customClaims.branchId = branchId;
  }
  await admin.auth().setCustomUserClaims(userRecord.uid, customClaims);
  
  // Create Firestore document
  await admin.firestore().collection('users').doc(userRecord.uid).set({
    email: email,
    role: role,
    branchId: branchId || null,
    fcmTokens: [],
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  // Send password reset email
  await admin.auth().generatePasswordResetLink(email);
  
  return {
    uid: userRecord.uid,
    success: true
  };
});
```

#### 3.2 setUserRole

**Purpose:** Update user role and custom claims (admin only).

**Input:**
```javascript
{
  uid: string,
  role: string,
  branchId?: string
}
```

**Output:**
```javascript
{
  success: boolean
}
```

#### 3.3 deleteUserAccount

**Purpose:** Delete user account from Auth and Firestore (admin only).

**Input:**
```javascript
{
  uid: string
}
```

**Output:**
```javascript
{
  success: boolean
}
```

---

### 4. Firestore Trigger Functions

#### 4.1 onUserCreate

**Purpose:** Automatically create user document when new user registers.

**Trigger:** `functions.auth.user().onCreate()`

**Algorithm:**
1. Extract user data from Auth event
2. Create user document in Firestore with default role "user"
3. Set custom claims with role "user"

**Example Implementation:**
```typescript
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  // Create Firestore user document
  await admin.firestore().collection('users').doc(user.uid).set({
    email: user.email,
    name: user.displayName || '',
    phone: user.phoneNumber || '',
    role: 'user',
    branchId: null,
    fcmTokens: [],
    profilePhotoUrl: user.photoURL || '',
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  // Set custom claims
  await admin.auth().setCustomUserClaims(user.uid, { role: 'user' });
});
```

#### 4.2 onTicketStatusChange

**Purpose:** Send FCM notification when ticket status changes.

**Trigger:** `functions.firestore.document('tickets/{ticketId}').onUpdate()`

**Algorithm:**
1. Compare before and after status
2. If status changed, query user's FCM tokens
3. Send notification to all user devices
4. Create notification document in Firestore

#### 4.3 onTicketDelete

**Purpose:** Clean up photos from Cloud Storage when ticket is deleted.

**Trigger:** `functions.firestore.document('tickets/{ticketId}').onDelete()`

**Algorithm:**
1. Extract ticketId from context
2. List all files in `ticket-images/{ticketId}/`
3. Delete all files
4. Delete folder

#### 4.4 onUserNameChange

**Purpose:** Update denormalized user name in all tickets.

**Trigger:** `functions.firestore.document('users/{userId}').onUpdate()`

**Algorithm:**
1. Compare before and after name
2. If name changed, query all tickets where customerId matches
3. Use batch write to update customerName in all tickets

---

## Testing Considerations

### Unit Testing

Each function should have unit tests that:
- Mock Firestore operations
- Mock Firebase Auth operations
- Test edge cases (empty data, invalid input)
- Test error handling

### Integration Testing

Integration tests (in `CloudFunctionsIntegrationTest.java`) should:
- Use Firebase Emulator Suite
- Test with real Firestore data
- Validate end-to-end behavior
- Test performance requirements

### Test Data Setup

For integration tests, create test data in emulator:
```javascript
// Test branches
await admin.firestore().collection('branches').doc('branch_1').set({
  name: 'Downtown Branch',
  location: new admin.firestore.GeoPoint(40.7128, -74.0060),
  coverageRadius: 10.0,
  isActive: true
});

// Test tickets
await admin.firestore().collection('tickets').doc('ticket_1').set({
  customerId: 'user_123',
  status: 'pending',
  location: new admin.firestore.GeoPoint(40.7580, -73.9855),
  createdAt: admin.firestore.FieldValue.serverTimestamp()
});
```

## Deployment

### Deploy All Functions
```bash
firebase deploy --only functions
```

### Deploy Specific Function
```bash
firebase deploy --only functions:assignTicketToBranch
```

### View Logs
```bash
firebase functions:log
```

## Related Documentation

- [Firebase Cloud Functions Documentation](https://firebase.google.com/docs/functions)
- [Firebase Admin SDK Documentation](https://firebase.google.com/docs/admin/setup)
- [Haversine Formula](https://en.wikipedia.org/wiki/Haversine_formula)
- [CLOUD_FUNCTIONS_TESTING_GUIDE.md](./CLOUD_FUNCTIONS_TESTING_GUIDE.md)
