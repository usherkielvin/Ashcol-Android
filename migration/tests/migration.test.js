/**
 * Migration Script Test Suite
 *
 * Tests for the Node.js data migration script that transfers data from MySQL to Firestore.
 * Written in TDD style - tests define expected behavior for the migration script (migrate.js).
 *
 * TODO: Once migrate.js is implemented, remove the jest.mock stubs and enable full integration.
 *
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */

'use strict';

const {
  sampleUsers,
  sampleBranches,
  sampleEmployees,
  sampleTickets,
  samplePayments,
  firebaseUidMap,
  branchDocIdMap,
  ticketDocIdMap,
} = require('./fixtures/sampleData');

// ---------------------------------------------------------------------------
// Mock firebase-admin
// ---------------------------------------------------------------------------
const mockBatch = {
  set: jest.fn().mockReturnThis(),
  commit: jest.fn().mockResolvedValue(undefined),
};

const mockDocRef = (id) => ({ id, path: `mock/${id}` });

const mockCollection = jest.fn(() => ({
  doc: jest.fn((id) => ({
    id: id || 'auto-id',
    set: jest.fn().mockResolvedValue(undefined),
    collection: jest.fn(() => mockSubCollection()),
  })),
  add: jest.fn().mockResolvedValue(mockDocRef('auto-id')),
}));

const mockSubCollection = jest.fn(() => ({
  doc: jest.fn((id) => ({
    id: id || 'auto-id',
    set: jest.fn().mockResolvedValue(undefined),
  })),
  add: jest.fn().mockResolvedValue(mockDocRef('auto-id')),
}));

const mockFirestore = {
  collection: mockCollection,
  batch: jest.fn(() => mockBatch),
};

const mockAuth = {
  createUser: jest.fn(),
  getUserByEmail: jest.fn(),
};

jest.mock('firebase-admin', () => ({
  initializeApp: jest.fn(),
  credential: { cert: jest.fn() },
  firestore: jest.fn(() => mockFirestore),
  auth: jest.fn(() => mockAuth),
}));

// Firestore.Timestamp mock
const admin = require('firebase-admin');
admin.firestore.Timestamp = {
  fromDate: jest.fn((date) => ({ _seconds: Math.floor(date.getTime() / 1000), _nanoseconds: 0 })),
};
admin.firestore.GeoPoint = jest.fn((lat, lng) => ({ latitude: lat, longitude: lng }));

// ---------------------------------------------------------------------------
// Mock mysql2
// ---------------------------------------------------------------------------
const mockConnection = {
  execute: jest.fn(),
  end: jest.fn().mockResolvedValue(undefined),
};

jest.mock('mysql2/promise', () => ({
  createConnection: jest.fn().mockResolvedValue(mockConnection),
}));

// ---------------------------------------------------------------------------
// Helper: convert MySQL datetime string to a JS Date
// ---------------------------------------------------------------------------
function mysqlDateToDate(str) {
  if (!str) return null;
  return new Date(str.replace(' ', 'T') + 'Z');
}

// ---------------------------------------------------------------------------
// Helper: build expected Firestore user document from MySQL row
// ---------------------------------------------------------------------------
function buildExpectedUserDoc(mysqlUser, firebaseUid) {
  return {
    uid: firebaseUid,
    email: mysqlUser.email,
    name: mysqlUser.name,
    phone: mysqlUser.phone,
    role: mysqlUser.role,
    branchId: mysqlUser.branch_id ? String(mysqlUser.branch_id) : null,
    fcmTokens: [],
    profilePhotoUrl: mysqlUser.profile_photo || null,
    createdAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(mysqlUser.created_at)),
    updatedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(mysqlUser.updated_at)),
  };
}

