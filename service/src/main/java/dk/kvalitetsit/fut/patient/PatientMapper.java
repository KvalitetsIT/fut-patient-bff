package dk.kvalitetsit.fut.patient;

import org.hl7.fhir.r4.model.Patient;
import org.openapitools.model.PatientDto;

public class PatientMapper {

    public static PatientDto mapPatient(Patient patient) {
        PatientDto result = new PatientDto();

        result.setId(patient.getIdElement().toUnqualifiedVersionless().getIdPart());
        result.setFirstName(patient.getNameFirstRep().getGivenAsSingleString());
        result.setLastName(patient.getNameFirstRep().getNameAsSingleString());
        result.setCpr(patient.getIdentifierFirstRep().getValue());

        return result;
    }
}
