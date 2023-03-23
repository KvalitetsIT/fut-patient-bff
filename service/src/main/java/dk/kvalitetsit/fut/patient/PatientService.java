package dk.kvalitetsit.fut.patient;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openapitools.model.CreateQuestionnaireResponseDto;
import org.openapitools.model.PatientDto;
import org.openapitools.model.QuestionnaireDto;

import java.util.List;

public interface PatientService {
    PatientDto getPatient(String patientId, String careTeamId) throws JsonProcessingException;
    List<QuestionnaireDto> getQuestionnaries(String patientId);
    String createQuestionnaireResponse(String patientId, CreateQuestionnaireResponseDto dto);
}
