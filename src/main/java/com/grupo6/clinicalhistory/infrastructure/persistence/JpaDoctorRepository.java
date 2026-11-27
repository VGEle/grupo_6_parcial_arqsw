package com.grupo6.clinicalhistory.infrastructure.persistence;

import com.grupo6.clinicalhistory.application.port.out.DoctorRepository;
import com.grupo6.clinicalhistory.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA de doctors. Implementa el puerto de salida de aplicación.
 */
@Repository
public interface JpaDoctorRepository extends JpaRepository<Doctor, UUID>, DoctorRepository {

    @Override
    Optional<Doctor> findByProfessionalLicense(String professionalLicense);
}