// ---------------------------------------------------------------------------
// Helper: build expected Firestore ticket document from MySQL row
// ---------------------------------------------------------------------------
function buildExpectedTicketDoc(mysqlTicket, uidMap) {
  return {
    customerId: uidMap[sampleUsers.find((u) => u.id === mysqlTicket.customer_id).email],
    customerName: sampleUsers.find((u) => u.id === mysqlTicket.customer_id).name,
    customerEmail: sampleUsers.find((u) => u.id === mysqlTicket.customer_id).email,
    customerPhone: sampleUsers.find((u) => u.id === mysqlTicket.customer_id).phone,
    serviceType: mysqlTicket.service_type,
    description: mysqlTicket.description,
    status: mysqlTicket.status,
    priority: mysqlTicket.priority,
    location: new admin.firestore.GeoPoint(mysqlTicket.latitude, mysqlTicket.longitude),
    address: mysqlTicket.address,
    branchId: mysqlTicket.branch_id ? branchDocIdMap[mysqlTicket.branch_id] : null,
    assignedEmployeeId: mysqlTicket.assigned_employee_id
      ? uidMap[sampleUsers.find((u) => u.id === mysqlTicket.assigned_employee_id).email]
      : null,
    scheduledDate: mysqlTicket.scheduled_date
      ? admin.firestore.Timestamp.fromDate(mysqlDateToDate(mysqlTicket.scheduled_date))
      : null,
    completedDate: mysqlTicket.completed_date
      ? admin.firestore.Timestamp.fromDate(mysqlDateToDate(mysqlTicket.completed_date))
      : null,
    photoUrls: mysqlTicket.photo_urls ? JSON.parse(mysqlTicket.photo_urls) : [],
    estimatedCost: mysqlTicket.estimated_cost,
    finalCost: mysqlTicket.final_cost || null,
    createdAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(mysqlTicket.created_at)),
    updatedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(mysqlTicket.updated_at)),
  };
}

// ===========================================================================
// Unit tests for migration helper utilities
// These test pure transformation logic that does NOT require the full script.
// ===========================================================================

