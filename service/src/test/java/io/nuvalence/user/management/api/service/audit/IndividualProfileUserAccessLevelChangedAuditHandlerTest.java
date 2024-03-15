package io.nuvalence.user.management.api.service.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.user.management.api.service.audit.profile.IndividualProfileUserAccessLevelChangedAuditHandler;
import io.nuvalence.user.management.api.service.config.SpringConfig;
import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.events.model.AuditEventRequestObjectDto;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
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
class IndividualProfileUserAccessLevelChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private IndividualProfileUserAccessLevelChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        UUID createdById = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileAccessLevel accessLevelOld = ProfileAccessLevel.READER;
        IndividualProfile individual =
                IndividualProfile.builder()
                        .id(UUID.randomUUID())
                        .createdBy(createdById.toString())
                        .build();

        IndividualProfileLink individualUserLink =
                IndividualProfileLink.builder().profile(individual).build();
        individualUserLink.setId(UUID.randomUUID());
        PublicUser publicUser = new PublicUser();
        publicUser.setId(userId);
        individualUserLink.setUser(publicUser);
        individualUserLink.setProfileAccessLevel(accessLevelOld);

        auditHandler.handlePreUpdateState(individualUserLink);

        ProfileAccessLevel profileAccessLevelNew = ProfileAccessLevel.WRITER;
        individualUserLink.setProfileAccessLevel(profileAccessLevelNew);
        auditHandler.handlePostUpdateState(individualUserLink);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("accessLevel", accessLevelOld.toString());

        Map<String, String> after = new HashMap<>();
        after.put("accessLevel", profileAccessLevelNew.toString());

        Map<String, Object> eventData =
                Map.of(
                        "createdById", createdById,
                        "userId", userId);
        String eventDataJson = SpringConfig.getMapper().writeValueAsString(eventData);

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());

        AuditEventRequestObjectDto capturedEvent = auditEventCaptor.getValue();
        Assertions.assertNotNull(capturedEvent);
        Assertions.assertEquals(originatorId, capturedEvent.getOriginatorId());
        Assertions.assertEquals(userId, capturedEvent.getBusinessObjectId());
        Assertions.assertEquals(
                AuditEventBusinessObject.INDIVIDUAL, capturedEvent.getBusinessObjectType());
        Assertions.assertEquals(
                String.format(
                        "Profile user access level changed to [%s] for individual profile"
                                + " user %s created by %s. Previously it was [%s]",
                        profileAccessLevelNew, userId, createdById, accessLevelOld),
                capturedEvent.getSummary());
        StateChangeEventData eventDataResult = (StateChangeEventData) capturedEvent.getData();
        Assertions.assertEquals(
                before.toString(),
                eventDataResult.getOldState().replace("\"", "").replace(":", "="));
        Assertions.assertEquals(
                after.toString(),
                eventDataResult.getNewState().replace("\"", "").replace(":", "="));
        Assertions.assertEquals(
                AuditActivityType.INDIVIDUAL_PROFILE_USER_ACCESS_LEVEL_CHANGED.getValue(),
                eventDataResult.getActivityType());
    }

    @Test
    void test_publishAuditEvent_WithNoChanges() {
        IndividualProfileLink individualUserLink =
                IndividualProfileLink.builder()
                        .profile(IndividualProfile.builder().id(UUID.randomUUID()).build())
                        .build();
        individualUserLink.setId(UUID.randomUUID());
        PublicUser publicUser = new PublicUser();
        publicUser.setId(UUID.randomUUID());
        individualUserLink.setUser(publicUser);
        individualUserLink.setProfileAccessLevel(ProfileAccessLevel.READER);

        auditHandler.handlePreUpdateState(individualUserLink);
        auditHandler.handlePostUpdateState(individualUserLink);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }
}
