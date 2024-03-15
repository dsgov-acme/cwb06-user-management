package io.nuvalence.user.management.api.service.audit.profile;

import io.nuvalence.user.management.api.service.audit.AuditHandler;
import io.nuvalence.user.management.api.service.config.SpringConfig;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
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
public class EmployerProfileUserAccessLevelChangedAuditHandler
        implements AuditHandler<EmployerProfileLink> {

    private static final String ACCESS_LEVEL = "accessLevel";

    private final Map<String, String> accessLevelBefore = new HashMap<>();
    private final Map<String, String> accessLevelAfter = new HashMap<>();

    private String createdByUserId;
    private UUID userId;
    private UUID employerProfileId;

    private final AuditEventService employerAuditEventService;

    @Override
    public void handlePreUpdateState(EmployerProfileLink subject) {
        createdByUserId = subject.getProfile().getCreatedBy();
        userId = subject.getUser().getId();
        employerProfileId = subject.getProfile().getId();
        accessLevelBefore.put(ACCESS_LEVEL, subject.getProfileAccessLevel().getValue());
    }

    @Override
    public void handlePostUpdateState(EmployerProfileLink subject) {
        accessLevelAfter.put(ACCESS_LEVEL, subject.getProfileAccessLevel().getValue());
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        try {
            String eventSummary;
            if (!accessLevelBefore.get(ACCESS_LEVEL).equals(accessLevelAfter.get(ACCESS_LEVEL))) {
                eventSummary =
                        String.format(
                                "Profile user access level changed to [%s] for employer profile"
                                        + " user %s created by %s. Previously it was [%s]",
                                accessLevelAfter.get(ACCESS_LEVEL),
                                userId,
                                createdByUserId,
                                accessLevelBefore.get(ACCESS_LEVEL));
            } else {
                return;
            }
            Map<String, Object> eventData =
                    Map.of(
                            "createdById", createdByUserId,
                            "userId", userId);
            String eventDataJson = SpringConfig.getMapper().writeValueAsString(eventData);

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(originatorId)
                            .userId(originatorId)
                            .summary(eventSummary)
                            .businessObjectId(employerProfileId)
                            .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                            .data(
                                    accessLevelBefore,
                                    accessLevelAfter,
                                    eventDataJson,
                                    AuditActivityType.EMPLOYER_PROFILE_USER_ACCESS_LEVEL_CHANGED
                                            .getValue())
                            .build();

            employerAuditEventService.sendAuditEvent(auditEvent);

        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for "
                            + " employer profile user access level  changed for user "
                            + userId;
            log.error(errorMessage, e);
        }
    }
}
