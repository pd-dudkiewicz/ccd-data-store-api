package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseEventBuilder.anCaseEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewBuilder.aCaseView;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseViewTriggerBuilder.aViewTrigger;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

class AuthorisedGetCaseViewOperationTest {
    private static final String JURISDICTION_ID = "Probate";
    private static final String CASE_TYPE_ID = "Grant";
    private static final String CASE_ID = "1226";
    private static final String CASE_REFERENCE = "1111222233334444";
    private static final Long EVENT_ID = 100L;
    private static final String STATE = "Plop";
    private static final String USERNAME = "26";
    private static final ProfileCaseState caseState = new ProfileCaseState(STATE, STATE, STATE, STATE);
    private static final String ROLE_IN_USER_ROLES = "caseworker-probate-loa1";
    private static final String ROLE_IN_USER_ROLES_2 = "caseworker-divorce-loa";
    private static final String ROLE_IN_CASE_ROLES = "[CLAIMANT]";
    private static final String ROLE_IN_CASE_ROLES_2 = "[DEFENDANT]";
    private static final Set<String> USER_ROLES = newHashSet(ROLE_IN_USER_ROLES, ROLE_IN_USER_ROLES_2);
    private static final String EVENT_ID_STRING = valueOf(EVENT_ID);
    private static final CaseViewTrigger[] EMPTY_TRIGGERS = new CaseViewTrigger[]{};
    private static final CaseEvent CASE_EVENT = anCaseEvent().withId(EVENT_ID_STRING).build();
    private static final CaseEvent CASE_EVENT_2 = anCaseEvent().withId("event2").build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER = aViewTrigger().withId(EVENT_ID_STRING).build();
    private static final CaseViewTrigger CASE_VIEW_TRIGGER_2 = aViewTrigger().withId("event2").build();
    private static final CaseViewTrigger[] AUTH_CASE_VIEW_TRIGGERS = new CaseViewTrigger[]{CASE_VIEW_TRIGGER};
    private static final CaseDetails CASE_DETAILS = newCaseDetails().withId(CASE_ID).build();
    private static final Jurisdiction jurisdiction = newJurisdiction()
        .withJurisdictionId(JURISDICTION_ID)
        .withName(JURISDICTION_ID)
        .build();
    private static final CaseType TEST_CASE_TYPE = newCaseType()
        .withId(CASE_TYPE_ID)
        .withJurisdiction(jurisdiction)
        .withEvent(CASE_EVENT)
        .withEvent(CASE_EVENT_2)
        .build();
    private static final CaseViewType TEST_CASE_VIEW_TYPE = CaseViewType.createFrom(TEST_CASE_TYPE);
    private static final CaseView TEST_CASE_VIEW = aCaseView()
        .withCaseId(CASE_REFERENCE)
        .withState(caseState)
        .withCaseViewType(TEST_CASE_VIEW_TYPE)
        .withCaseViewTrigger(CASE_VIEW_TRIGGER)
        .withCaseViewTrigger(CASE_VIEW_TRIGGER_2)
        .build();

