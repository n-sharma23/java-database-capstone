package com.project.back_end.services;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
                         TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * The doctor's slots on {@code date}, minus slots already booked. A slot "09:00-10:00"
     * is booked when an appointment starts at 09:00 that day.
     */
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null) {
            return List.of();
        }
        Set<String> bookedStarts = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, date.atStartOfDay(), date.atTime(LocalTime.MAX))
                .stream()
                .map(a -> a.getAppointmentTime().toLocalTime().format(HH_MM))
                .collect(Collectors.toSet());
        return doctor.getAvailableTimes().stream()
                .filter(slot -> !bookedStarts.contains(slotStart(slot)))
                .toList();
    }

    /** @return 1 saved, -1 email already registered, 0 unexpected error */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }
            doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Could not save doctor", e);
            return 0;
        }
    }

    /** @return 1 updated, -1 doctor not found, 0 unexpected error */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || !doctorRepository.existsById(doctor.getId())) {
                return -1;
            }
            doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            log.error("Could not update doctor {}", doctor.getId(), e);
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /** Deletes the doctor and their appointments together. @return 1 deleted, -1 not found, 0 error */
    @Transactional
    public int deleteDoctor(long id) {
        try {
            if (!doctorRepository.existsById(id)) {
                return -1;
            }
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            log.error("Could not delete doctor {}", id, e);
            return 0;
        }
    }

    /** Checks the credentials and returns a JWT on success. The same message is used for unknown email and wrong password. */
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> body = new HashMap<>();
        try {
            Doctor doctor = doctorRepository.findByEmail(login.getEmail());
            if (doctor == null || login.getPassword() == null
                    || !passwordEncoder.matches(login.getPassword(), doctor.getPassword())) {
                body.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("token", tokenService.generateToken(doctor.getEmail(), "doctor"));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Doctor login failed", e);
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        return wrap(doctorRepository.findByNameLike(name));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        return wrap(filterDoctorByTime(
                doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty), amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        return wrap(filterDoctorByTime(doctorRepository.findByNameLike(name), amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        return wrap(doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        return wrap(filterDoctorByTime(doctorRepository.findBySpecialtyIgnoreCase(specialty), amOrPm));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        return wrap(doctorRepository.findBySpecialtyIgnoreCase(specialty));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return wrap(filterDoctorByTime(doctorRepository.findAll(), amOrPm));
    }

    /** Keeps doctors with at least one slot in the morning (start before 12:00, "AM") or afternoon ("PM"). */
    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        boolean wantMorning = "AM".equalsIgnoreCase(amOrPm);
        return doctors.stream()
                .filter(d -> d.getAvailableTimes().stream().anyMatch(slot -> {
                    int hour = LocalTime.parse(slotStart(slot), HH_MM).getHour();
                    return wantMorning == (hour < 12);
                }))
                .toList();
    }

    static String slotStart(String slot) {
        return slot.split("-")[0].trim();
    }

    private static Map<String, Object> wrap(List<Doctor> doctors) {
        Map<String, Object> map = new HashMap<>();
        map.put("doctors", doctors);
        return map;
    }
}
