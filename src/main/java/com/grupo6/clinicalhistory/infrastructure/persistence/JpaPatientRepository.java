package com.grupo6.clinicalhistory.infrastructure.persistence;

import com.grupo6.clinicalhistory.application.port.out.PatientRepository;
import com.grupo6.clinicalhistory.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA de patients. Implementa el puerto de salida de aplicación.
 */
@Repository
public interface JpaPatientRepository extends JpaRepository<Patient, UUID>, PatientRepository {

    @Override
    Optional<Patient> findByIdentification(String identification);
}
