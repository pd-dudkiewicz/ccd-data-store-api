package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@Singleton
public class CaseSanitiser {

    private final Map<String, Sanitiser> sanitisers = new HashMap<>();

    @Inject
    public CaseSanitiser(List<Sanitiser> sanitisers) {
        sanitisers.forEach(sanitiser -> {
            this.sanitisers.put(sanitiser.getType(), sanitiser);
        });
    }

    public Map<String, JsonNode> sanitise(final CaseTypeDefinition caseTypeDefinition, final Map<String, JsonNode> caseData) {

        final Map<String, JsonNode> sanitisedData = new HashMap<>();

        if (null == caseData) {
            return sanitisedData;
        }

        final Map<String, CaseFieldDefinition> fieldsMap = new HashMap<>();

        caseTypeDefinition.getCaseFieldDefinitions().forEach(field -> {
            fieldsMap.put(field.getId(), field);
        });

        caseData.forEach((key, value) -> {
            if (fieldsMap.containsKey(key)) {
                final CaseFieldDefinition caseFieldDefinition = fieldsMap.get(key);
                final FieldType fieldType = caseFieldDefinition.getFieldType();

                if (sanitisers.containsKey(fieldType.getType())) {
                    final Sanitiser sanitiser = sanitisers.get(fieldType.getType());
                    sanitisedData.put(key, sanitiser.sanitise(fieldType, value));
                } else {
                    sanitisedData.put(key, value);
                }
            }
        });

        return sanitisedData;
    }

}
