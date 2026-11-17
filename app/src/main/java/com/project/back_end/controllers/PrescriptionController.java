package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
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

@RestController
@RequestMapping("${api.path}prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentService appointmentService;
    private final TokenService tokenService;

    public PrescriptionController(PrescriptionService prescriptionService, Service service,
                                  AppointmentService appointmentService, TokenService tokenService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
        this.tokenService = tokenService;
    }

    /**
     * Saves a prescription. The token must be a doctor's, the body is validated with @Valid,
     * and the doctor must own the appointment. On success the appointment is marked completed (status 1).
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(@Valid @RequestBody Prescription prescription,
                                                                @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "doctor");
        if (!check.getBody().isEmpty()) {
            return check;
        }
        if (!appointmentService.isDoctorsAppointment(prescription.getAppointmentId(), tokenService.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only prescribe for your own appointments"));
        }
        ResponseEntity<Map<String, String>> saved = prescriptionService.savePrescription(prescription);
        if (saved.getStatusCode() == HttpStatus.CREATED) {
            appointmentService.changeStatus(prescription.getAppointmentId(), 1);
        }
        return saved;
    }

    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescription(@PathVariable Long appointmentId,
                                                               @PathVariable String token) {
        ResponseEntity<Map<String, String>> check = service.validateToken(token, "doctor");
        if (!check.getBody().isEmpty()) {
            return ResponseEntity.status(check.getStatusCode()).body(new HashMap<>(check.getBody()));
        }
        if (!appointmentService.isDoctorsAppointment(appointmentId, tokenService.extractEmail(token))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only view prescriptions for your own appointments"));
        }
        return prescriptionService.getPrescription(appointmentId);
    }
}
