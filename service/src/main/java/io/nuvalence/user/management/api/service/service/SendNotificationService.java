package io.nuvalence.user.management.api.service.service;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.DirectNotificationEvent;
import io.nuvalence.events.event.Event;
import io.nuvalence.events.event.dto.CommunicationMethod;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.events.EventFactory;
import io.nuvalence.user.management.api.service.events.PublisherTopic;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages the communication with notification service.
 */
@RequiredArgsConstructor
@Service
public class SendNotificationService {

    private final PublisherProperties publisherProperties;
    private final EventGateway eventGateway;

    private static final String PORTAL_URL_KEY = "portal-url";

    private static final String PROFILE_INVITATION_TEMPLATE_KEY = "ProfileInvitationTemplate";

    @Value("${invitation.individual.claim.url}")
    private String individualProfileClaimUrl;

    @Value("${invitation.employer.claim.url}")
    private String employerProfileClaimUrl;

    /**
     * Send a profile invitation direct email notification.

     * @param profileInvitation Profile invitation.
     * @param profileDisplayName Profile display name.
     */
    public void sendProfileInvitationEmailNotification(
            ProfileInvitation profileInvitation, String profileDisplayName) {

        String claimUrl =
                switch (profileInvitation.getType()) {
                    case INDIVIDUAL -> individualProfileClaimUrl;
                    case EMPLOYER -> employerProfileClaimUrl;
                };

        Map<String, String> properties =
                new HashMap<>(
                        Map.ofEntries(
                                Map.entry(
                                        PORTAL_URL_KEY, claimUrl + "/" + profileInvitation.getId()),
                                Map.entry("profile-display-name", profileDisplayName),
                                Map.entry("invitation-id", profileInvitation.getId().toString())));

        DirectNotificationEvent notificationEvent =
                EventFactory.createDirectNotificationEvent(
                        CommunicationMethod.EMAIL,
                        profileInvitation.getEmail(),
                        PROFILE_INVITATION_TEMPLATE_KEY,
                        properties);

        sendNotification(notificationEvent);
    }

    private void sendNotification(Event notificationEvent) {
        Optional<String> fullyQualifiedTopicNameOptional =
                publisherProperties.getFullyQualifiedTopicName(
                        PublisherTopic.NOTIFICATION_REQUESTS.name());

        if (fullyQualifiedTopicNameOptional.isEmpty()) {
            throw new NotFoundException(
                    "Notification requests topic not found, topic name: "
                            + PublisherTopic.NOTIFICATION_REQUESTS.name());
        }

        eventGateway.publishEvent(notificationEvent, fullyQualifiedTopicNameOptional.get());
    }
}
