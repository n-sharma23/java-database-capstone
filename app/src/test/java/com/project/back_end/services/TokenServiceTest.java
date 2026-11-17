package com.project.back_end.services;

import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";

    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(mock(AdminRepository.class), doctors, patients, SECRET);
        when(doctors.findByEmail("doc@clinic.example")).thenReturn(new Doctor());
        when(patients.findByEmail("doc@clinic.example")).thenReturn(new Patient());
    }

    @Test
    void tokenCarriesTheEmailAsSubject() {
        String token = tokenService.generateToken("doc@clinic.example", "doctor");
        assertThat(tokenService.extractEmail(token)).isEqualTo("doc@clinic.example");
        assertThat(tokenService.validateToken(token, "doctor")).isTrue();
    }

    @Test
    void tokenIsRejectedForAnotherRoleEvenIfTheEmailExistsThere() {
        String token = tokenService.generateToken("doc@clinic.example", "doctor");
        assertThat(tokenService.validateToken(token, "patient")).isFalse();
        assertThat(tokenService.validateToken(token, "admin")).isFalse();
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        String token = tokenService.generateToken("doc@clinic.example", "doctor");
        assertThat(tokenService.validateToken(token + "x", "doctor")).isFalse();
        assertThat(tokenService.validateToken("not-a-jwt", "doctor")).isFalse();

        TokenService otherKey = new TokenService(mock(AdminRepository.class), doctors, patients,
                "a-completely-different-secret-of-32-bytes!!");
        assertThat(otherKey.validateToken(token, "doctor")).isFalse();
    }
}
