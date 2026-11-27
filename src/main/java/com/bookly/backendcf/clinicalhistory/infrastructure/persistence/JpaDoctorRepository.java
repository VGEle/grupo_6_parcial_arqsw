package com.bookly.backendcf.clinicalhistory.infrastructure.persistence;

import com.bookly.backendcf.clinicalhistory.application.port.out.DoctorRepository;
import com.bookly.backendcf.clinicalhistory.domain.Doctor;
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
