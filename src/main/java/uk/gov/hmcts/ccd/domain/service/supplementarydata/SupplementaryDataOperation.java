package uk.gov.hmcts.ccd.domain.service.supplementarydata;

import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

public interface SupplementaryDataOperation {

    SupplementaryData updateSupplementaryData(String caseReference, SupplementaryDataUpdateRequest supplementaryData);

}
