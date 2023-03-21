package dk.kvalitetsit.fut.auth;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfiguration {

    @Value("${auth.username}")
    public String username;

    @Value("${auth.password}")
    public String password;

    @Value("${auth.patient.cpr}")
    public String cpr;

    @Value("${auth.token.url}")
    private String authTokenUrl;

    @Value("${auth.userinfo.url}")
    private String authUserinfoUrl;

    @Value("${auth.context.url}")
    private String authContextUrl;

    @Bean
    public AuthService getAuthService() {
        return new AuthService(username, password, cpr, authTokenUrl, authUserinfoUrl, authContextUrl);
    }

    @Bean
    public FhirContext getFhirContext() {
        return FhirContext.forR4();
    }
}
