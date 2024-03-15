package io.nuvalence.user.management.api.service.service;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.entity.profile.Address;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.events.model.AuditEventRequestObjectDto;
import io.nuvalence.user.management.api.service.models.EmployerFilters;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import io.nuvalence.user.management.api.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.user.management.api.service.models.auditevents.ProfileCreatedAuditEventDto;
import io.nuvalence.user.management.api.service.models.auditevents.ProfileUserAddedAuditEventDto;
import io.nuvalence.user.management.api.service.models.auditevents.ProfileUserRemovedAuditEventDto;
import io.nuvalence.user.management.api.service.repository.EmployerProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
/**
 * Service for managing employer profiles.
 */
public class EmployerProfileService {
    private final EmployerProfileRepository repository;
    private final AuditEventService auditEventService;

    public Page<EmployerProfile> getEmployersByFilters(final EmployerFilters filters) {
        return repository.findAll(
                filters.getEmployerProfileSpecification(), filters.getPageRequest());
    }

    /**
     * Gets an employer profile by ID.
     *
     * @param id the ID of the employer profile to get
     * @return the employer profile
     */
    public Optional<EmployerProfile> getEmployerById(final UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        return repository.findById(id);
    }

    /**
     * Saves a single employer profile.
     *
     * @param employer the employer profile to save
     * @return the saved employer profile
     */
    public EmployerProfile saveEmployer(final EmployerProfile employer) {
        if (employer.getMailingAddress() != null) {
            employer.getMailingAddress().setEmployerForMailing(employer);
        }

        if (employer.getLocations() != null) {
            for (Address location : employer.getLocations()) {
                location.setEmployerForLocations(employer);
            }
        }

        return repository.save(employer);
    }

    /**
     * Publishes an audit event for an employer profile being created.
     *
     * @param profile the employer profile that was created
     * @return the UUID of the audit event that was published
     */
    public UUID postAuditEventForEmployerCreated(EmployerProfile profile) {

        ProfileCreatedAuditEventDto profileInfo =
                new ProfileCreatedAuditEventDto(profile.getCreatedBy());

        final String summary = "Profile Created.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(profile.getCreatedBy())
                        .userId(profile.getCreatedBy())
                        .summary(summary)
                        .businessObjectId(profile.getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(profileInfo.toJson(), AuditActivityType.EMPLOYER_PROFILE_CREATED)
                        .build();

        return auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForEmployerProfileUserAdded(EmployerProfileLink employerProfileLink) {

        ProfileUserAddedAuditEventDto profileUserInfo =
                new ProfileUserAddedAuditEventDto(
                        employerProfileLink.getUser().getId().toString(),
                        employerProfileLink.getUser().getId().toString(),
                        employerProfileLink.getProfileAccessLevel().getValue());

        final String summary = "Employer Profile User Added.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(SecurityContextUtility.getAuthenticatedUserId())
                        .userId(SecurityContextUtility.getAuthenticatedUserId())
                        .summary(summary)
                        .businessObjectId(employerProfileLink.getProfile().getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileUserInfo.toJson(),
                                AuditActivityType.EMPLOYER_PROFILE_USER_ADDED)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }

    public void postAuditEventForEmployerProfileUserRemoved(EmployerProfileLink employerUserLink) {

        ProfileUserRemovedAuditEventDto profileUserInfo =
                new ProfileUserRemovedAuditEventDto(
                        employerUserLink.getUser().getId().toString(),
                        employerUserLink.getUser().getId().toString());

        final String summary = "Employer Profile User Removed.";

        final AuditEventRequestObjectDto auditEvent =
                AuditEventRequestObjectDto.builder()
                        .originatorId(SecurityContextUtility.getAuthenticatedUserId())
                        .userId(SecurityContextUtility.getAuthenticatedUserId())
                        .summary(summary)
                        .businessObjectId(employerUserLink.getProfile().getId())
                        .businessObjectType(AuditEventBusinessObject.EMPLOYER)
                        .data(
                                profileUserInfo.toJson(),
                                AuditActivityType.EMPLOYER_PROFILE_USER_REMOVED)
                        .build();

        auditEventService.sendAuditEvent(auditEvent);
    }
}
