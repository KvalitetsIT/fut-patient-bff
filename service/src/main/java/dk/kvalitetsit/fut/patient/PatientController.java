package dk.kvalitetsit.fut.patient;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openapitools.api.PatientApi;
import org.openapitools.model.PatientDto;
import org.openapitools.model.QuestionnaireDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PatientController implements PatientApi {
    private final PatientService patientService;
    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Override
    public ResponseEntity<PatientDto> v1GetPatient(String patientId, String careTeamId) {
        PatientDto patientDto = null;
        try {
            patientDto = patientService.getPatient(patientId, careTeamId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(patientDto);
    }

    @Override
    public ResponseEntity<List<QuestionnaireDto>> v1GetPatientQuestionnary(String patientId) {
        return ResponseEntity.ok(patientService.getQuestionnaries(patientId));
    }

}
