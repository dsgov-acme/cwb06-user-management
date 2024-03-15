package io.nuvalence.user.management.api.service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

class SendNotificationServiceTest {

    @Mock private PublisherProperties publisherProperties;

    @Mock private EventGateway eventGateway;

    @InjectMocks private SendNotificationService sendNotificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendDirectNotification_shouldPublishEvent() {

        ProfileInvitation profileInvitation = new ProfileInvitation();
        profileInvitation.setId(UUID.randomUUID());
        profileInvitation.setEmail("test@example.com");
        profileInvitation.setType(ProfileType.EMPLOYER);

        when(publisherProperties.getFullyQualifiedTopicName(any()))
                .thenReturn(Optional.of("notification-topic"));

        sendNotificationService.sendProfileInvitationEmailNotification(
                profileInvitation, "Test User");

        verify(eventGateway, times(1)).publishEvent(any(), eq("notification-topic"));
    }

    @Test
    void sendDirectNotification_shouldThrowNotFoundException_whenTopicNotFound() {

        ProfileInvitation profileInvitation = new ProfileInvitation();
        profileInvitation.setId(UUID.randomUUID());
        profileInvitation.setEmail("test@example.com");
        profileInvitation.setType(ProfileType.INDIVIDUAL);

        when(publisherProperties.getFullyQualifiedTopicName(any())).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () ->
                        sendNotificationService.sendProfileInvitationEmailNotification(
                                profileInvitation, "Test User"));
    }
}
