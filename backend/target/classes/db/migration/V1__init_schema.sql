CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    national_id VARCHAR(50) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version INT DEFAULT 0
);

CREATE TABLE applications (
    id UUID PRIMARY KEY,
    applicant_id UUID,
    national_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    loan_amount NUMERIC(15,2) NOT NULL,
    tenure_months INT NOT NULL,
    income NUMERIC(15,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version INT DEFAULT 0
);

CREATE TABLE decisions (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    decision_type VARCHAR(20) NOT NULL,
    reason TEXT,
    decided_by VARCHAR(100),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version INT DEFAULT 0,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    document_type VARCHAR(100),
    file_name VARCHAR(255),
    file_url TEXT,
    mime_type VARCHAR(100),
    file_size BIGINT,
    uploaded_by VARCHAR(100),
    uploaded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version INT DEFAULT 0,
    FOREIGN KEY (application_id) REFERENCES applications(id)
);
