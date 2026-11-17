package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /** One prescription per appointment: a second one is rejected with 400. */
    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        Map<String, String> body = new HashMap<>();
        try {
            if (!prescriptionRepository.findByAppointmentId(prescription.getAppointmentId()).isEmpty()) {
                body.put("message", "A prescription already exists for this appointment");
                return ResponseEntity.badRequest().body(body);
            }
            prescriptionRepository.save(prescription);
            body.put("message", "Prescription saved");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (Exception e) {
            log.error("Could not save prescription", e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    /** Returns {"prescription": [...]}; an empty list means none has been written yet. */
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        Map<String, Object> body = new HashMap<>();
        try {
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);
            body.put("prescription", prescriptions);
            if (prescriptions.isEmpty()) {
                body.put("message", "No prescription found for this appointment");
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Could not load prescription for appointment {}", appointmentId, e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
