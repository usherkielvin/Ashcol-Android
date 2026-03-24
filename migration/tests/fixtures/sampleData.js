/**
 * Sample data fixtures representing realistic MySQL records for migration testing.
 * These fixtures mirror the MySQL schema used in the Laravel backend.
 */

const sampleUsers = [
  {
    id: 1,
    name: 'Alice Admin',
    email: 'alice@example.com',
    phone: '+1-555-0101',
    role: 'admin',
    branch_id: null,
    profile_photo: null,
    created_at: '2023-01-15 08:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
  {
    id: 2,
    name: 'Bob Manager',
    email: 'bob@example.com',
    phone: '+1-555-0102',
    role: 'manager',
    branch_id: 1,
    profile_photo: null,
    created_at: '2023-02-10 09:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
  {
    id: 3,
    name: 'Carol Employee',
    email: 'carol@example.com',
    phone: '+1-555-0103',
    role: 'employee',
    branch_id: 1,
    profile_photo: null,
    created_at: '2023-03-05 10:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
  {
    id: 4,
    name: 'Dave Customer',
    email: 'dave@example.com',
    phone: '+1-555-0104',
    role: 'user',
    branch_id: null,
    profile_photo: null,
    created_at: '2023-04-20 11:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
  {
    id: 5,
    name: 'Eve Customer',
    email: 'eve@example.com',
    phone: '+1-555-0105',
    role: 'user',
    branch_id: null,
    profile_photo: null,
    created_at: '2023-05-01 12:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
];

const sampleBranches = [
  {
    id: 1,
    name: 'Downtown Branch',
    latitude: 40.7128,
    longitude: -74.006,
    address: '123 Main St, New York, NY 10001',
    coverage_radius: 10.0,
    manager_id: 2,
    phone: '+1-555-0200',
    email: 'downtown@example.com',
    is_active: 1,
    created_at: '2023-01-20 08:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
  {
    id: 2,
    name: 'Uptown Branch',
    latitude: 40.7831,
    longitude: -73.9712,
    address: '456 Park Ave, New York, NY 10022',
    coverage_radius: 8.0,
    manager_id: null,
    phone: '+1-555-0201',
    email: 'uptown@example.com',
    is_active: 1,
    created_at: '2023-02-15 08:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
];

const sampleEmployees = [
  {
    id: 1,
    user_id: 3,
    branch_id: 1,
    specializations: 'plumbing,electrical',
    is_available: 1,
    current_ticket_count: 2,
    total_completed_tickets: 45,
    rating: 4.8,
    joined_at: '2023-03-05 10:00:00',
    updated_at: '2023-06-01 10:00:00',
  },
];

const sampleTickets = [
  {
    id: 1,
    customer_id: 4,
    service_type: 'plumbing',
    description: 'Leaking pipe under kitchen sink',
    status: 'completed',
    priority: 'high',
    latitude: 40.7128,
    longitude: -74.006,
    address: '789 Oak St, New York, NY 10002',
    branch_id: 1,
    assigned_employee_id: 3,
    scheduled_date: '2023-05-10 09:00:00',
    completed_date: '2023-05-10 11:30:00',
    photo_urls: '["https://storage.example.com/ticket1/photo1.jpg"]',
    estimated_cost: 150.0,
    final_cost: 175.0,
    created_at: '2023-05-08 14:00:00',
    updated_at: '2023-05-10 11:30:00',
  },
  {
    id: 2,
    customer_id: 5,
    service_type: 'electrical',
    description: 'Power outlet not working in living room',
    status: 'pending',
    priority: 'medium',
    latitude: 40.7589,
    longitude: -73.9851,
    address: '321 Elm St, New York, NY 10019',
    branch_id: null,
    assigned_employee_id: null,
    scheduled_date: null,
    completed_date: null,
    photo_urls: null,
    estimated_cost: 80.0,
    final_cost: null,
    created_at: '2023-06-01 09:00:00',
    updated_at: '2023-06-01 09:00:00',
  },
  {
    id: 3,
    customer_id: 4,
    service_type: 'hvac',
    description: 'Air conditioning not cooling properly',
    status: 'in_progress',
    priority: 'high',
    latitude: 40.7128,
    longitude: -74.006,
    address: '789 Oak St, New York, NY 10002',
    branch_id: 1,
    assigned_employee_id: 3,
    scheduled_date: '2023-06-05 10:00:00',
    completed_date: null,
    photo_urls: null,
    estimated_cost: 200.0,
    final_cost: null,
    created_at: '2023-06-02 08:00:00',
    updated_at: '2023-06-03 09:00:00',
  },
];

const samplePayments = [
  {
    id: 1,
    ticket_id: 1,
    amount: 175.0,
    method: 'credit_card',
    status: 'paid',
    transaction_id: 'TXN-20230510-001',
    employee_id: 3,
    paid_at: '2023-05-10 11:45:00',
    created_at: '2023-05-10 11:30:00',
    updated_at: '2023-05-10 11:45:00',
  },
];

// Firebase UIDs that would be created for each MySQL user (keyed by email)
const firebaseUidMap = {
  'alice@example.com': 'firebase-uid-alice-001',
  'bob@example.com': 'firebase-uid-bob-002',
  'carol@example.com': 'firebase-uid-carol-003',
  'dave@example.com': 'firebase-uid-dave-004',
  'eve@example.com': 'firebase-uid-eve-005',
};

// Firestore document IDs that would be created for each MySQL branch
const branchDocIdMap = {
  1: 'branch-downtown-001',
  2: 'branch-uptown-002',
};

// Firestore document IDs that would be created for each MySQL ticket
const ticketDocIdMap = {
  1: 'ticket-doc-001',
  2: 'ticket-doc-002',
  3: 'ticket-doc-003',
};

module.exports = {
  sampleUsers,
  sampleBranches,
  sampleEmployees,
  sampleTickets,
  samplePayments,
  firebaseUidMap,
  branchDocIdMap,
  ticketDocIdMap,
};
