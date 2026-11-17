package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import com.project.back_end.services.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.project.back_end.controllers.PathParams.optional;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;
    private final TokenService tokenService;

    public PatientController(PatientService patientService, Service service, TokenService tokenService) {
        this.patientService = patientService;
        this.service = service;
        this.tokenService = tokenService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "patient");
        if (!check.getBody().isEmpty()) {
            return ResponseEntity.status(check.getStatusCode()).body(new HashMap<>(check.getBody()));
        }
        return patientService.getPatientDetails(token);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@Valid @RequestBody Patient patient) {
        if (!service.validatePatient(patient)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Patient with email id or phone no already exist"));
        }
        if (patientService.createPatient(patient) == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Signup successful"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal server error"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    /**
     * Appointments of patient {id}. A patient token sees only their own; a doctor token
     * (from the doctor's patient-record page) sees only appointments with that doctor.
     */
    @GetMapping("/{id}/{user}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(@PathVariable Long id, @PathVariable String user,
                                                                     @PathVariable String token) {
        if (!"patient".equals(user) && !"doctor".equals(user)) {
            return ResponseEntity.badRequest().body(Map.of("message", "User must be patient or doctor"));
        }
        ResponseEntity<Map<String, String>> check = service.validateToken(token, user);
        if (!check.getBody().isEmpty()) {
            return ResponseEntity.status(check.getStatusCode()).body(new HashMap<>(check.getBody()));
        }
        return "patient".equals(user)
                ? patientService.getPatientAppointment(id, token)
                : patientService.getPatientAppointmentsForDoctor(id, tokenService.extractEmail(token));
    }

    /** condition is past or future; either segment may be "null". */
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(@PathVariable String condition,
                                                                        @PathVariable String name,
                                                                        @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "patient");
        if (!check.getBody().isEmpty()) {
            return ResponseEntity.status(check.getStatusCode()).body(new HashMap<>(check.getBody()));
        }
        return service.filterPatient(optional(condition), optional(name), token);
    }
}
