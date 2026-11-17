package com.project.back_end.services;

import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DoctorServiceTest {

    private final DoctorService service = new DoctorService(mock(DoctorRepository.class),
            mock(AppointmentRepository.class),
            new TokenService(mock(AdminRepository.class), mock(DoctorRepository.class), mock(PatientRepository.class),
                    "test-secret-that-is-at-least-32-bytes-long!!"),
            new BCryptPasswordEncoder());

    private static Doctor doctor(String name, String... slots) {
        Doctor d = new Doctor();
        d.setName(name);
        d.setAvailableTimes(List.of(slots));
        return d;
    }

    @Test
    void amAndPmAreDecidedBySlotStartTime() {
        Doctor morning = doctor("Morning", "09:00-10:00", "11:00-12:00");
        Doctor afternoon = doctor("Afternoon", "12:00-13:00", "16:00-17:00");
        Doctor both = doctor("Both", "10:00-11:00", "15:00-16:00");
        List<Doctor> all = List.of(morning, afternoon, both);

        assertThat(service.filterDoctorByTime(all, "AM")).containsExactly(morning, both);
        assertThat(service.filterDoctorByTime(all, "pm")).containsExactly(afternoon, both);
    }
}
