package com.bookly.backendcf.clinicalhistory.application.port.out;

import com.bookly.backendcf.clinicalhistory.domain.Patient;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {

    Patient save(Patient patient);

    Optional<Patient> findById(UUID id);

    Optional<Patient> findByIdentification(String identification);
}
