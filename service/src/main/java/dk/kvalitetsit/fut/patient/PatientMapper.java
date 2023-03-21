package dk.kvalitetsit.fut.patient;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openapitools.model.PatientDto;
import org.openapitools.model.QuestionnaireDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatientMapper {
    private static final Logger logger = LoggerFactory.getLogger(PatientMapper.class);

    public static PatientDto mapPatient(Patient patient) {
        PatientDto result = new PatientDto();

        result.setId(patient.getIdElement().toUnqualifiedVersionless().getIdPart());
        result.setFirstName(patient.getNameFirstRep().getGivenAsSingleString());
        result.setLastName(patient.getNameFirstRep().getNameAsSingleString());
        result.setCpr(patient.getIdentifierFirstRep().getValue());

        return result;
    }
}
