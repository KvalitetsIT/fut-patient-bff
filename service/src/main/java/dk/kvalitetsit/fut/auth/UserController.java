package dk.kvalitetsit.fut.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.kvalitetsit.fut.patient.PatientService;
import org.openapitools.api.UserApi;
import org.openapitools.model.UserInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserApi {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final AuthService authService;
    private final PatientService patientService;

    public UserController(PatientService patientService, AuthService authService) {
        this.authService = authService;
        this.patientService = patientService;
    }

    @Override
    public ResponseEntity<UserInfoDto> v1GetUserInfo() {
        AuthService.Token token = authService.getToken();
        UserInfoDto userInfo = null;
        try {
            userInfo = authService.getUserInfo(token);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(userInfo);
    }
}
