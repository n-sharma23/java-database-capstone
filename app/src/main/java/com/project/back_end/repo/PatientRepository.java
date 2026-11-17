package com.project.back_end.repo;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** Derived query: SELECT ... WHERE email = ? */
    Patient findByEmail(String email);

    /** Derived query: SELECT ... WHERE email = ? OR phone = ?, used to block duplicate sign-ups. */
    Patient findByEmailOrPhone(String email, String phone);
}
