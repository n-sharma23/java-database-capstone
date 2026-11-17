package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository, AppointmentRepository appointmentRepository,
                          TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /** @return 1 saved, 0 error */
    @Transactional
    public int createPatient(Patient patient) {
        try {
            patient.setPassword(passwordEncoder.encode(patient.getPassword()));
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            log.error("Could not create patient", e);
            return 0;
        }
    }

    /** A patient's own appointments; the token must belong to that patient. */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null || !patient.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "You can only view your own appointments"));
            }
            return ok(appointmentRepository.findByPatientId(id));
        } catch (Exception e) {
            log.error("Could not load appointments for patient {}", id, e);
            return error();
        }
    }

    /** All appointments of a patient with the requesting doctor only, for the doctor's patient-record page. */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientAppointmentsForDoctor(Long patientId, String doctorEmail) {
        try {
            List<Appointment> mine = appointmentRepository.findByPatientId(patientId).stream()
                    .filter(a -> a.getDoctor().getEmail().equals(doctorEmail))
                    .toList();
            return ok(mine);
        } catch (Exception e) {
            log.error("Could not load appointments for patient {}", patientId, e);
            return error();
        }
    }

    /** "future" = scheduled (status 0), "past" = completed (status 1). */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Integer status = statusFor(condition);
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Condition must be 'past' or 'future'"));
        }
        return ok(appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        return ok(appointmentRepository.filterByDoctorNameAndPatientId(name, patientId));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Integer status = statusFor(condition);
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Condition must be 'past' or 'future'"));
        }
        return ok(appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status));
    }

    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractEmail(token));
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Patient not found"));
            }
            Map<String, Object> body = new HashMap<>();
            body.put("patient", patient);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Could not load patient details", e);
            return error();
        }
    }

    private static Integer statusFor(String condition) {
        if ("future".equalsIgnoreCase(condition)) return 0;
        if ("past".equalsIgnoreCase(condition)) return 1;
        return null;
    }

    private static ResponseEntity<Map<String, Object>> ok(List<Appointment> appointments) {
        Map<String, Object> body = new HashMap<>();
        body.put("appointments", appointments.stream().map(AppointmentDTO::from).toList());
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String, Object>> error() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal server error"));
    }
}
