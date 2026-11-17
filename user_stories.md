# User Stories: Smart Clinic Management System

Each story follows the template *As a [role], I want [function], so that [benefit]*, with
acceptance criteria, a priority and a story-point estimate. They are also tracked as
[GitHub Issues](https://github.com/n-sharma23/java-database-capstone/issues).

## Admin

### 1. Admin logs in
**Title:** _As an admin, I want to log into the portal with my username and password, so that I can manage the platform securely._

**Acceptance criteria:**
1. Valid credentials open the admin dashboard.
2. Invalid credentials show an error and no dashboard is shown.
3. The session token expires after 7 days.

**Priority:** High · **Story points:** 2

### 2. Admin logs out
**Title:** _As an admin, I want to log out of the portal, so that nobody else can use my session._

**Acceptance criteria:**
1. Logging out clears the stored token and role.
2. The user returns to the role-selection page.

**Priority:** High · **Story points:** 1

### 3. Admin adds a doctor
**Title:** _As an admin, I want to add a doctor with their specialty, contact details and available time slots, so that patients can find and book them._

**Acceptance criteria:**
1. Name, specialty, email, password, a 10-digit phone number and at least one slot are required.
2. A duplicate email is rejected with a clear message.
3. The new doctor appears on the dashboard without a manual refresh.

**Priority:** High · **Story points:** 3

### 4. Admin deletes a doctor
**Title:** _As an admin, I want to delete a doctor's profile, so that doctors who have left are no longer bookable._

**Acceptance criteria:**
1. The admin confirms before deletion.
2. The doctor and their appointments are removed together.

**Priority:** Medium · **Story points:** 2

### 5. Admin views monthly appointment statistics
**Title:** _As an admin, I want to run a stored procedure in MySQL to see the number of appointments per month, so that I can track usage._

**Acceptance criteria:**
1. Reports exist for a day's appointments per doctor, and for the doctor with most patients per month and per year.
2. Reports run with `CALL` from the MySQL CLI.

**Priority:** Medium · **Story points:** 3

## Patient

### 6. Patient browses doctors without logging in
**Title:** _As a patient, I want to view a list of doctors without logging in, so that I can explore options before registering._

**Acceptance criteria:**
1. All doctors show name, specialty, email and available times.
2. Booking prompts the patient to log in first.

**Priority:** High · **Story points:** 2

### 7. Patient signs up
**Title:** _As a patient, I want to sign up with my name, email, password, phone and address, so that I can book appointments._

**Acceptance criteria:**
1. An email or phone number already in use is rejected.
2. The password is stored hashed, never as plain text.

**Priority:** High · **Story points:** 2

### 8. Patient logs in and out
**Title:** _As a patient, I want to log in and out, so that only I can manage my bookings._

**Acceptance criteria:**
1. Valid credentials open the logged-in patient dashboard.
2. Logging out returns to the public patient dashboard.

**Priority:** High · **Story points:** 1

### 9. Patient searches doctors
**Title:** _As a patient, I want to search doctors by name and filter by specialty and AM/PM availability, so that I can find a suitable doctor quickly._

**Acceptance criteria:**
1. Results update as the patient types.
2. Filters can be combined.
3. "No doctors found" is shown when nothing matches.

**Priority:** High · **Story points:** 3

### 10. Patient books an appointment
**Title:** _As a patient, I want to book a one-hour slot with a doctor, so that I can get a consultation._

**Acceptance criteria:**
1. Only the doctor's free slots on the chosen date can be booked.
2. A slot that is already taken is rejected.
3. The appointment is booked for the logged-in patient only.

**Priority:** High · **Story points:** 3

### 11. Patient views and manages upcoming appointments
**Title:** _As a patient, I want to see my upcoming and past appointments and reschedule upcoming ones, so that I can prepare and plan._

**Acceptance criteria:**
1. Appointments can be filtered by doctor name and by past or future.
2. Only scheduled (not completed) appointments can be changed.
3. A patient can never see or change another patient's appointments.

**Priority:** Medium · **Story points:** 3

## Doctor

### 12. Doctor logs in and out
**Title:** _As a doctor, I want to log in and out of the portal, so that my patients' data stays protected._

**Acceptance criteria:**
1. Valid credentials open the doctor dashboard.
2. Patient or admin tokens cannot open the doctor dashboard.

**Priority:** High · **Story points:** 1

### 13. Doctor views the appointment calendar
**Title:** _As a doctor, I want to view my appointments for any date, so that I stay organized._

**Acceptance criteria:**
1. Today's appointments show by default.
2. A date picker and a "Today" button change the day.
3. The list can be searched by patient name.

**Priority:** High · **Story points:** 2

### 14. Doctor views patient details
**Title:** _As a doctor, I want to see a patient's contact details and their previous appointments with me, so that I am prepared for the consultation._

**Acceptance criteria:**
1. Only appointments with this doctor are shown.

**Priority:** Medium · **Story points:** 2

### 15. Doctor writes a prescription
**Title:** _As a doctor, I want to add a prescription with medication, dosage and notes to an appointment, so that the patient has accurate medication._

**Acceptance criteria:**
1. One prescription per appointment.
2. Saving a prescription marks the appointment as completed.
3. A doctor can only prescribe for their own appointments.

**Priority:** High · **Story points:** 3

### 16. Doctor manages availability
**Title:** _As a doctor, I want my available time slots stored with my profile, so that patients can only book times when I am available._

**Acceptance criteria:**
1. Booked slots are removed from the availability shown to patients.

**Priority:** Medium · **Story points:** 2
