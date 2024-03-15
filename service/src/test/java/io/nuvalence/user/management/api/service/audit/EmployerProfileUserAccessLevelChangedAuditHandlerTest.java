package io.nuvalence.user.management.api.service.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.user.management.api.service.audit.profile.EmployerProfileUserAccessLevelChangedAuditHandler;
import io.nuvalence.user.management.api.service.config.SpringConfig;
import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
class EmployerProfileUserAccessLevelChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private EmployerProfileUserAccessLevelChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        UUID createdByUserId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();
        ProfileAccessLevel accessLevelOld = ProfileAccessLevel.READER;
        EmployerProfile employer =
                EmployerProfile.builder()
                        .id(UUID.randomUUID())
                        .createdBy(String.valueOf(createdByUserId))
                        .build();

        EmployerProfileLink employerUserLink =
                EmployerProfileLink.builder().profile(employer).build();
        employerUserLink.setId(UUID.randomUUID());
        PublicUser publicUser = new PublicUser();
        publicUser.setId(UUID.fromString(userId));
        employerUserLink.setUser(publicUser);
        employerUserLink.setProfileAccessLevel(accessLevelOld);

        auditHandler.handlePreUpdateState(employerUserLink);

        ProfileAccessLevel profileAccessLevelNew = ProfileAccessLevel.WRITER;
        employerUserLink.setProfileAccessLevel(profileAccessLevelNew);
        auditHandler.handlePostUpdateState(employerUserLink);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("accessLevel", accessLevelOld.toString());

        Map<String, String> after = new HashMap<>();
        after.put("accessLevel", profileAccessLevelNew.toString());

        Map<String, Object> eventData =
                Map.of(
                        "createdById", createdByUserId,
                        "userId", userId);
        String eventDataJson = SpringConfig.getMapper().writeValueAsString(eventData);

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
                String.format(
                        "Profile user access level changed to [%s] for employer profile user %s"
                                + " created by %s. Previously it was [%s]",
                        profileAccessLevelNew, userId, createdByUserId, accessLevelOld),
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
        EmployerProfileLink employerUserLink =
                EmployerProfileLink.builder()
                        .profile(
                                EmployerProfile.builder()
                                        .id(UUID.randomUUID())
                                        .createdBy(String.valueOf(UUID.randomUUID()))
                                        .build())
                        .build();
        employerUserLink.setId(UUID.randomUUID());
        PublicUser publicUser = new PublicUser();
        publicUser.setId(UUID.randomUUID());
        employerUserLink.setUser(publicUser);
        employerUserLink.setProfileAccessLevel(ProfileAccessLevel.ADMIN);

        auditHandler.handlePreUpdateState(employerUserLink);
        auditHandler.handlePostUpdateState(employerUserLink);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }
}
