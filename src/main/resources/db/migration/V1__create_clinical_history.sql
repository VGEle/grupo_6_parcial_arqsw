CREATE TABLE patients (
    id UUID PRIMARY KEY,
    identification VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL CHECK (age >= 0 AND age <= 150),
    gender VARCHAR(20) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    professional_license VARCHAR(50) NOT NULL UNIQUE,
    specialty VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinical_histories (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    doctor_id UUID NOT NULL REFERENCES doctors(id),
    consultation_reason VARCHAR(500) NOT NULL,
    diagnosis VARCHAR(500) NOT NULL,
    treatment VARCHAR(1000) NOT NULL,
    consultation_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinical_histories_patient_date
    ON clinical_histories (patient_id, consultation_date DESC);

