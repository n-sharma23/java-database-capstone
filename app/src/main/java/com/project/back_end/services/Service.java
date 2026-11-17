package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Logic shared by several controllers: token checks, logins and doctor/appointment filtering. */
@org.springframework.stereotype.Service
public class Service {

    private static final Logger log = LoggerFactory.getLogger(Service.class);

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final PasswordEncoder passwordEncoder;

    public Service(TokenService tokenService, AdminRepository adminRepository, DoctorRepository doctorRepository,
                   PatientRepository patientRepository, DoctorService doctorService,
                   PatientService patientService, PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Empty body with 200 when the token is valid for {@code user}; otherwise 401 with a message.
     * Controllers return the response as-is when the body is not empty.
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();
        if (!tokenService.validateToken(token, user)) {
            body.put("message", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null || receivedAdmin.getPassword() == null
                    || !passwordEncoder.matches(receivedAdmin.getPassword(), admin.getPassword())) {
                body.put("message", "Invalid username or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("token", tokenService.generateToken(admin.getUsername(), "admin"));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Admin login failed", e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /** Any combination of name, specialty and time (AM/PM); a missing filter is null. No filters returns all doctors. */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        boolean hasName = name != null, hasSpecialty = specialty != null, hasTime = time != null;
        if (hasName && hasSpecialty && hasTime) return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        if (hasName && hasSpecialty) return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        if (hasName && hasTime) return doctorService.filterDoctorByNameAndTime(name, time);
        if (hasSpecialty && hasTime) return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        if (hasName) return doctorService.findDoctorByName(name);
        if (hasSpecialty) return doctorService.filterDoctorBySpecility(specialty);
        if (hasTime) return doctorService.filterDoctorsByTime(time);
        Map<String, Object> all = new HashMap<>();
        all.put("doctors", doctorService.getDoctors());
        return all;
    }

    /** @return 1 if the requested start time is a free slot of the doctor that day, 0 if not, -1 if the doctor does not exist */
    public int validateAppointment(Appointment appointment) {
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                || !doctorRepository.existsById(appointment.getDoctor().getId())) {
            return -1;
        }
        String requestedStart = appointment.getAppointmentTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        List<String> freeSlots = doctorService.getDoctorAvailability(
                appointment.getDoctor().getId(), appointment.getAppointmentTime().toLocalDate());
        return freeSlots.stream().anyMatch(slot -> DoctorService.slotStart(slot).equals(requestedStart)) ? 1 : 0;
    }

    /** @return true if no patient already uses this email or phone, so the sign-up may go ahead */
    public boolean validatePatient(Patient patient) {
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(login.getEmail());
            if (patient == null || login.getPassword() == null
                    || !passwordEncoder.matches(login.getPassword(), patient.getPassword())) {
                body.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("token", tokenService.generateToken(patient.getEmail(), "patient"));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Patient login failed", e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /** Filters the logged-in patient's appointments by condition (past/future) and/or doctor name. */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        String email = tokenService.extractEmail(token);
        Patient patient = patientRepository.findByEmail(email);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Patient not found"));
        }
        if (condition != null && name != null) return patientService.filterByDoctorAndCondition(condition, name, patient.getId());
        if (condition != null) return patientService.filterByCondition(condition, patient.getId());
        if (name != null) return patientService.filterByDoctor(name, patient.getId());
        return patientService.getPatientAppointment(patient.getId(), token);
    }
}
