# Schema Design: Smart Clinic Management System

The system uses two databases, each for the kind of data it suits best:

- **MySQL (`cms`)** holds structured data with fixed fields and relationships: admins,
  doctors, patients and appointments. Foreign keys keep appointments tied to real
  doctors and patients, and transactions keep bookings consistent.
- **MongoDB (`prescriptions`)** holds prescriptions, whose content varies by case and
  may grow new fields (refills, pharmacy notes) without a schema migration.

Tables are generated from the JPA entities in `app/src/main/java/com/project/back_end/models`;
the definitions below match the live schema (`SHOW CREATE TABLE`).

## MySQL Database Design

### Table: admin
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary key, auto increment |
| username | VARCHAR(255) | Not null, unique |
| password | VARCHAR(255) | Not null; BCrypt hash, never plain text |

### Table: doctor
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary key, auto increment |
| name | VARCHAR(100) | Not null; 3–100 characters |
| specialty | VARCHAR(50) | Not null; 3–50 characters |
| email | VARCHAR(255) | Not null, unique; valid email format |
| password | VARCHAR(255) | Not null; BCrypt hash |
| phone | VARCHAR(255) | Not null; exactly 10 digits (validated in the app) |

### Table: doctor_available_times
One row per time slot a doctor offers (from `@ElementCollection` on `Doctor.availableTimes`).

| Column | Type | Constraints |
|---|---|---|
| doctor_id | BIGINT | Not null; foreign key → doctor(id) |
| available_times | VARCHAR(255) | Slot such as `09:00-10:00` |

### Table: patient
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary key, auto increment |
| name | VARCHAR(100) | Not null; 3–100 characters |
| email | VARCHAR(255) | Not null, unique; valid email format |
| password | VARCHAR(255) | Not null; BCrypt hash |
| phone | VARCHAR(255) | Not null; exactly 10 digits; also unique per sign-up check |
| address | VARCHAR(255) | Not null; max 255 characters |

### Table: appointment
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | Primary key, auto increment |
| doctor_id | BIGINT | Not null; foreign key → doctor(id) |
| patient_id | BIGINT | Not null; foreign key → patient(id) |
| appointment_time | DATETIME(6) | Not null; must be in the future when booked |
| status | INT | Not null; 0 = scheduled, 1 = completed |

### Relationships
```
doctor 1 ──< appointment >── 1 patient
doctor 1 ──< doctor_available_times
```
- A doctor has many appointments and many available time slots.
- A patient has many appointments.
- Each appointment lasts one hour; its end time is derived (`appointment_time + 1 hour`), not stored.

### Design decisions
- **Deleting a doctor** deletes their appointments in the same transaction, so no appointment
  points to a missing doctor. Patients are not deleted automatically: medical history is kept.
- **No double booking:** before saving, the service checks the requested start time against
  the doctor's free slots for that day (`Service.validateAppointment`).
- **Passwords** are stored as BCrypt hashes and never returned by the API.
- **Stored procedures** for reporting live in `app/src/main/resources/db/stored_procedures.sql`:
  `GetDailyAppointmentReportByDoctor`, `GetDoctorWithMostPatientsByMonth`, `GetDoctorWithMostPatientsByYear`.

## MongoDB Collection Design

### Collection: prescriptions
```json
{
  "_id": "ObjectId('6789abcd1234ef5678901234')",
  "patientName": "John Smith",
  "appointmentId": 1,
  "medication": "Paracetamol 500mg",
  "dosage": "1 tablet every 6 hours",
  "doctorNotes": "Take after meals. Return if fever persists beyond 3 days."
}
```
- `appointmentId` links the prescription to `appointment.id` in MySQL. One prescription per
  appointment is enforced by the service layer.
- `patientName`: 3–100 characters; `medication`: 3–100 characters; `dosage` required;
  `doctorNotes`: up to 200 characters.
