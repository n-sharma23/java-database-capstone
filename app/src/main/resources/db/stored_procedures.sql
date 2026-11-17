-- Reporting procedures for the Smart Clinic database (cms).
-- Run with:  mysql -u root cms < app/src/main/resources/db/stored_procedures.sql
-- DELIMITER is a mysql-client command, so this file is applied with the CLI, not by Spring.

DELIMITER $$

DROP PROCEDURE IF EXISTS GetDailyAppointmentReportByDoctor $$
-- Every appointment on report_date, grouped by doctor, with the patient and status.
CREATE PROCEDURE GetDailyAppointmentReportByDoctor(IN report_date DATE)
BEGIN
    SELECT d.name AS doctor_name,
           a.appointment_time,
           a.status,
           p.name AS patient_name,
           p.phone AS patient_phone
    FROM appointment a
    JOIN doctor d ON a.doctor_id = d.id
    JOIN patient p ON a.patient_id = p.id
    WHERE DATE(a.appointment_time) = report_date
    ORDER BY d.name, a.appointment_time;
END $$

DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByMonth $$
-- The doctor who saw the most distinct patients in a month.
CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(IN input_month INT, IN input_year INT)
BEGIN
    SELECT a.doctor_id, d.name AS doctor_name, COUNT(DISTINCT a.patient_id) AS patients_seen
    FROM appointment a
    JOIN doctor d ON a.doctor_id = d.id
    WHERE MONTH(a.appointment_time) = input_month AND YEAR(a.appointment_time) = input_year
    GROUP BY a.doctor_id, d.name
    ORDER BY patients_seen DESC
    LIMIT 1;
END $$

DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByYear $$
-- The doctor who saw the most distinct patients in a year.
CREATE PROCEDURE GetDoctorWithMostPatientsByYear(IN input_year INT)
BEGIN
    SELECT a.doctor_id, d.name AS doctor_name, COUNT(DISTINCT a.patient_id) AS patients_seen
    FROM appointment a
    JOIN doctor d ON a.doctor_id = d.id
    WHERE YEAR(a.appointment_time) = input_year
    GROUP BY a.doctor_id, d.name
    ORDER BY patients_seen DESC
    LIMIT 1;
END $$

DELIMITER ;
