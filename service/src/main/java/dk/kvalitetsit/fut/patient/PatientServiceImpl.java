package dk.kvalitetsit.fut.patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.fut.auth.AuthService;
import dk.kvalitetsit.fut.exception.NotSupportedException;
import org.hl7.fhir.r4.model.*;
import org.openapitools.model.PatientDto;
import org.openapitools.model.QuestionnaireDto;
import org.openapitools.model.QuestionnaireItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class PatientServiceImpl implements PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);
    private FhirContext fhirContext;
    private final String patientServiceUrl;
    private final String organizationServiceUrl;
    private final String careplanServiceUrl;
    private final String planServiceUrl;
    private final String questionnaireServiceUrl;
    private AuthService authService;

    public PatientServiceImpl(FhirContext fhirContext,
                              String patientServiceUrl,
                              String organizationServiceUrl,
                              String careplanServiceUrl,
                              String planServiceUrl,
                              String questionnaireServiceUrl,
                              AuthService authService) {
        this.fhirContext = fhirContext;
        this.patientServiceUrl = patientServiceUrl;
        this.organizationServiceUrl = organizationServiceUrl;
        this.planServiceUrl = planServiceUrl;
        this.careplanServiceUrl = careplanServiceUrl;
        this.questionnaireServiceUrl = questionnaireServiceUrl;
        this.authService = authService;
    }

    @Override
    public PatientDto getPatient(String patientId, String careTeamId) throws JsonProcessingException {
        String careTeamResource = organizationServiceUrl + "CareTeam/" + careTeamId;

        AuthService.Token token = authService.getToken();
        token = authService.refreshTokenWithCareTeamAndPatientContext(
                token, careTeamResource, patientId);

        IGenericClient client = getFhirClient(token, patientServiceUrl);
        try {
            Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
            return PatientMapper.mapPatient(patient);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return null;
    }

    @Override
    public List<QuestionnaireDto> getQuestionnaries(String patientId) {
        AuthService.Token token = authService.getToken();
        IGenericClient clientCareplan = getFhirClient(token, careplanServiceUrl);
        IGenericClient clientPlan = getFhirClient(token, planServiceUrl);
        String patientUrl = patientServiceUrl + "Patient/" + patientId;
        Set<QuestionnaireDto> returnSet = new LinkedHashSet<>();

        Reference patientReference = new Reference(patientUrl);
        Parameters parameters = new Parameters();
        Date dateOneMonthAgo = Date.from(LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        parameters.addParameter().setName("patient").setValue(patientReference);
        parameters.addParameter().setName("start").setValue(new DateTimeType(dateOneMonthAgo));
        parameters.addParameter().setName("end").setValue(new DateTimeType(new Date()));

        Bundle result = clientCareplan.operation()
                .onServer()
                .named("$get-patient-procedures")
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();

        List<ServiceRequest> list = result.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r.getClass().equals(ServiceRequest.class))
                .map(sr -> (ServiceRequest) sr)
                .toList();

        for (ServiceRequest sr : list) {
            CanonicalType ct = sr.getInstantiatesCanonical().get(0);
            ActivityDefinition activityDefinition = clientPlan
                    .read()
                    .resource(ActivityDefinition.class)
                    .withUrl(ct.getValue()).execute();

            for (RelatedArtifact artefact : activityDefinition.getRelatedArtifact()) {
                if (artefact.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)) {
                    logger.info(artefact.getResource());
                    returnSet.add(getQuestionnarie(artefact.getResource()));
                }
            }
        }
        return returnSet.stream().toList();
    }

    public QuestionnaireDto getQuestionnarie(String resource) {
        logger.info(resource);

        AuthService.Token token = authService.getToken();
        IGenericClient client = getFhirClient(token, questionnaireServiceUrl);
        Questionnaire questionnaire = client
                .read()
                .resource(Questionnaire.class)
                .withUrl(resource)
                .execute();

        QuestionnaireDto dto = new QuestionnaireDto();
        dto.setResource(resource);
        dto.setDescription(questionnaire.getDescription());
        dto.setTitle(questionnaire.getTitle());
        dto.setItems(questionnaire.getItem().stream().map((item -> {
            QuestionnaireItemDto itemDto = new QuestionnaireItemDto();

            // TODO: Only "type": "CHOICE" is supported for now.
            // How to handle the rest?
            if (!item.getType().name().equals("CHOICE")) {
                throw new NotSupportedException();
            }

            itemDto.setType(item.getType().name());
            itemDto.setRequired(item.getRequired());
            itemDto.setText(item.getText());
            itemDto.setAnswerOptions(item.getAnswerOption().stream().map(
                    o -> o.getValueStringType().getValueAsString()).toList());

            return itemDto;
        })).toList());
        return dto;
    }

    private <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, List<ICriterion> criteria) {
        IGenericClient client = getFhirClient(authService.getToken(), patientServiceUrl);
        IQuery<Bundle> query = client
                .search()
                .forResource(resourceClass)
                .returnBundle(Bundle.class);

        if (!criteria.isEmpty()) {
            query = query.where(criteria.get(0));
            for(int i = 1; i < criteria.size(); i++) {
                query = query.and(criteria.get(i));
            }
        }

        Bundle result = query.execute();

        return result.getEntry().stream()
                .map(bundleEntryComponent -> (T)bundleEntryComponent.getResource())
                .collect(Collectors.toList());
    }

    private IGenericClient getFhirClient(AuthService.Token token, String url) {
        IGenericClient client = fhirContext.newRestfulGenericClient(url);
        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token.accessToken());
        client.registerInterceptor(authInterceptor);
        return client;
    }
}