describe('Migration helper utilities', () => {
  // -------------------------------------------------------------------------
  // Timestamp conversion (Requirement 11.7)
  // -------------------------------------------------------------------------
  describe('MySQL datetime to Firestore Timestamp conversion', () => {
    test('converts a valid MySQL datetime string to a Firestore Timestamp', () => {
      const mysqlDatetime = '2023-05-10 11:30:00';
      const date = mysqlDateToDate(mysqlDatetime);
      const ts = admin.firestore.Timestamp.fromDate(date);

      expect(ts).toBeDefined();
      expect(ts._seconds).toBe(Math.floor(new Date('2023-05-10T11:30:00Z').getTime() / 1000));
    });

    test('returns null for null datetime values', () => {
      const result = mysqlDateToDate(null);
      expect(result).toBeNull();
    });

    test('handles datetime at epoch boundary', () => {
      const mysqlDatetime = '1970-01-01 00:00:01';
      const date = mysqlDateToDate(mysqlDatetime);
      expect(date).toBeInstanceOf(Date);
      expect(date.getTime()).toBeGreaterThan(0);
    });

    test('preserves all sample ticket timestamps', () => {
      sampleTickets.forEach((ticket) => {
        const createdAt = mysqlDateToDate(ticket.created_at);
        const updatedAt = mysqlDateToDate(ticket.updated_at);
        expect(createdAt).toBeInstanceOf(Date);
        expect(updatedAt).toBeInstanceOf(Date);
        expect(updatedAt.getTime()).toBeGreaterThanOrEqual(createdAt.getTime());
      });
    });
  });

  // -------------------------------------------------------------------------
  // User document mapping (Requirement 11.1, 11.6)
  // -------------------------------------------------------------------------
  describe('User document field mapping', () => {
    test('maps all required fields from MySQL user to Firestore document', () => {
      const mysqlUser = sampleUsers[3]; // Dave Customer
      const uid = firebaseUidMap[mysqlUser.email];
      const doc = buildExpectedUserDoc(mysqlUser, uid);

      expect(doc.uid).toBe(uid);
      expect(doc.email).toBe(mysqlUser.email);
      expect(doc.name).toBe(mysqlUser.name);
      expect(doc.phone).toBe(mysqlUser.phone);
      expect(doc.role).toBe('user');
      expect(doc.branchId).toBeNull();
      expect(doc.fcmTokens).toEqual([]);
    });

    test('maps manager user with branchId', () => {
      const mysqlUser = sampleUsers[1]; // Bob Manager
      const uid = firebaseUidMap[mysqlUser.email];
      const doc = buildExpectedUserDoc(mysqlUser, uid);

      expect(doc.role).toBe('manager');
      expect(doc.branchId).toBe('1');
    });

    test('maps admin user with null branchId', () => {
      const mysqlUser = sampleUsers[0]; // Alice Admin
      const uid = firebaseUidMap[mysqlUser.email];
      const doc = buildExpectedUserDoc(mysqlUser, uid);

      expect(doc.role).toBe('admin');
      expect(doc.branchId).toBeNull();
    });

    test('initialises fcmTokens as empty array', () => {
      sampleUsers.forEach((u) => {
        const doc = buildExpectedUserDoc(u, firebaseUidMap[u.email]);
        expect(Array.isArray(doc.fcmTokens)).toBe(true);
        expect(doc.fcmTokens.length).toBe(0);
      });
    });
  });

  // -------------------------------------------------------------------------
  // Ticket document mapping (Requirement 11.2, 11.7, 11.8)
  // -------------------------------------------------------------------------
  describe('Ticket document field mapping', () => {
    test('maps all required fields from MySQL ticket to Firestore document', () => {
      const mysqlTicket = sampleTickets[0]; // completed ticket
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.customerId).toBe(firebaseUidMap['dave@example.com']);
      expect(doc.serviceType).toBe('plumbing');
      expect(doc.status).toBe('completed');
      expect(doc.priority).toBe('high');
      expect(doc.estimatedCost).toBe(150.0);
      expect(doc.finalCost).toBe(175.0);
    });

    test('maps foreign key customer_id to Firebase UID', () => {
      const mysqlTicket = sampleTickets[0];
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.customerId).toBe(firebaseUidMap['dave@example.com']);
      expect(typeof doc.customerId).toBe('string');
    });

    test('maps foreign key branch_id to Firestore document ID', () => {
      const mysqlTicket = sampleTickets[0]; // has branch_id = 1
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.branchId).toBe(branchDocIdMap[1]);
    });

    test('maps foreign key assigned_employee_id to Firebase UID', () => {
      const mysqlTicket = sampleTickets[0]; // assigned_employee_id = 3 (Carol)
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.assignedEmployeeId).toBe(firebaseUidMap['carol@example.com']);
    });

    test('handles null branch_id for unassigned ticket', () => {
      const mysqlTicket = sampleTickets[1]; // pending, no branch
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.branchId).toBeNull();
      expect(doc.assignedEmployeeId).toBeNull();
    });

    test('converts scheduled_date to Firestore Timestamp', () => {
      const mysqlTicket = sampleTickets[0];
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.scheduledDate).toBeDefined();
      expect(doc.scheduledDate._seconds).toBeGreaterThan(0);
    });

    test('handles null scheduled_date', () => {
      const mysqlTicket = sampleTickets[1]; // no scheduled_date
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.scheduledDate).toBeNull();
    });

    test('parses photo_urls JSON string into array', () => {
      const mysqlTicket = sampleTickets[0]; // has photo_urls JSON
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(Array.isArray(doc.photoUrls)).toBe(true);
      expect(doc.photoUrls.length).toBe(1);
    });

    test('defaults photo_urls to empty array when null', () => {
      const mysqlTicket = sampleTickets[1]; // null photo_urls
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.photoUrls).toEqual([]);
    });

    test('creates GeoPoint from latitude and longitude', () => {
      const mysqlTicket = sampleTickets[0];
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.location).toBeDefined();
      expect(doc.location.latitude).toBe(mysqlTicket.latitude);
      expect(doc.location.longitude).toBe(mysqlTicket.longitude);
    });

    test('denormalises customer name, email, phone into ticket document', () => {
      const mysqlTicket = sampleTickets[0]; // customer_id = 4 (Dave)
      const doc = buildExpectedTicketDoc(mysqlTicket, firebaseUidMap);

      expect(doc.customerName).toBe('Dave Customer');
      expect(doc.customerEmail).toBe('dave@example.com');
      expect(doc.customerPhone).toBe('+1-555-0104');
    });
  });
});

// ===========================================================================
// Integration-style tests for migration phases
// These tests mock MySQL + Firebase and verify the migration logic end-to-end.
// TODO: Import the actual migration functions once migrate.js is implemented.
//       Replace the inline implementations below with imports from migrate.js.
// ===========================================================================

// ---------------------------------------------------------------------------
// Inline reference implementations (TDD stubs)
// These represent the expected behaviour of the functions in migrate.js.
// ---------------------------------------------------------------------------

