package dk.kvalitetsit.fut.patient;

import ca.uhn.fhir.context.FhirContext;
import dk.kvalitetsit.fut.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PatientConfiguration implements WebMvcConfigurer {
    @Value("${organization.fhir.service.endpoint}")
    private String organizationServiceUrl;
    @Value("${patient.fhir.service.endpoint}")
    private String patientServiceUrl;

    @Bean
    public PatientServiceImpl patientService(@Autowired AuthService authService, @Autowired FhirContext fhirContext) {
        return new PatientServiceImpl(fhirContext, patientServiceUrl, organizationServiceUrl, authService);
    }
}
