package com.grupo6.clinicalhistory.application.port.out;

import com.grupo6.clinicalhistory.domain.Doctor;

import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository {

    Doctor save(Doctor doctor);

    Optional<Doctor> findById(UUID id);

    Optional<Doctor> findByProfessionalLicense(String professionalLicense);
}