/**
 * TODO: Replace with: const { migrateUsers } = require('../migrate');
 *
 * Migrates MySQL users to Firebase Auth + Firestore.
 * @param {Array} users - MySQL user rows
 * @param {object} auth - Firebase Auth instance
 * @param {object} db - Firestore instance
 * @returns {{ successCount, failureCount, uidMap, errors }}
 */
async function migrateUsers(users, auth, db) {
  const uidMap = {};
  let successCount = 0;
  let failureCount = 0;
  const errors = [];

  for (const user of users) {
    try {
      let firebaseUser;
      try {
        firebaseUser = await auth.createUser({
          email: user.email,
          displayName: user.name,
          phoneNumber: user.phone,
        });
      } catch (createErr) {
        // If user already exists, fetch by email
        firebaseUser = await auth.getUserByEmail(user.email);
      }

      const uid = firebaseUser.uid;
      uidMap[user.email] = uid;

      const userDoc = {
        uid,
        email: user.email,
        name: user.name,
        phone: user.phone,
        role: user.role,
        branchId: user.branch_id ? String(user.branch_id) : null,
        fcmTokens: [],
        profilePhotoUrl: user.profile_photo || null,
        createdAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(user.created_at)),
        updatedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(user.updated_at)),
      };

      await db.collection('users').doc(uid).set(userDoc);
      successCount++;
    } catch (err) {
      failureCount++;
      errors.push({ record: user.email, error: err.message });
    }
  }

  return { successCount, failureCount, uidMap, errors };
}

/**
 * TODO: Replace with: const { migrateBranches } = require('../migrate');
 *
 * Migrates MySQL branches to Firestore.
 * @param {Array} branches - MySQL branch rows
 * @param {object} db - Firestore instance
 * @param {object} uidMap - email → Firebase UID mapping
 * @returns {{ successCount, failureCount, branchIdMap, errors }}
 */
async function migrateBranches(branches, db, uidMap) {
  const branchIdMap = {};
  let successCount = 0;
  let failureCount = 0;
  const errors = [];

  const batch = db.batch();

  for (const branch of branches) {
    try {
      const docRef = db.collection('branches').doc();
      branchIdMap[branch.id] = docRef.id;

      const managerEmail = branch.manager_id
        ? sampleUsers.find((u) => u.id === branch.manager_id)?.email
        : null;

      const branchDoc = {
        name: branch.name,
        location: new admin.firestore.GeoPoint(branch.latitude, branch.longitude),
        address: branch.address,
        coverageRadius: branch.coverage_radius,
        managerId: managerEmail ? uidMap[managerEmail] || null : null,
        managerName: managerEmail ? sampleUsers.find((u) => u.email === managerEmail)?.name || null : null,
        phone: branch.phone,
        email: branch.email,
        isActive: branch.is_active === 1,
        createdAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(branch.created_at)),
        updatedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(branch.updated_at)),
      };

      batch.set(docRef, branchDoc);
      successCount++;
    } catch (err) {
      failureCount++;
      errors.push({ record: branch.id, error: err.message });
    }
  }

  await batch.commit();
  return { successCount, failureCount, branchIdMap, errors };
}

/**
 * TODO: Replace with: const { migrateEmployees } = require('../migrate');
 *
 * Migrates MySQL employees to Firestore branch subcollections.
 */
async function migrateEmployees(employees, db, uidMap, branchIdMap) {
  let successCount = 0;
  let failureCount = 0;
  const errors = [];

  const batch = db.batch();

  for (const emp of employees) {
    try {
      const branchDocId = branchIdMap[emp.branch_id];
      if (!branchDocId) throw new Error(`Branch ${emp.branch_id} not found in branchIdMap`);

      const userEmail = sampleUsers.find((u) => u.id === emp.user_id)?.email;
      const uid = userEmail ? uidMap[userEmail] : null;

      const empDocRef = db.collection('branches').doc(branchDocId).collection('employees').doc();

      const empDoc = {
        userId: uid,
        name: userEmail ? sampleUsers.find((u) => u.email === userEmail)?.name : null,
        email: userEmail || null,
        phone: userEmail ? sampleUsers.find((u) => u.email === userEmail)?.phone : null,
        specializations: emp.specializations ? emp.specializations.split(',') : [],
        isAvailable: emp.is_available === 1,
        currentTicketCount: emp.current_ticket_count,
        totalCompletedTickets: emp.total_completed_tickets,
        rating: emp.rating,
        joinedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(emp.joined_at)),
        updatedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(emp.updated_at)),
      };

      batch.set(empDocRef, empDoc);
      successCount++;
    } catch (err) {
      failureCount++;
      errors.push({ record: emp.id, error: err.message });
    }
  }

  await batch.commit();
  return { successCount, failureCount, errors };
}

