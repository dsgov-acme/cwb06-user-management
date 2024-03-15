package io.nuvalence.user.management.api.service.audit.profile;

import io.nuvalence.user.management.api.service.audit.AuditHandler;
import io.nuvalence.user.management.api.service.config.SpringConfig;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.events.model.AuditEventRequestObjectDto;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import io.nuvalence.user.management.api.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.user.management.api.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IndividualProfileUserAccessLevelChangedAuditHandler
        implements AuditHandler<IndividualProfileLink> {

    private static final String ACCESS_LEVEL = "accessLevel";

    private final Map<String, String> accessLevelBefore = new HashMap<>();
    private final Map<String, String> accessLevelAfter = new HashMap<>();

    private String createdById;
    private String userId;

    private final AuditEventService individualAuditEventService;

    @Override
    public void handlePreUpdateState(IndividualProfileLink subject) {
        createdById = subject.getProfile().getCreatedBy();
        userId = subject.getUser().getId().toString();
        accessLevelBefore.put(ACCESS_LEVEL, subject.getProfileAccessLevel().getValue());
    }

    @Override
    public void handlePostUpdateState(IndividualProfileLink subject) {
        accessLevelAfter.put(ACCESS_LEVEL, subject.getProfileAccessLevel().getValue());
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary;

            if (!accessLevelBefore.get(ACCESS_LEVEL).equals(accessLevelAfter.get(ACCESS_LEVEL))) {
                eventSummary =
                        String.format(
                                "Profile user access level changed to [%s] for individual profile"
                                        + " user %s created by %s. Previously it was [%s]",
                                accessLevelAfter.get(ACCESS_LEVEL),
                                userId,
                                createdById,
                                accessLevelBefore.get(ACCESS_LEVEL));
            } else {
                return;
            }

            Map<String, Object> individualEventData =
                    Map.of(
                            "createdById", createdById,
                            "userId", userId);

            String individualEventDataJson =
                    SpringConfig.getMapper().writeValueAsString(individualEventData);

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(eventSummary)
                            .businessObjectId(UUID.fromString(userId))
                            .businessObjectType(AuditEventBusinessObject.INDIVIDUAL)
                            .data(
                                    accessLevelBefore,
                                    accessLevelAfter,
                                    individualEventDataJson,
                                    AuditActivityType.INDIVIDUAL_PROFILE_USER_ACCESS_LEVEL_CHANGED
                                            .getValue())
                            .build();

            individualAuditEventService.sendAuditEvent(auditEvent);
        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for "
                            + " individual profile user access level  changed for user "
                            + userId;
            log.error(errorMessage, e);
        }
    }
}
