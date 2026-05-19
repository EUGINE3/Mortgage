-- Insert sample users for testing
-- Passwords are hashed using BCrypt (assuming they're pre-encoded: password123 -> $2a$10$...)

INSERT INTO users (id, full_name, email, password, national_id, role, created_at) VALUES
(
    '550e8400-e29b-41d4-a716-446655440001',
    'Alice Johnson',
    'alice@example.com',
    '$2a$10$slYQmyNdGzin7olVgsqFUe4XNjNjKmAGtIIBrMCH3XLHG7iZ9bnHC',
    '1234567890',
    'APPLICANT',
    '2026-05-19 00:00:00'
),
(
    '550e8400-e29b-41d4-a716-446655440002',
    'Bob Smith',
    'bob@example.com',
    '$2a$10$slYQmyNdGzin7olVgsqFUe4XNjNjKmAGtIIBrMCH3XLHG7iZ9bnHC',
    '0987654321',
    'APPLICANT',
    '2026-05-19 00:00:00'
),
(
    '550e8400-e29b-41d4-a716-446655440003',
    'Charlie Brown',
    'charlie@example.com',
    '$2a$10$slYQmyNdGzin7olVgsqFUe4XNjNjKmAGtIIBrMCH3XLHG7iZ9bnHC',
    '1111111111',
    'CREDIT_OFFICER',
    '2026-05-19 00:00:00'
),
(
    '550e8400-e29b-41d4-a716-446655440004',
    'Diana Prince',
    'diana@example.com',
    '$2a$10$slYQmyNdGzin7olVgsqFUe4XNjNjKmAGtIIBrMCH3XLHG7iZ9bnHC',
    '2222222222',
    'CREDIT_OFFICER',
    '2026-05-19 00:00:00'
),
(
    '550e8400-e29b-41d4-a716-446655440005',
    'Eve Davis',
    'eve@example.com',
    '$2a$10$slYQmyNdGzin7olVgsqFUe4XNjNjKmAGtIIBrMCH3XLHG7iZ9bnHC',
    '3333333333',
    'APPLICANT',
    '2026-05-19 00:00:00'
);
