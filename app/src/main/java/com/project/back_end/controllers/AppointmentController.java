package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import com.project.back_end.services.TokenService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.project.back_end.controllers.PathParams.optional;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;

    public AppointmentController(AppointmentService appointmentService, Service service,
                                 TokenService tokenService, PatientRepository patientRepository) {
        this.appointmentService = appointmentService;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
    }

    /** The logged-in doctor's appointments for a date; patientName may be "null". */
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String patientName, @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "doctor");
        if (!check.getBody().isEmpty()) {
            return ResponseEntity.status(check.getStatusCode()).body(new HashMap<>(check.getBody()));
        }
        return ResponseEntity.ok(appointmentService.getAppointment(optional(patientName), date, token));
    }

    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@Valid @RequestBody Appointment appointment,
                                                               @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "patient");
        if (!check.getBody().isEmpty()) {
            return check;
        }
        // Book for the patient in the token, whatever patient id the request body claims.
        Patient caller = patientRepository.findByEmail(tokenService.extractEmail(token));
        appointment.setPatient(caller);
        appointment.setStatus(0);
        int slot = service.validateAppointment(appointment);
        if (slot == -1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid doctor id"));
        }
        if (slot == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Appointment already booked for given time or doctor not available"));
        }
        if (appointmentService.bookAppointment(appointment) == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Appointment booked successfully"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Could not book appointment"));
    }

    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@Valid @RequestBody Appointment appointment,
                                                                 @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "patient");
        if (!check.getBody().isEmpty()) {
            return check;
        }
        return appointmentService.updateAppointment(appointment, token);
    }

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable long id, @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "patient");
        if (!check.getBody().isEmpty()) {
            return check;
        }
        return appointmentService.cancelAppointment(id, token);
    }
}
