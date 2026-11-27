package com.grupo6.clinicalhistory;

import com.grupo6.clinicalhistory.application.FindClinicalHistoriesService;
import com.grupo6.clinicalhistory.application.RegisterClinicalHistoryService;
import com.grupo6.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.grupo6.clinicalhistory.domain.ClinicalHistory;
import com.grupo6.clinicalhistory.domain.Gender;
import com.grupo6.clinicalhistory.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El arranque del contexto ya ejecuta Flyway (V1) y Hibernate ddl-auto=validate:
 * si las entidades no coinciden con el SQL, el contexto no inicia.
 */
@SpringBootTest
@Transactional
class ClinicalHistoryPersistenceTest {

    private static final Instant CONSULTATION_DATE = Instant.parse("2026-01-15T10:00:00Z");

    @Autowired
    private RegisterClinicalHistoryService registerService;

    @Autowired
    private FindClinicalHistoriesService findService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void lasTablasDeLaMigracionExisten() {
        List<String> tablas = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);
        assertThat(tablas).contains("patients", "doctors", "clinical_histories");
    }

    @Test
    void registraYConsultaHistoriaPorCedula() {
        ClinicalHistory guardada = registerService.register(validCommand());

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getCreatedAt()).isNotNull();
        assertThat(guardada.getPatient().getIdentification()).isEqualTo("1234567890");
        assertThat(guardada.getDoctor().getProfessionalLicense()).isEqualTo("LP-1234");

        Page<ClinicalHistory> page = findService.findByIdentification("1234567890", 0, 10);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(guardada.getId());
        assertThat(findService.findById(guardada.getId())).isPresent();
    }

    @Test
    void reutilizaPacienteYDoctorExistente() {
        registerService.register(validCommand());
        registerService.register(validCommand());

        Page<ClinicalHistory> page = findService.findByIdentification("1234567890", 0, 10);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from patients", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from doctors", Integer.class)).isEqualTo(1);
    }

    @Test
    void rechazaCedulaInvalida() {
        assertThatThrownBy(() -> registerService.register(command("abc", 30, Gender.FEMALE, "Gripe", CONSULTATION_DATE)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("cédula");
    }

    @Test
    void rechazaEdadFueraDelRango() {
        assertThatThrownBy(() -> registerService.register(command("1234567890", -1, Gender.FEMALE, "Gripe", CONSULTATION_DATE)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> registerService.register(command("1234567890", 151, Gender.FEMALE, "Gripe", CONSULTATION_DATE)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rechazaGeneroNulo() {
        assertThatThrownBy(() -> registerService.register(command("1234567890", 30, null, "Gripe", CONSULTATION_DATE)))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        assertThatThrownBy(() -> registerService.register(
                command("1234567890", 30, Gender.FEMALE, "   ", CONSULTATION_DATE)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> registerService.register(
                command("1234567890", 30, Gender.FEMALE, "Gripe", null)))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> registerService.register(new RegisterClinicalHistoryCommand(
                null,
                new RegisterClinicalHistoryCommand.DoctorData("Dr. Casa", "LP-1234", "General"),
                new RegisterClinicalHistoryCommand.HistoryData("Motivo", "Gripe", "Reposo", CONSULTATION_DATE))))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rechazaConsultaConCedulaInvalida() {
        assertThatThrownBy(() -> findService.findByIdentification("!!", 0, 10))
                .isInstanceOf(DomainValidationException.class);
    }

    private static RegisterClinicalHistoryCommand validCommand() {
        return command("1234567890", 30, Gender.FEMALE, "Gripe", CONSULTATION_DATE);
    }

    private static RegisterClinicalHistoryCommand command(String cedula, int age, Gender gender,
                                                          String diagnosis, Instant consultationDate) {
        return new RegisterClinicalHistoryCommand(
                new RegisterClinicalHistoryCommand.PatientData(cedula, "Ana", "Pérez", age, gender),
                new RegisterClinicalHistoryCommand.DoctorData("Dr. Casa", "LP-1234", "Medicina General"),
                new RegisterClinicalHistoryCommand.HistoryData("Dolor de cabeza", diagnosis, "Reposo", consultationDate));
    }
}
