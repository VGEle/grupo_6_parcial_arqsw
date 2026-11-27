
package com.bookly.backendcf.clinicalhistory.presentation.graphql;

import com.bookly.backendcf.clinicalhistory.application.FindClinicalHistoriesService;
import com.bookly.backendcf.clinicalhistory.application.RegisterClinicalHistoryService;
import com.bookly.backendcf.clinicalhistory.application.command.RegisterClinicalHistoryCommand;
import com.bookly.backendcf.clinicalhistory.domain.ClinicalHistory;
import com.bookly.backendcf.clinicalhistory.domain.Gender;
import com.bookly.backendcf.clinicalhistory.domain.exception.DomainValidationException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Controller
public class ClinicalHistoryGraphqlController {

    private final FindClinicalHistoriesService finder;
    private final RegisterClinicalHistoryService registrar;

    public ClinicalHistoryGraphqlController(
            FindClinicalHistoriesService finder,
            RegisterClinicalHistoryService registrar) {
        this.finder = finder;
        this.registrar = registrar;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @QueryMapping
    public Map<String, Object> clinicalHistoriesByIdentification(
            @Argument String identification,
            @Argument Integer first,
            @Argument String after) {

        int size = first == null ? 10 : first;

        if (size < 1 || size > 100) {
            throw new DomainValidationException(
                    "first debe estar entre 1 y 100");
        }

        int pageNumber = decodePage(after);

        Page<ClinicalHistory> page =
                finder.findByIdentification(
                        identification, pageNumber, size);

        List<Map<String, Object>> nodes = page.getContent()
                .stream()
                .map(this::toGraphql)
                .toList();

        boolean hasNext = page.hasNext();

        String endCursor = hasNext
                ? encodePage(pageNumber + 1)
                : null;

        return Map.of(
                "nodes", nodes,
                "pageInfo", Map.of(
                        "hasNextPage", hasNext,
                        "endCursor", endCursor == null ? "" : endCursor
                )
        );
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
            Map<String, Object> input, String key) {

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

    private static int decodePage(String after) {

        if (after == null || after.isBlank()) {
            return 0;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(after),
                    StandardCharsets.UTF_8
            );

            if (!decoded.startsWith("page:")) {
                throw new IllegalArgumentException();
            }

            int page = Integer.parseInt(
                    decoded.substring(5));

            if (page < 0) {
                throw new IllegalArgumentException();
            }

            return page;

        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException(
                    "Cursor de paginacion invalido");
        }
    }

    private static String encodePage(int page) {

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        ("page:" + page).getBytes(
                                StandardCharsets.UTF_8));
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
