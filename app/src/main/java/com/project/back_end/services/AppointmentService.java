package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final com.project.back_end.services.Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              com.project.back_end.services.Service service,
                              TokenService tokenService, PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /** Saves a new appointment. @return 1 saved, 0 failed */
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            log.error("Could not book appointment", e);
            return 0;
        }
    }

    /**
     * Moves an appointment to a new time. Only the patient who owns it may change it,
     * only while it is still scheduled, and only into a free slot of the doctor.
     */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment updated, String token) {
        Map<String, String> body = new HashMap<>();
        Appointment existing = updated.getId() == null ? null : appointmentRepository.findById(updated.getId()).orElse(null);
        if (existing == null) {
            body.put("message", "Appointment not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        Patient caller = patientRepository.findByEmail(tokenService.extractEmail(token));
        if (caller == null || !existing.getPatient().getId().equals(caller.getId())) {
            body.put("message", "You can only change your own appointments");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        if (existing.getStatus() != 0) {
            body.put("message", "Completed appointments cannot be changed");
            return ResponseEntity.badRequest().body(body);
        }
        updated.setPatient(existing.getPatient());
        int slot = service.validateAppointment(updated);
        if (slot == -1) {
            body.put("message", "Doctor not found");
            return ResponseEntity.badRequest().body(body);
        }
        if (slot == 0) {
            body.put("message", "That time is not available for this doctor");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        existing.setDoctor(doctorRepository.getReferenceById(updated.getDoctor().getId()));
        existing.setAppointmentTime(updated.getAppointmentTime());
        appointmentRepository.save(existing);
        body.put("message", "Appointment updated successfully");
        return ResponseEntity.ok(body);
    }

    /** Deletes an appointment, but only for the patient who booked it. */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> body = new HashMap<>();
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) {
            body.put("message", "Appointment not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        Patient caller = patientRepository.findByEmail(tokenService.extractEmail(token));
        if (caller == null || !appointment.getPatient().getId().equals(caller.getId())) {
            body.put("message", "You can only cancel your own appointments");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
        appointmentRepository.delete(appointment);
        body.put("message", "Appointment cancelled successfully");
        return ResponseEntity.ok(body);
    }

    /**
     * The logged-in doctor's appointments on {@code date}, optionally narrowed to patients
     * whose name contains {@code patientName}. The doctor is taken from the token, never from the request.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String patientName, LocalDate date, String token) {
        Doctor doctor = doctorRepository.findByEmail(tokenService.extractEmail(token));
        var start = date.atStartOfDay();
        var end = date.atTime(LocalTime.MAX);
        List<Appointment> appointments = patientName == null
                ? appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end)
                : appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                        doctor.getId(), patientName, start, end);
        Map<String, Object> body = new HashMap<>();
        body.put("appointments", appointments.stream().map(AppointmentDTO::from).toList());
        return body;
    }

    @Transactional
    public void changeStatus(long id, int status) {
        appointmentRepository.updateStatus(status, id);
    }

    /** True if the appointment exists and belongs to the doctor with this email. */
    @Transactional(readOnly = true)
    public boolean isDoctorsAppointment(long appointmentId, String doctorEmail) {
        return appointmentRepository.findById(appointmentId)
                .map(a -> a.getDoctor().getEmail().equals(doctorEmail))
                .orElse(false);
    }
}
