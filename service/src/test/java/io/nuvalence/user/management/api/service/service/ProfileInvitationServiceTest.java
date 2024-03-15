package io.nuvalence.user.management.api.service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.config.exception.ConflictException;
import io.nuvalence.user.management.api.service.config.exception.ProvidedDataException;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.events.model.AuditEventRequestObjectDto;
import io.nuvalence.user.management.api.service.models.ProfileInvitationFilters;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import io.nuvalence.user.management.api.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.user.management.api.service.models.auditevents.ProfileInvitationAuditEventDTO;
import io.nuvalence.user.management.api.service.repository.ProfileInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProfileInvitationServiceTest {

    private static final String DISPLAY_NAME = "displayName";
    private static final String TEST_EMAIL = "test@example.com";

    @Mock private ProfileInvitationRepository repository;
    @Mock private SendNotificationService sendNotificationService;
    @Mock private AuditEventService auditEventService;

    private ProfileInvitationService service;

    @BeforeEach
    void setUp() {
        service =
                new ProfileInvitationService(
                        repository, sendNotificationService, auditEventService);
    }

    @Test
    void getProfileInvitationsByFilters() {
        Page<ProfileInvitation> invitations = new PageImpl<>(List.of(new ProfileInvitation()));
        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(invitations);

        final ProfileInvitationFilters profileInvitationFilters =
                new ProfileInvitationFilters(
                        "createdTimestamp", "ASC", 0, 2, null, null, null, null, null);

        Page<ProfileInvitation> result =
                service.getProfileInvitationsByFilters(profileInvitationFilters);

        assertEquals(invitations, result);
        verify(repository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getActiveInvitationForEmail_Found() {
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setProfileId(UUID.randomUUID());
        invitation.setEmail(TEST_EMAIL);
        OffsetDateTime expires = OffsetDateTime.now().plusDays(1);
        invitation.setExpires(expires);

        when(repository.findFirstByEmailAndProfileIdAndExpiresAfter(anyString(), any(), any()))
                .thenReturn(Optional.of(invitation));

        Optional<ProfileInvitation> result =
                service.getActiveInvitationForEmailAndId(TEST_EMAIL, invitation.getProfileId());

        assertTrue(result.isPresent());
        assertEquals(invitation, result.get());
    }

    @Test
    void getActiveInvitationForEmail_NotFound() {
        UUID profileId = UUID.randomUUID();
        when(repository.findFirstByEmailAndProfileIdAndExpiresAfter(anyString(), any(), any()))
                .thenReturn(Optional.empty());

        Optional<ProfileInvitation> result =
                service.getActiveInvitationForEmailAndId(TEST_EMAIL, profileId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getProfileInvitationById_Found() {
        ProfileInvitation invitation = new ProfileInvitation();
        when(repository.findById(any())).thenReturn(Optional.of(invitation));

        Optional<ProfileInvitation> result = service.getProfileInvitationById(UUID.randomUUID());

        assertTrue(result.isPresent());
        assertEquals(invitation, result.get());
    }

    @Test
    void getProfileInvitationById_NotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        Optional<ProfileInvitation> result = service.getProfileInvitationById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteProfileInvitation() {
        service.deleteProfileInvitation(UUID.randomUUID());

        verify(repository).deleteById(any());
    }

    @Test
    void createProfileInvitation_Success() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setEmail(TEST_EMAIL);
        invitation.setAccessLevel(ProfileAccessLevel.ADMIN);

        when(repository.save(any())).thenReturn(invitation);

        ProfileInvitation result =
                service.createProfileInvitation(ProfileType.EMPLOYER, DISPLAY_NAME, invitation);

        assertEquals(invitation, result);
        verify(repository).save(invitation);
        verify(sendNotificationService)
                .sendProfileInvitationEmailNotification(invitation, DISPLAY_NAME);
    }

    @Test
    void createProfileInvitation_NoEmail() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setAccessLevel(ProfileAccessLevel.ADMIN);

        assertThrows(
                ProvidedDataException.class,
                () -> {
                    service.createProfileInvitation(ProfileType.EMPLOYER, DISPLAY_NAME, invitation);
                });

        verify(repository, never()).save(any());
        verify(sendNotificationService, never())
                .sendProfileInvitationEmailNotification(any(), anyString());
    }

    @Test
    void createProfileInvitation_NoAccessLevel() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setEmail(TEST_EMAIL);

        assertThrows(
                ProvidedDataException.class,
                () -> {
                    service.createProfileInvitation(ProfileType.EMPLOYER, DISPLAY_NAME, invitation);
                });

        verify(repository, never()).save(any());
        verify(sendNotificationService, never())
                .sendProfileInvitationEmailNotification(any(), anyString());
    }

    @Test
    void createProfileInvitation_InvitationExists() {

        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setEmail(TEST_EMAIL);
        invitation.setAccessLevel(ProfileAccessLevel.ADMIN);

        when(repository.findFirstByEmailAndProfileIdAndExpiresAfter(anyString(), any(), any()))
                .thenReturn(Optional.of(invitation));

        assertThrows(
                ConflictException.class,
                () -> {
                    service.createProfileInvitation(ProfileType.EMPLOYER, DISPLAY_NAME, invitation);
                });

        verify(repository, never()).save(any());
        verify(sendNotificationService, never())
                .sendProfileInvitationEmailNotification(any(), anyString());
    }

    @Test
    void postAuditEventForProfileInvitationSent() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();
        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId);
            service.postAuditEventForProfileInvite(
                    profileInvitation, AuditActivityType.PROFILE_INVITATION_SENT);
        }

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Sent")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInviteInfo.toJson(), AuditActivityType.PROFILE_INVITATION_SENT)
                        .build();

        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForProfileInvitationClaimed() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId);
            service.postAuditEventForProfileInvite(
                    profileInvitation, AuditActivityType.PROFILE_INVITATION_CLAIMED);
        }

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Claimed")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileInviteInfo.toJson(),
                                AuditActivityType.PROFILE_INVITATION_CLAIMED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForProfileInvitationDeleted() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId);
            service.postAuditEventForProfileInvite(
                    profileInvitation, AuditActivityType.PROFILE_INVITATION_DELETED);
        }

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Deleted")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileInviteInfo.toJson(),
                                AuditActivityType.PROFILE_INVITATION_DELETED)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void postAuditEventForProfileInvitationResent() {
        ProfileInvitation profileInvitation = createProfileInvitation();
        String userId = UUID.randomUUID().toString();

        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId);
            service.postAuditEventForProfileInvite(
                    profileInvitation, AuditActivityType.PROFILE_INVITATION_RESENT);
        }

        ProfileInvitationAuditEventDTO profileInviteInfo =
                new ProfileInvitationAuditEventDTO(
                        profileInvitation.getId().toString(),
                        profileInvitation.getAccessLevel(),
                        profileInvitation.getEmail());

        ArgumentCaptor<AuditEventRequestObjectDto> auditEventCaptor =
                ArgumentCaptor.forClass(AuditEventRequestObjectDto.class);
        verify(auditEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditEventRequestObjectDto capturedAuditEvent = auditEventCaptor.getValue();

        AuditEventRequestObjectDto testEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(userId)
                        .userId(userId)
                        .summary("Profile Invitation Resent")
                        .businessObjectId(profileInvitation.getProfileId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileInviteInfo.toJson(),
                                AuditActivityType.PROFILE_INVITATION_RESENT)
                        .build();

        assertEquals(testEvent.getOriginatorId(), capturedAuditEvent.getOriginatorId());
        assertEquals(testEvent.getUserId(), capturedAuditEvent.getUserId());
        assertEquals(testEvent.getSummary(), capturedAuditEvent.getSummary());
        assertEquals(testEvent.getBusinessObjectId(), capturedAuditEvent.getBusinessObjectId());
        assertEquals(testEvent.getBusinessObjectType(), capturedAuditEvent.getBusinessObjectType());
        assertEquals(testEvent.getData().toString(), capturedAuditEvent.getData().toString());
    }

    @Test
    void resendExistingInvitation_InvitationNotClaimedAndExpired_Success() {

        ProfileInvitation existingInvitation = new ProfileInvitation();
        existingInvitation.setClaimed(false);
        existingInvitation.setExpires(OffsetDateTime.now().minusDays(15));
        String profileDisplayName = "Test Profile";

        service.resendExistingInvitation(existingInvitation, profileDisplayName);

        assertTrue(existingInvitation.getExpires().isAfter(OffsetDateTime.now().plusDays(6)));
        assertTrue(existingInvitation.getExpires().isBefore(OffsetDateTime.now().plusDays(8)));
        verify(sendNotificationService)
                .sendProfileInvitationEmailNotification(existingInvitation, profileDisplayName);
        verify(repository).save(existingInvitation);
    }

    @Test
    void resendExistingInvitation_InvitationNotClaimedAndNotExpired_Success() {

        ProfileInvitation existingInvitation = new ProfileInvitation();
        existingInvitation.setClaimed(false);
        existingInvitation.setExpires(OffsetDateTime.now().plusDays(2));
        String profileDisplayName = "Test Profile";

        service.resendExistingInvitation(existingInvitation, profileDisplayName);

        assertTrue(existingInvitation.getExpires().isAfter(OffsetDateTime.now().plusDays(6)));
        assertTrue(existingInvitation.getExpires().isBefore(OffsetDateTime.now().plusDays(8)));
        verify(sendNotificationService)
                .sendProfileInvitationEmailNotification(existingInvitation, profileDisplayName);
        verify(repository).save(existingInvitation);
    }

    @Test
    void resendExistingInvitation_InvitationClaimed_ConflictExceptionThrown() {

        ProfileInvitation existingInvitation = new ProfileInvitation();
        existingInvitation.setClaimed(true);
        String profileDisplayName = "Test Profile";

        var e =
                assertThrows(
                        ConflictException.class,
                        () -> {
                            service.resendExistingInvitation(
                                    existingInvitation, profileDisplayName);
                        });

        assertEquals(
                "This invitation has already been claimed and cannot be resent.", e.getMessage());

        verify(sendNotificationService, never())
                .sendProfileInvitationEmailNotification(any(), anyString());
        verify(repository, never()).save(any());
    }

    private ProfileInvitation createProfileInvitation() {
        return ProfileInvitation.builder()
                .id(UUID.randomUUID())
                .profileId(UUID.randomUUID())
                .type(ProfileType.EMPLOYER)
                .accessLevel(ProfileAccessLevel.ADMIN)
                .email("test@example.com")
                .build();
    }

    @Test
    void markAsClaimedAndSave() {
        ProfileInvitation invitation = new ProfileInvitation();

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNull(invitation.getClaimed());
        assertNull(invitation.getClaimedTimestamp());

        OffsetDateTime beforeClaim = OffsetDateTime.now().minusNanos(1L);

        ProfileInvitation result = service.markAsClaimedAndSave(invitation);

        assertTrue(result.getClaimed());
        assertTrue(result.getClaimedTimestamp().isAfter(beforeClaim));

        verify(repository).save(invitation);
    }

    @Test
    void verifyInvitationIsClaimable_InvitationNotClaimedAndNotExpired() {
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setClaimed(false);
        invitation.setExpires(OffsetDateTime.now().plusDays(1));

        assertDoesNotThrow(
                () -> {
                    service.verifyInvitationIsClaimable(invitation);
                });
    }

    @Test
    void verifyInvitationIsClaimable_InvitationClaimed() {
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setClaimed(true);
        invitation.setExpires(OffsetDateTime.now().plusDays(1));

        var e =
                assertThrows(
                        ConflictException.class,
                        () -> {
                            service.verifyInvitationIsClaimable(invitation);
                        });

        assertEquals("Invitation has already been claimed", e.getMessage());
    }

    @Test
    void verifyInvitationIsClaimable_InvitationExpired() {
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setClaimed(false);
        invitation.setExpires(OffsetDateTime.now().minusDays(1));

        var e =
                assertThrows(
                        ConflictException.class,
                        () -> {
                            service.verifyInvitationIsClaimable(invitation);
                        });
        assertEquals("Invitation has expired", e.getMessage());
    }
}