/**
 * TODO: Replace with: const { migrateTickets } = require('../migrate');
 *
 * Migrates MySQL tickets to Firestore.
 */
async function migrateTickets(tickets, db, uidMap, branchIdMap) {
  const ticketIdMap = {};
  let successCount = 0;
  let failureCount = 0;
  const errors = [];

  const batch = db.batch();

  for (const ticket of tickets) {
    try {
      const docRef = db.collection('tickets').doc();
      ticketIdMap[ticket.id] = docRef.id;

      const customerUser = sampleUsers.find((u) => u.id === ticket.customer_id);
      const assignedUser = ticket.assigned_employee_id
        ? sampleUsers.find((u) => u.id === ticket.assigned_employee_id)
        : null;

      const ticketDoc = {
        customerId: uidMap[customerUser.email],
        customerName: customerUser.name,
        customerEmail: customerUser.email,
        customerPhone: customerUser.phone,
        serviceType: ticket.service_type,
        description: ticket.description,
        status: ticket.status,
        priority: ticket.priority,
        location: new admin.firestore.GeoPoint(ticket.latitude, ticket.longitude),
        address: ticket.address,
        branchId: ticket.branch_id ? branchIdMap[ticket.branch_id] || null : null,
        assignedEmployeeId: assignedUser ? uidMap[assignedUser.email] || null : null,
        assignedEmployeeName: assignedUser ? assignedUser.name : null,
        scheduledDate: ticket.scheduled_date
          ? admin.firestore.Timestamp.fromDate(mysqlDateToDate(ticket.scheduled_date))
          : null,
        completedDate: ticket.completed_date
          ? admin.firestore.Timestamp.fromDate(mysqlDateToDate(ticket.completed_date))
          : null,
        photoUrls: ticket.photo_urls ? JSON.parse(ticket.photo_urls) : [],
        estimatedCost: ticket.estimated_cost,
        finalCost: ticket.final_cost || null,
        createdAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(ticket.created_at)),
        updatedAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(ticket.updated_at)),
      };

      batch.set(docRef, ticketDoc);
      successCount++;
    } catch (err) {
      failureCount++;
      errors.push({ record: ticket.id, error: err.message });
    }
  }

  await batch.commit();
  return { successCount, failureCount, ticketIdMap, errors };
}

/**
 * TODO: Replace with: const { migratePayments } = require('../migrate');
 *
 * Migrates MySQL payments to Firestore ticket subcollections.
 */
async function migratePayments(payments, db, uidMap, ticketIdMap) {
  let successCount = 0;
  let failureCount = 0;
  const errors = [];

  const batch = db.batch();

  for (const payment of payments) {
    try {
      const ticketDocId = ticketIdMap[payment.ticket_id];
      if (!ticketDocId) throw new Error(`Ticket ${payment.ticket_id} not found in ticketIdMap`);

      const employeeUser = payment.employee_id
        ? sampleUsers.find((u) => u.id === payment.employee_id)
        : null;

      const paymentDocRef = db
        .collection('tickets')
        .doc(ticketDocId)
        .collection('payments')
        .doc();

      const paymentDoc = {
        amount: payment.amount,
        method: payment.method,
        status: payment.status,
        transactionId: payment.transaction_id,
        employeeId: employeeUser ? uidMap[employeeUser.email] || null : null,
        paidAt: payment.paid_at
          ? admin.firestore.Timestamp.fromDate(mysqlDateToDate(payment.paid_at))
          : null,
        createdAt: admin.firestore.Timestamp.fromDate(mysqlDateToDate(payment.created_at)),
      };

      batch.set(paymentDocRef, paymentDoc);
      successCount++;
    } catch (err) {
      failureCount++;
      errors.push({ record: payment.id, error: err.message });
    }
  }

  await batch.commit();
  return { successCount, failureCount, errors };
}
