package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class SearchResultDefinition implements Serializable {
    private SearchResultField[] fields;

    public SearchResultField[] getFields() {
        return fields;
    }

    public void setFields(SearchResultField[] fields) {
        this.fields = fields;
    }

    public List<SearchResultField> getFieldsWithPaths() {
        return Arrays.stream(fields)
            .filter(f -> StringUtils.isNotBlank(f.getCaseFieldPath()))
            .collect(Collectors.toList());
    }

    public Map<String, String> getFieldsUserRoles() {
        Map<String, String> fields = new HashMap<>();
        for (SearchResultField srf : getFields()) {
            fields.put(srf.getCaseFieldId(), srf.getRole());
        }
        return fields;
    }

    public boolean fieldExists(String caseFieldId) {
        Map<String, String> fields = getFieldsUserRoles();
        if (!fields.containsKey(caseFieldId)) {
            return false;
        }
        return true;
    }

    public boolean fieldHasRole(String caseFieldId, Set<String> roles) {
        Map<String, String> fields = getFieldsUserRoles();
        String role = fields.get(caseFieldId);
        if (role != null && !roles.contains(role)) {
            return false;
        }
        return true;
    }
}
