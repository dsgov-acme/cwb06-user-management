package io.nuvalence.user.management.api.service.service;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.config.exception.ConflictException;
import io.nuvalence.user.management.api.service.config.exception.ProvidedDataException;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.events.model.AuditEventRequestObjectDto;
import io.nuvalence.user.management.api.service.models.ProfileInvitationFilters;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import io.nuvalence.user.management.api.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.user.management.api.service.models.auditevents.ProfileInvitationAuditEventDTO;
import io.nuvalence.user.management.api.service.repository.ProfileInvitationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing profile invitations.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProfileInvitationService {

    private final ProfileInvitationRepository repository;
    private final SendNotificationService sendNotificationService;
    private final AuditEventService auditEventService;

    /**
     * Creates a profile invitation and sends a notification to the invitee.

     * @param profileType Profile type
     * @param profileDisplayName Profile display name
     * @param profileInvitation Profile invitation
     * @return ProfileInvitation generated
     */
    public ProfileInvitation createProfileInvitation(
            ProfileType profileType,
            String profileDisplayName,
            ProfileInvitation profileInvitation) {
        if (profileInvitation.getEmail() == null || profileInvitation.getEmail().isBlank()) {
            throw new ProvidedDataException("Email is required");
        }
        if (profileInvitation.getAccessLevel() == null) {
            throw new ProvidedDataException("Access level is required");
        }

        Optional<ProfileInvitation> optionalProfileInvitation =
                getActiveInvitationForEmailAndId(
                        profileInvitation.getEmail(), profileInvitation.getProfileId());

        if (optionalProfileInvitation.isPresent()) {
            throw new ConflictException("Invitation already exists for this email");
        }

        profileInvitation.setType(profileType);
        profileInvitation.setClaimed(false);

        ProfileInvitation savedProfileInvitation = repository.save(profileInvitation);

        sendNotificationService.sendProfileInvitationEmailNotification(
                savedProfileInvitation, profileDisplayName);

        return savedProfileInvitation;
    }

    public ProfileInvitation markAsClaimedAndSave(ProfileInvitation profileInvitation) {
        profileInvitation.setClaimed(true);
        profileInvitation.setClaimedTimestamp(OffsetDateTime.now());
        return repository.save(profileInvitation);
    }

    public void verifyInvitationIsClaimable(ProfileInvitation profileInvitation) {
        if (Boolean.TRUE.equals(profileInvitation.getClaimed())) {
            throw new ConflictException("Invitation has already been claimed");
        }

        if (OffsetDateTime.now().isAfter(profileInvitation.getExpires())) {
            throw new ConflictException("Invitation has expired");
        }
    }

    public Page<ProfileInvitation> getProfileInvitationsByFilters(
            ProfileInvitationFilters filters) {
        return repository.findAll(
                filters.getProfileInvitationSpecification(), filters.getPageRequest());
    }

    public Optional<ProfileInvitation> getActiveInvitationForEmailAndId(
            String email, UUID profileId) {
        return repository.findFirstByEmailAndProfileIdAndExpiresAfter(
                email, profileId, OffsetDateTime.now());
    }

    public Optional<ProfileInvitation> getProfileInvitationById(UUID id) {
        return repository.findById(id);
    }

    public void deleteProfileInvitation(UUID profileInvitationId) {
        repository.deleteById(profileInvitationId);
    }

    /**
     * Resends an existing invitation if it has not been claimed.

     * @param existingInvitation existing invitation
     * @param profileDisplayName profile display name
     * @throws ConflictException if the invitation has already been claimed
     */
    public void resendExistingInvitation(
            ProfileInvitation existingInvitation, String profileDisplayName) {

        if (Boolean.TRUE.equals(existingInvitation.getClaimed())) {
            throw new ConflictException(
                    "This invitation has already been claimed and cannot be resent.");
        }

        existingInvitation.setExpires(OffsetDateTime.now().plusDays(7));

        sendNotificationService.sendProfileInvitationEmailNotification(
                existingInvitation, profileDisplayName);

        repository.save(existingInvitation);
    }

    public void postAuditEventForProfileInvite(
            ProfileInvitation profileInvitation, AuditActivityType auditActivityType) {

        try {

            ProfileInvitationAuditEventDTO profileInviteInfo =
                    new ProfileInvitationAuditEventDTO(
                            profileInvitation.getId().toString(),
                            profileInvitation.getAccessLevel(),
                            profileInvitation.getEmail());

            final String summary =
                    switch (auditActivityType) {
                        case PROFILE_INVITATION_SENT -> "Profile Invitation Sent";
                        case PROFILE_INVITATION_CLAIMED -> "Profile Invitation Claimed";
                        case PROFILE_INVITATION_DELETED -> "Profile Invitation Deleted";
                        case PROFILE_INVITATION_RESENT -> "Profile Invitation Resent";
                        default -> "Profile Invitation Event";
                    };

            final AuditEventBusinessObject businessObject =
                    switch (profileInvitation.getType()) {
                        case INDIVIDUAL -> AuditEventBusinessObject.INDIVIDUAL;
                        case EMPLOYER -> AuditEventBusinessObject.EMPLOYER;
                        default -> throw new IllegalStateException(
                                "Audit Event unsupported profile type");
                    };

            final AuditEventRequestObjectDto auditEvent =
                    AuditEventRequestObjectDto.builder()
                            .originatorId(SecurityContextUtility.getAuthenticatedUserId())
                            .userId(SecurityContextUtility.getAuthenticatedUserId())
                            .summary(summary)
                            .businessObjectId(profileInvitation.getProfileId())
                            .businessObjectType(businessObject)
                            .data(profileInviteInfo.toJson(), auditActivityType)
                            .build();

            auditEventService.sendAuditEvent(auditEvent);

        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            "Error recording audit event for profile"
                                    + " invitation with ID %s. Action"
                                    + " attempted was %s",
                            profileInvitation.getId(), auditActivityType);
            log.error(errorMessage, e);
        }
    }
}
