package io.nuvalence.user.management.api.service.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.user.management.api.service.audit.profile.EmployerProfileDataChangedAuditHandler;
import io.nuvalence.user.management.api.service.entity.profile.Address;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.events.model.AuditEventRequestObjectDto;
import io.nuvalence.user.management.api.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.user.management.api.service.service.AuditEventService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmployerProfileDataChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private EmployerProfileDataChangedAuditHandler auditHandler;

    private static final String LEGAL_NAME = "legalName";

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        EmployerProfile employer = createEmployer();

        auditHandler.handlePreUpdateState(employer);
        employer.setLegalName("New Name");
        auditHandler.handlePostUpdateState(employer);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put(LEGAL_NAME, LEGAL_NAME);

        Map<String, String> after = new HashMap<>();
        after.put(LEGAL_NAME, "New Name");

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(employer.getId(), capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.EMPLOYER, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                String.format("Data for employer profile %s changed.", employer.getId()),
                capturedEvent.getSummary());

        StateChangeEventData eventDataResult = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals(
                before.toString(),
                eventDataResult.getOldState().replace("\"", "").replace(":", "="));
        Assertions.assertEquals(
                after.toString(),
                eventDataResult.getNewState().replace("\"", "").replace(":", "="));
    }

    @Test
    void test_publishAuditEvent_WithNoChanges() {
        EmployerProfile employer = createEmployer();

        auditHandler.handlePreUpdateState(employer);
        auditHandler.handlePostUpdateState(employer);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }

    private EmployerProfile createEmployer() {
        Address address =
                Address.builder()
                        .id(UUID.randomUUID())
                        .address1("addressLine1")
                        .address2("addressLine2")
                        .city("city")
                        .state("state")
                        .postalCode("zipCode")
                        .build();

        return EmployerProfile.builder()
                .id(UUID.randomUUID())
                .fein("fein")
                .legalName(LEGAL_NAME)
                .otherNames(Collections.EMPTY_LIST)
                .type("LLC")
                .industry("industry")
                .businessPhone("businessPhone")
                .summaryOfBusiness("summaryOfBusiness")
                .mailingAddress(address)
                .locations(List.of(address))
                .build();
    }
}
