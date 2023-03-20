package dk.kvalitetsit.fut.patient;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openapitools.model.PatientDto;

public interface PatientService {
    PatientDto getPatient(String patientId, String careTeamId) throws JsonProcessingException;
}
