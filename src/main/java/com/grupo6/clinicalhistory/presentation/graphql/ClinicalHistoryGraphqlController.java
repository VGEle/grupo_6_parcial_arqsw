package com.grupo6.clinicalhistory.presentation.graphql;

import com.grupo6.clinicalhistory.application.FindClinicalHistoriesService;
import com.grupo6.clinicalhistory.application.RegisterClinicalHistoryService;
import com.grupo6.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.grupo6.clinicalhistory.application.page.ClinicalHistoryPage;
import com.grupo6.clinicalhistory.application.port.out.ClinicalHistoryQueryCache;
import com.grupo6.clinicalhistory.domain.ClinicalHistory;
import com.grupo6.clinicalhistory.domain.Gender;
import com.grupo6.clinicalhistory.domain.exception.DomainValidationException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ClinicalHistoryGraphqlController {

    private final FindClinicalHistoriesService finder;
    private final RegisterClinicalHistoryService registrar;
    private final ClinicalHistoryQueryCache cache;

    public ClinicalHistoryGraphqlController(
            FindClinicalHistoriesService finder,
            RegisterClinicalHistoryService registrar,
            ClinicalHistoryQueryCache cache) {
        this.finder = finder;
        this.registrar = registrar;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    @QueryMapping
    public Map<String, Object> clinicalHistoriesByIdentification(
            @Argument String identification,
            @Argument Integer first,
            @Argument String after) {

        Optional<Map<String, Object>> cached = cache.get(identification, first, after);
        if (cached.isPresent()) {
            return cached.get();
        }

        ClinicalHistoryPage page = finder.findPage(identification, first, after);

        List<Map<String, Object>> nodes = page.nodes()
                .stream()
                .map(this::toGraphql)
                .toList();

        // HashMap permite almacenar endCursor como null
        // cuando no existe una página siguiente.
        Map<String, Object> pageInfo = new HashMap<>();

        pageInfo.put("hasNextPage", page.hasNextPage());
        pageInfo.put("endCursor", page.endCursor());

        Map<String, Object> response = new HashMap<>();

        response.put("nodes", nodes);
        response.put("pageInfo", pageInfo);

        cache.put(identification, first, after, response);
        return response;
    }

    @MutationMapping
    public Map<String, Object> createClinicalHistory(
            @Argument Map<String, Object> input) {

        if (input == null) {
            throw new DomainValidationException(
                    "input es obligatorio");
        }

        Map<String, Object> patient =
                requireObject(input, "patient");

        Map<String, Object> doctor =
                requireObject(input, "doctor");

        Map<String, Object> history =
                requireObject(input, "history");

        RegisterClinicalHistoryCommand command =
                new RegisterClinicalHistoryCommand(

                        new RegisterClinicalHistoryCommand.PatientData(
                                (String) patient.get("identification"),
                                (String) patient.get("firstName"),
                                (String) patient.get("lastName"),
                                (Integer) patient.get("age"),
                                requireGender(patient.get("gender"))
                        ),

                        new RegisterClinicalHistoryCommand.DoctorData(
                                (String) doctor.get("name"),
                                (String) doctor.get("professionalLicense"),
                                (String) doctor.get("specialty")
                        ),

                        new RegisterClinicalHistoryCommand.HistoryData(
                                (String) history.get("consultationReason"),
                                (String) history.get("diagnosis"),
                                (String) history.get("treatment"),
                                requireDate(history.get("date"))
                        )
                );

        return toGraphql(registrar.register(command));
    }

    private Map<String, Object> toGraphql(
            ClinicalHistory history) {

        var patient = history.getPatient();
        var doctor = history.getDoctor();

        return Map.of(
                "id", history.getId().toString(),

                "patient", Map.of(
                        "id", patient.getId().toString(),
                        "identification", patient.getIdentification(),
                        "firstName", patient.getFirstName(),
                        "lastName", patient.getLastName(),
                        "age", patient.getAge(),
                        "gender", patient.getGender().name()
                ),

                "doctor", Map.of(
                        "id", doctor.getId().toString(),
                        "name", doctor.getName(),
                        "professionalLicense",
                        doctor.getProfessionalLicense(),
                        "specialty", doctor.getSpecialty()
                ),

                "consultationReason",
                history.getConsultationReason(),

                "diagnosis", history.getDiagnosis(),

                "treatment", history.getTreatment(),

                "date", history.getConsultationDate().toString()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireObject(
            Map<String, Object> input,
            String key) {

        Object value = input.get(key);

        if (!(value instanceof Map<?, ?>)) {
            throw new DomainValidationException(
                    "El campo " + key + " es obligatorio");
        }

        return (Map<String, Object>) value;
    }

    private static Gender requireGender(Object value) {

        if (!(value instanceof String gender)) {
            throw new DomainValidationException(
                    "El genero es obligatorio");
        }

        try {
            return Gender.valueOf(gender);
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException(
                    "Genero invalido");
        }
    }

    private static Instant requireDate(Object value) {

        if (!(value instanceof String date)) {
            throw new DomainValidationException(
                    "La fecha es obligatoria");
        }

        try {
            return Instant.parse(date);
        } catch (DateTimeParseException ex) {
            throw new DomainValidationException(
                    "La fecha debe ser ISO-8601. Ejemplo: 2026-11-27T14:00:00Z");
        }
    }

    @GraphQlExceptionHandler({
            DomainValidationException.class,
            IllegalArgumentException.class
    })
    public GraphQLError handleInvalidInput(
            Exception ex,
            DataFetchingEnvironment env) {

        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
    }
}
