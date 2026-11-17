package com.project.back_end.config;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Fills empty tables with a small, realistic data set so the portals and reports have
 * something to show. Runs only when app.seed-demo-data=true. Passwords are hashed like
 * any other account; the plain-text demo passwords are listed in the README.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(AdminRepository adminRepository, DoctorRepository doctorRepository,
                          PatientRepository patientRepository, AppointmentRepository appointmentRepository,
                          PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminRepository.count() > 0 || doctorRepository.count() > 0) {
            log.info("Demo data not seeded: tables already contain data");
            return;
        }
        adminRepository.save(new Admin("admin", passwordEncoder.encode("admin@1234")));

        String doctorPassword = passwordEncoder.encode("doctor@123");
        List<Doctor> doctors = doctorRepository.saveAll(List.of(
                doctor("Dr. Emily Adams", "cardiologist", "emily.adams@clinic.example", "5551012020", doctorPassword,
                        "09:00-10:00", "10:00-11:00", "14:00-15:00"),
                doctor("Dr. Mark Johnson", "neurologist", "mark.johnson@clinic.example", "5552023030", doctorPassword,
                        "10:00-11:00", "11:00-12:00", "15:00-16:00"),
                doctor("Dr. Sarah Lee", "pediatrician", "sarah.lee@clinic.example", "5553034040", doctorPassword,
                        "09:00-10:00", "13:00-14:00", "16:00-17:00"),
                doctor("Dr. Priya Nair", "dermatologist", "priya.nair@clinic.example", "5554045050", doctorPassword,
                        "11:00-12:00", "14:00-15:00"),
                doctor("Dr. Alan Brooks", "cardiologist", "alan.brooks@clinic.example", "5555056060", doctorPassword,
                        "15:00-16:00", "16:00-17:00"),
                doctor("Dr. Olivia Chen", "dentist", "olivia.chen@clinic.example", "5556067070", doctorPassword,
                        "09:00-10:00", "10:00-11:00", "11:00-12:00")));

        String patientPassword = passwordEncoder.encode("patient@123");
        List<Patient> patients = patientRepository.saveAll(List.of(
                patient("John Smith", "john.smith@mail.example", "8881112222", "12 Oak Street, Springfield", patientPassword),
                patient("Aisha Khan", "aisha.khan@mail.example", "8882223333", "48 Pine Road, Riverton", patientPassword),
                patient("Rahul Verma", "rahul.verma@mail.example", "8883334444", "7 Lake View, Hillcrest", patientPassword),
                patient("Maria Garcia", "maria.garcia@mail.example", "8884445555", "301 Elm Avenue, Fairview", patientPassword),
                patient("Liam O'Brien", "liam.obrien@mail.example", "8885556666", "19 Cedar Lane, Brookside", patientPassword),
                patient("Sofia Rossi", "sofia.rossi@mail.example", "8886667777", "5 Maple Court, Lakeside", patientPassword),
                patient("David Kim", "david.kim@mail.example", "8887778888", "90 Birch Way, Westfield", patientPassword)));

        LocalDate day1 = LocalDate.of(2026, 12, 1);
        LocalDate day2 = LocalDate.of(2026, 12, 2);
        LocalDate day3 = LocalDate.of(2026, 12, 15);
        Doctor adams = doctors.get(0), johnson = doctors.get(1), lee = doctors.get(2), nair = doctors.get(3);
        appointmentRepository.saveAll(List.of(
                appointment(adams, patients.get(0), day1, 9, 0),
                appointment(adams, patients.get(1), day1, 10, 0),
                appointment(adams, patients.get(2), day1, 14, 0),
                appointment(adams, patients.get(3), day2, 9, 0),
                appointment(adams, patients.get(0), day3, 10, 1),
                appointment(johnson, patients.get(4), day1, 10, 0),
                appointment(johnson, patients.get(5), day1, 11, 0),
                appointment(johnson, patients.get(0), day2, 15, 1),
                appointment(lee, patients.get(6), day1, 9, 0),
                appointment(lee, patients.get(1), day3, 13, 1),
                appointment(nair, patients.get(2), day2, 11, 0),
                appointment(nair, patients.get(3), LocalDate.of(2027, 1, 12), 14, 0)));
        log.info("Seeded demo data: 1 admin, {} doctors, {} patients", doctors.size(), patients.size());
    }

    private static Doctor doctor(String name, String specialty, String email, String phone, String password,
                                 String... slots) {
        Doctor d = new Doctor();
        d.setName(name);
        d.setSpecialty(specialty);
        d.setEmail(email);
        d.setPhone(phone);
        d.setPassword(password);
        d.setAvailableTimes(List.of(slots));
        return d;
    }

    private static Patient patient(String name, String email, String phone, String address, String password) {
        Patient p = new Patient();
        p.setName(name);
        p.setEmail(email);
        p.setPhone(phone);
        p.setAddress(address);
        p.setPassword(password);
        return p;
    }

    private static Appointment appointment(Doctor doctor, Patient patient, LocalDate date, int hour, int status) {
        return new Appointment(doctor, patient, LocalDateTime.of(date, LocalTime.of(hour, 0)), status);
    }
}
