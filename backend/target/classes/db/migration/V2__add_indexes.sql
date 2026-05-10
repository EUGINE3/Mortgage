-- Add indexes for better query performance

CREATE INDEX idx_applications_applicant_id ON applications(applicant_id);
CREATE INDEX idx_applications_national_id ON applications(national_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_created_at ON applications(created_at);

CREATE INDEX idx_decisions_application_id ON decisions(application_id);
CREATE INDEX idx_decisions_created_at ON decisions(created_at);

CREATE INDEX idx_documents_application_id ON documents(application_id);
CREATE INDEX idx_documents_created_at ON documents(created_at);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_national_id ON users(national_id);