    @Mock
    private GetCaseViewOperation getCaseViewOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseUserRepository caseUserRepository;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Spy
    @InjectMocks
    private AuthorisedGetCaseViewOperation authorisedGetCaseViewOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_CASE_TYPE).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(USER_ROLES).when(userRepository).getUserRoles();
        doReturn(USERNAME).when(userRepository).getUserName();
        doReturn(Optional.of(CASE_DETAILS)).when(caseDetailsRepository).findByReference(JURISDICTION_ID, Long.valueOf(CASE_REFERENCE));

        TEST_CASE_VIEW.setCaseType(TEST_CASE_VIEW_TYPE);

        doReturn(TEST_CASE_VIEW).when(getCaseViewOperation).execute(CASE_REFERENCE);
    }

    @Test
    @DisplayName("should call not-deprecated #execute(caseReference)")
    void shouldCallNotDeprecatedExecute() {
        final CaseView expectedCaseView = new CaseView();
        doReturn(expectedCaseView).when(authorisedGetCaseViewOperation).execute(CASE_REFERENCE);

        final CaseView actualCaseView = authorisedGetCaseViewOperation.execute(JURISDICTION_ID, CASE_TYPE_ID, CASE_REFERENCE);

        assertAll(
            () -> verify(authorisedGetCaseViewOperation).execute(CASE_REFERENCE),
            () -> assertThat(actualCaseView, sameInstance(expectedCaseView))
        );
    }

    @Test
    @DisplayName("should fail when no READ access type on case type")
    void shouldFailWhenWhenNoReadAccess() {
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);

        assertThrows(ResourceNotFoundException.class, () -> authorisedGetCaseViewOperation.execute(CASE_REFERENCE));
    }

    @Test
    @DisplayName("should remove all case view triggers when no UPDATE access type on case type")
    void shouldRemoveCaseViewTriggersWhenNoUpdateAccessForCaseType() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(false).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);

        assertThat(caseView.getTriggers(), arrayWithSize(0));
    }

    @Test
    @DisplayName("should remove all case view triggers when no UPDATE access type on case state")
    void shouldRemoveCaseViewTriggersWhenNoUpdateAccessForState() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(false).when(accessControlService).canAccessCaseStateWithCriteria(STATE, TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);

        assertThat(caseView.getTriggers(), arrayWithSize(0));
    }

    @Test
    @DisplayName("should return case view triggers when there is CREATE access for relevant events")
    void shouldReturnCaseViewTriggersAuthorisedByAccess() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(true).when(accessControlService).canAccessCaseStateWithCriteria(STATE, TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(AUTH_CASE_VIEW_TRIGGERS).when(accessControlService).filterCaseViewTriggersByCreateAccess(TEST_CASE_VIEW.getTriggers(), TEST_CASE_TYPE.getEvents(), USER_ROLES);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);
        assertThat(caseView.getTriggers(), arrayWithSize(1));
        assertThat(caseView.getTriggers(), arrayContaining(CASE_VIEW_TRIGGER));
    }

    @Test
    @DisplayName("returns empty case view triggers when no CREATE access for relevant events")
    void shouldReturnEmptyCaseViewTriggersWhenNotAuthorisedByAccess() {
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_READ);
        doReturn(true).when(accessControlService).canAccessCaseTypeWithCriteria(TEST_CASE_TYPE, USER_ROLES, CAN_UPDATE);
        doReturn(EMPTY_TRIGGERS).when(accessControlService).filterCaseViewTriggersByCreateAccess(TEST_CASE_VIEW.getTriggers(), TEST_CASE_TYPE.getEvents(), USER_ROLES);

        CaseView caseView = authorisedGetCaseViewOperation.execute(CASE_REFERENCE);

        assertThat(caseView.getTriggers(), arrayWithSize(0));
    }

    @Test
    @DisplayName("get User Roles must merge user roles and case roles")
    void shouldMergeRoles() {
        doReturn(Arrays.asList(ROLE_IN_CASE_ROLES, ROLE_IN_CASE_ROLES_2)).when(caseUserRepository).findCaseRolesUserHasForACase(Long.valueOf(CASE_REFERENCE), USERNAME);

        Set<String> userRoles = authorisedGetCaseViewOperation.getUserRoles(CASE_REFERENCE);

        assertAll(
            () -> assertThat(userRoles.size(), is(4)),
            () -> assertThat(userRoles, hasItems(ROLE_IN_USER_ROLES, ROLE_IN_USER_ROLES, ROLE_IN_CASE_ROLES, ROLE_IN_CASE_ROLES_2))
        );
    }

    @Test
    @DisplayName("should return Case Type")
    void shouldReturnCaseType() {
        CaseType caseType = authorisedGetCaseViewOperation.getCaseType(CASE_TYPE_ID);

        assertThat(caseType, is(TEST_CASE_TYPE));
    }

    @Test
    @DisplayName("shoudl throw excetpion when case Type is invalid")
    void shouldThrowExceptionforInvalidCaseType() {
        doReturn(null).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);

        assertThrows(ValidationException.class, () -> authorisedGetCaseViewOperation.getCaseType(CASE_TYPE_ID));
    }

    @Test
    @DisplayName("should return Case ID")
    void shouldReturnCaseId() {
        String caseId = authorisedGetCaseViewOperation.getCaseId(JURISDICTION_ID, CASE_REFERENCE);

        assertThat(caseId, is(CASE_ID));
    }

    @Test
    @DisplayName("should throw exception when case reference is invalid")
    void shouldThrowException() {
        doReturn(Optional.empty()).when(caseDetailsRepository).findByReference(JURISDICTION_ID,Long.valueOf(CASE_REFERENCE));

        assertThrows(CaseNotFoundException.class, () -> authorisedGetCaseViewOperation.getCaseId(JURISDICTION_ID, CASE_REFERENCE));
    }
}
