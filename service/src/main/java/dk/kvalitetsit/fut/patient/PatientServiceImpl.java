package dk.kvalitetsit.fut.patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.fut.auth.AuthService;
import org.hl7.fhir.r4.model.*;
import org.openapitools.model.PatientDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class PatientServiceImpl implements PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientServiceImpl.class);
    private FhirContext fhirContext;
    private String patientServiceUrl;
    private String organizationServiceUrl;
    private AuthService authService;

    public PatientServiceImpl(FhirContext fhirContext, String patientServiceUrl, String organizationServiceUrl, AuthService authService) {
        this.fhirContext = fhirContext;
        this.patientServiceUrl = patientServiceUrl;
        this.organizationServiceUrl = organizationServiceUrl;
        this.authService = authService;
    }

    @Override
    public PatientDto getPatient(String patientId, String careTeamId) throws JsonProcessingException {
        String careTeamResource = organizationServiceUrl + "CareTeam/" + careTeamId;

        AuthService.Token token = authService.getToken();
        token = authService.refreshTokenWithCareTeamAndPatientContext(
                token, careTeamResource, patientId);

        IGenericClient client = getFhirClient(token);
        try {
            Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
            return PatientMapper.mapPatient(patient);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return null;
    }

    private <T extends Resource> List<T> lookupByCriteria(Class<T> resourceClass, List<ICriterion> criteria) {
        IGenericClient client = getFhirClient(authService.getToken());
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

    private IGenericClient getFhirClient(AuthService.Token token) {
        IGenericClient client = fhirContext.newRestfulGenericClient(patientServiceUrl);
        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token.accessToken());
        client.registerInterceptor(authInterceptor);
        return client;
    }
}
