package dk.kvalitetsit.fut.patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.util.BundleBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.fut.auth.AuthService;
import dk.kvalitetsit.fut.exception.NotSupportedException;
import org.hl7.fhir.r4.model.*;
import org.openapitools.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class PatientServiceImpl implements PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);;
    private FhirContext fhirContext;
    private final String patientServiceUrl;
    private final String organizationServiceUrl;
    private final String careplanServiceUrl;
    private final String planServiceUrl;
    private final String questionnaireServiceUrl;
    private final String measurementServiceUrl;
    private AuthService authService;

    public PatientServiceImpl(FhirContext fhirContext,
                              String patientServiceUrl,
                              String organizationServiceUrl,
                              String careplanServiceUrl,
                              String planServiceUrl,
                              String questionnaireServiceUrl,
                              String measurementServiceUrl,
                              AuthService authService) {
        this.fhirContext = fhirContext;
        this.patientServiceUrl = patientServiceUrl;
        this.organizationServiceUrl = organizationServiceUrl;
        this.planServiceUrl = planServiceUrl;
        this.careplanServiceUrl = careplanServiceUrl;
        this.questionnaireServiceUrl = questionnaireServiceUrl;
        this.measurementServiceUrl = measurementServiceUrl;
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

        List<ServiceRequest> serviceRequests = result.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(r -> r.getClass().equals(ServiceRequest.class))
                .map(sr -> (ServiceRequest) sr)
                .toList();


        for (ServiceRequest sr : serviceRequests) {
            Extension ext = sr.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/workflow-episodeOfCare");
            String srUrl = careplanServiceUrl + "ServiceRequest/" + sr.getIdElement().getIdPart();

            // How else to get the reference from the value?
            String eoc = ((Reference) ext.getValue()).getReference();

            CanonicalType ct = sr.getInstantiatesCanonical().get(0);
            ActivityDefinition activityDefinition = clientPlan
                    .read()
                    .resource(ActivityDefinition.class)
                    .withUrl(ct.getValue()).execute();

            for (RelatedArtifact artefact : activityDefinition.getRelatedArtifact()) {
                if (artefact.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)) {
                    logger.info(artefact.getResource());
                    returnSet.add(getQuestionnarie(
                            artefact.getResource(),
                            eoc,
                            srUrl,
                            patientId
                    ));
                }
            }
        }
        return returnSet.stream().toList();
    }

    public QuestionnaireDto getQuestionnarie(String resource, String episodeOfCare,
                                             String serviceRequest, String patientId) {
        logger.info(resource);

        AuthService.Token token = authService.getToken();
        IGenericClient client = getFhirClient(token, questionnaireServiceUrl);
        Questionnaire questionnaire = client
                .read()
                .resource(Questionnaire.class)
                .withUrl(resource)
                .execute();

        QuestionnaireDto dto = new QuestionnaireDto();
        dto.setEpisodeOfCare(episodeOfCare);
        dto.setServiceRequest(serviceRequest);
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

            itemDto.setLinkId(item.getLinkId());
            itemDto.setType(item.getType().name());
            itemDto.setRequired(item.getRequired());
            itemDto.setText(item.getText());
            itemDto.setAnswerOptions(item.getAnswerOption().stream().map(
                    o -> o.getValueStringType().getValueAsString()).toList());

            return itemDto;
        })).toList());
        return dto;
    }

    @Override
    public String createQuestionnaireResponse(String patientId, CreateQuestionnaireResponseDto dto) {
        String questionnaire = dto.getResource();
        String episodeOfCare = dto.getEpisodeOfCare();
        String serviceRequest = dto.getServiceRequest();
        List<CreateQuestionnaireResponseItemDto> answers =  dto.getItems();

        AuthService.Token token = authService.getTokenWithEpisodeOfCareContext(
                authService.USERNAME,
                authService.PASSWORD,
                episodeOfCare);
        IGenericClient client = getFhirClient(token, measurementServiceUrl);

        // Create the QuestionnaireResponse resource
        QuestionnaireResponse qr = new QuestionnaireResponse();
        Meta meta = new Meta();
        meta.addProfile("http://ehealth.sundhed.dk/fhir/StructureDefinition/ehealth-questionnaireresponse");
        qr.setMeta(meta);
        qr.addBasedOn(new Reference(serviceRequest));
        qr.setQuestionnaire(questionnaire);
        qr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        qr.setSubject(new Reference("https://patient.devenvcgi.ehealth.sundhed.dk/fhir/Patient/258981"));
        qr.setAuthored(new Date());
        qr.setSource(new Reference("https://patient.devenvcgi.ehealth.sundhed.dk/fhir/Patient/258981"));

        // Set the extensions
        List<Extension> extensions = new ArrayList<>();
        qr.setExtension(extensions);

        // Extension 1
        Extension ext1 = new Extension("http://hl7.org/fhir/StructureDefinition/workflow-episodeOfCare",
                new Reference(episodeOfCare));
        extensions.add(ext1);

        // Extension 2
        Extension ext2 = new Extension();
        ext2.setUrl("http://ehealth.sundhed.dk/fhir/StructureDefinition/ehealth-quality");
        ext2.addExtension(new Extension("qualityType",
                new CodeableConcept(new Coding(
                        "http://ehealth.sundhed.dk/cs/quality-types",
                        "TBD", ""))));
        ext2.addExtension(new Extension("qualityCode",
                new CodeableConcept(new Coding(
                        "http://ehealth.sundhed.dk/cs/usage-quality",
                        "TBD", ""))));
        extensions.add(ext2);

        // Extension 3
        Extension ext3 = new Extension();
        ext3.setUrl("http://ehealth.sundhed.dk/fhir/StructureDefinition/ehealth-resolved-timing");
        Extension ext3inner1 = new Extension();
        // "42" is from the example in the docs
        ext3inner1.setValue(new IdType(42)).setUrl("serviceRequestVersionId");
        ext3.addExtension(ext3inner1);
        Extension ext3inner2 = new Extension();
        ext3inner2.setValue(new CodeableConcept(new Coding(
                "http://ehealth.sundhed.dk/cs/resolved-timing-type",
                "Adhoc", ""))).setUrl("type");
        ext3.addExtension(ext3inner2);
        extensions.add(ext3);

        // Create answers
        for (CreateQuestionnaireResponseItemDto answerItem : answers) {
            List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items = new ArrayList<>();
            QuestionnaireResponse.QuestionnaireResponseItemComponent qric =
                    new QuestionnaireResponse.QuestionnaireResponseItemComponent();
            items.add(qric);
            qric.setLinkId(answerItem.getLinkId());

            for (String text : answerItem.getAnswers()) {
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent a =
                        new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();
                a.setValue(new StringType().setValue(text));
                qric.addAnswer(a);
            }

            qr.setItem(items);
        }

        // Built bundle
        BundleBuilder builder = new BundleBuilder(fhirContext);
        builder.addTransactionCreateEntry(qr);
        Bundle bundle = (Bundle) builder.getBundle(); // Why is return type IBaseBundle?

        // Work-around: FullUrl will be overwritten by the server.
        bundle.getEntry().get(0).setFullUrl("42");

        Parameters parameters = new Parameters();
        parameters.addParameter().setResource(bundle).setName("measurement");

        Bundle result = client.operation()
                .onServer()
                .named("$submit-measurement")
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();

        return result.getEntry().get(0).getResponse().getLocation();
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
