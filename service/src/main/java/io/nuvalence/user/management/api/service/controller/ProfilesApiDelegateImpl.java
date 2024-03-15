package io.nuvalence.user.management.api.service.controller;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.audit.AuditableAction;
import io.nuvalence.user.management.api.service.audit.profile.EmployerProfileDataChangedAuditHandler;
import io.nuvalence.user.management.api.service.audit.profile.EmployerProfileUserAccessLevelChangedAuditHandler;
import io.nuvalence.user.management.api.service.audit.profile.IndividualProfileDataChangedAuditHandler;
import io.nuvalence.user.management.api.service.audit.profile.IndividualProfileUserAccessLevelChangedAuditHandler;
import io.nuvalence.user.management.api.service.config.exception.ConflictException;
import io.nuvalence.user.management.api.service.config.exception.ProvidedDataException;
import io.nuvalence.user.management.api.service.config.exception.UnexpectedException;
import io.nuvalence.user.management.api.service.entity.UserEntity;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.Profile;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.enums.UserType;
import io.nuvalence.user.management.api.service.generated.controllers.ProfilesApiDelegate;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileCreateModel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileLinkRequestModel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileLinkResponse;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileResponseModel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileUpdateModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileCreateModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileLinkResponseModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileLinkUpdateModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileResponseModel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileUpdateModel;
import io.nuvalence.user.management.api.service.generated.models.PageEmployerProfileLink;
import io.nuvalence.user.management.api.service.generated.models.PageEmployerProfileResponseModel;
import io.nuvalence.user.management.api.service.generated.models.PageIndividualLinksResponseModel;
import io.nuvalence.user.management.api.service.generated.models.PageIndividualProfileResponseModel;
import io.nuvalence.user.management.api.service.generated.models.PageProfileInvitationResponse;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationResponse;
import io.nuvalence.user.management.api.service.mapper.EmployerProfileLinkMapper;
import io.nuvalence.user.management.api.service.mapper.EmployerProfileMapper;
import io.nuvalence.user.management.api.service.mapper.IndividualProfileLinkMapper;
import io.nuvalence.user.management.api.service.mapper.IndividualProfileMapper;
import io.nuvalence.user.management.api.service.mapper.PagingMetadataMapper;
import io.nuvalence.user.management.api.service.mapper.ProfileInvitationMapper;
import io.nuvalence.user.management.api.service.models.EmployerFilters;
import io.nuvalence.user.management.api.service.models.EmployerProfileLinkFilters;
import io.nuvalence.user.management.api.service.models.IndividualFilters;
import io.nuvalence.user.management.api.service.models.IndividualProfileLinksFilters;
import io.nuvalence.user.management.api.service.models.ProfileInvitationFilters;
import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import io.nuvalence.user.management.api.service.service.AuditEventService;
import io.nuvalence.user.management.api.service.service.CommonProfileService;
import io.nuvalence.user.management.api.service.service.EmployerProfileLinkService;
import io.nuvalence.user.management.api.service.service.EmployerProfileService;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileService;
import io.nuvalence.user.management.api.service.service.ProfileInvitationService;
import io.nuvalence.user.management.api.service.service.UserService;
import io.nuvalence.user.management.api.service.util.RequestContextTimestamp;
import io.nuvalence.user.management.api.service.util.UserUtility;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilesApiDelegateImpl implements ProfilesApiDelegate {
    private final AuthorizationHandler authorizationHandler;
    private final IndividualProfileMapper individualMapper;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final RequestContextTimestamp requestContextTimestamp;
    private final AuditEventService individualAuditEventService;
    private final EmployerProfileMapper employerMapper;
    private final EmployerProfileLinkMapper employerProfileLinkMapper;
    private final IndividualProfileLinkMapper individualUserLinkMapper;

    private final AuditEventService auditEventService;
    private final UserService userService;
    private final EmployerProfileLinkService employerProfileLinkService;
    private final AuditEventService employerAuditEventService;
    private final IndividualProfileLinkService individualUserLinkService;
    private final CommonProfileService commonProfileService;
    private final EmployerProfileService employerService;
    private final IndividualProfileService individualService;
    private final ProfileInvitationService invitationService;
    private final ProfileInvitationMapper invitationMapper;

    private static final String CREATION_AUDIT_EVENT_ERR_MSG =
            "An error has occurred when recording a creation audit event for an";
    private static final String PROFILE_INVITATION_WRONG_TYPE =
            "Profile invitation is not for the required profile type (%s).";
    private static final String INDIVIDUAL_PROFILE_NOT_FOUND_MSG = "Individual profile not found";
    private static final String EMPLOYER_PROFILE_NOT_FOUND_MSG = "Employer profile not found";
    private static final String PROFILE_INVITATION_NOT_FOUND = "Profile invitation not found";

    private static final String VIEW_ACTION = "view";

    private static final String CREATE_ACTION = "create";

    private static final String UPDATE_ACTION = "update";

    private static final String INVITE_ACTION = "invite";

    private static final String LIST_ACTION = "list";

    private static final String LINK_ACTION = "link";

    private static final String DELETE_LINK_ACTION = "delete-link";

    @Override
    public ResponseEntity<EmployerProfileResponseModel> postEmployerProfile(
            EmployerProfileCreateModel employerProfileCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_ACTION, EmployerProfile.class)) {
            throw new ForbiddenException();
        }

        EmployerProfile employer =
                employerService.saveEmployer(
                        employerMapper.createModelToEmployer(employerProfileCreateModel));

        EmployerProfileResponseModel employerProfileResponseModel =
                employerMapper.employerToResponseModel(employer);

        postAuditEventForEmployerProfileCreated(employer);

        return ResponseEntity.status(200).body(employerProfileResponseModel);
    }

    @Override
    public ResponseEntity<PageEmployerProfileResponseModel> getEmployerProfiles(
            String fein,
            String name,
            String type,
            String industry,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed(LIST_ACTION, EmployerProfile.class)) {
            throw new ForbiddenException();
        }

        Page<EmployerProfileResponseModel> results =
                employerService
                        .getEmployersByFilters(
                                new EmployerFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        fein,
                                        name,
                                        type,
                                        industry))
                        .map(employerMapper::employerToResponseModel);

        PageEmployerProfileResponseModel response = new PageEmployerProfileResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<EmployerProfileResponseModel> getEmployerProfile(UUID profileId) {
        final EmployerProfileResponseModel employerProfileResponseModel =
                employerService
                        .getEmployerById(profileId)
                        .filter(
                                employerInstance ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_ACTION, employerInstance))
                        .map(employerMapper::employerToResponseModel)
                        .orElseThrow(() -> new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG));

        return ResponseEntity.status(200).body(employerProfileResponseModel);
    }

    @Override
    public ResponseEntity<EmployerProfileResponseModel> updateEmployerProfile(
            UUID profileId, EmployerProfileUpdateModel employerProfileUpdateModel) {
        Optional<EmployerProfile> optionalEmployer = employerService.getEmployerById(profileId);
        if (optionalEmployer.isEmpty()
                || !authorizationHandler.isAllowedForInstance(
                        VIEW_ACTION, optionalEmployer.get())) {
            throw new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG);
        }
        EmployerProfile existingEmployer = optionalEmployer.get();

        if (!authorizationHandler.isAllowedForInstance(UPDATE_ACTION, existingEmployer)) {
            throw new ForbiddenException();
        }

        try {
            final EmployerProfile savedEmployer =
                    AuditableAction.builder(EmployerProfile.class)
                            .auditHandler(
                                    new EmployerProfileDataChangedAuditHandler(auditEventService))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    employer -> {
                                        EmployerProfile employerToBeSaved =
                                                employerMapper.updateModelToEmployer(
                                                        employerProfileUpdateModel);
                                        employerToBeSaved.setId(profileId);
                                        employerToBeSaved.setCreatedBy(
                                                existingEmployer.getCreatedBy());
                                        employerToBeSaved.setCreatedTimestamp(
                                                existingEmployer.getCreatedTimestamp());

                                        employerService.saveEmployer(employerToBeSaved);

                                        return employerService
                                                .getEmployerById(profileId)
                                                .orElseThrow(
                                                        () ->
                                                                new UnexpectedException(
                                                                        EMPLOYER_PROFILE_NOT_FOUND_MSG
                                                                                + " after saving"));
                                    })
                            .build()
                            .execute(existingEmployer);

            return ResponseEntity.status(200)
                    .body(employerMapper.employerToResponseModel(savedEmployer));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public ResponseEntity<IndividualProfileResponseModel> postIndividualProfile(
            IndividualProfileCreateModel individualProfileCreateModel) {
        if (!authorizationHandler.isAllowed(CREATE_ACTION, IndividualProfile.class)) {
            throw new ForbiddenException();
        }

        IndividualProfile individual =
                individualService.saveIndividual(
                        individualMapper.createModelToIndividual(individualProfileCreateModel));

        individualService.postAuditEventForIndividualProfileCreated(individual);

        IndividualProfileResponseModel individualProfileResponseModel =
                individualMapper.individualToResponseModel(individual);

        return ResponseEntity.status(HttpStatus.OK).body(individualProfileResponseModel);
    }

    @Override
    public ResponseEntity<IndividualProfileResponseModel> getIndividualProfile(UUID profileId) {
        final IndividualProfileResponseModel individualProfileResponseModel =
                individualService
                        .getIndividualById(profileId)
                        .filter(
                                individualInstance ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_ACTION, individualInstance))
                        .map(individualMapper::individualToResponseModel)
                        .orElseThrow(() -> new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG));

        return ResponseEntity.status(200).body(individualProfileResponseModel);
    }

    @Override
    public ResponseEntity<IndividualProfileResponseModel> updateIndividualProfile(
            UUID profileId, IndividualProfileUpdateModel individualProfileUpdateModel) {

        Optional<IndividualProfile> optionalIndividual =
                individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()
                || !authorizationHandler.isAllowedForInstance(
                        VIEW_ACTION, optionalIndividual.get())) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }
        IndividualProfile existingIndividual = optionalIndividual.get();
        if (!authorizationHandler.isAllowedForInstance(UPDATE_ACTION, existingIndividual)) {
            throw new ForbiddenException();
        }

        try {
            final IndividualProfile savedIndividual =
                    AuditableAction.builder(IndividualProfile.class)
                            .auditHandler(
                                    new IndividualProfileDataChangedAuditHandler(
                                            individualAuditEventService))
                            .requestContextTimestamp(requestContextTimestamp)
                            .action(
                                    individual -> {
                                        IndividualProfile individualToBeSaved =
                                                individualMapper.updateModelToIndividual(
                                                        individualProfileUpdateModel);
                                        individualToBeSaved.setId(profileId);
                                        individualToBeSaved.setCreatedBy(
                                                existingIndividual.getCreatedBy());
                                        individualToBeSaved.setCreatedTimestamp(
                                                existingIndividual.getCreatedTimestamp());

                                        individualService.saveIndividual(individualToBeSaved);

                                        return individualService
                                                .getIndividualById(profileId)
                                                .orElseThrow(
                                                        () ->
                                                                new UnexpectedException(
                                                                        INDIVIDUAL_PROFILE_NOT_FOUND_MSG
                                                                                + " after saving"));
                                    })
                            .build()
                            .execute(existingIndividual);

            return ResponseEntity.status(200)
                    .body(individualMapper.individualToResponseModel(savedIndividual));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public ResponseEntity<PageIndividualProfileResponseModel> getIndividualProfiles(
            String ssn,
            String name,
            String email,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed(LIST_ACTION, IndividualProfile.class)) {
            throw new ForbiddenException();
        }

        Page<IndividualProfileResponseModel> results =
                individualService
                        .getIndividualsByFilters(
                                new IndividualFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        ssn,
                                        email,
                                        name,
                                        userService))
                        .map(individualMapper::individualToResponseModel);

        PageIndividualProfileResponseModel response = new PageIndividualProfileResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));
        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<EmployerProfileLinkResponse> updateEmployerProfileLink(
            UUID profileId,
            UUID userId,
            EmployerProfileLinkRequestModel employerProfileLinkRequest) {

        if (!isAllowedOnProfile(LINK_ACTION, profileId)) {
            throw new ForbiddenException();
        }

        Optional<EmployerProfile> optionalEmployer = employerService.getEmployerById(profileId);
        if (optionalEmployer.isEmpty()) {
            throw new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG);
        }

        EmployerProfileLink savedEmployerUserLink =
                saveEmployerUserLink(
                        profileId,
                        userId,
                        optionalEmployer.get(),
                        ProfileAccessLevel.fromValue(
                                employerProfileLinkRequest.getProfileAccessLevel()));
        return ResponseEntity.status(200)
                .body(
                        employerProfileLinkMapper.employerProfileLinkToResponseModel(
                                savedEmployerUserLink));
    }

    @Override
    public ResponseEntity<Void> deleteEmployerProfileLink(UUID profileId, UUID userId) {

        if (!isAllowedOnProfile(DELETE_LINK_ACTION, profileId)) {
            throw new ForbiddenException();
        }

        EmployerProfileLink employerUserLink =
                employerProfileLinkService
                        .getEmployerUserLink(profileId, userId)
                        .orElseThrow(() -> new NotFoundException("Employer user link not found"));

        try {
            employerProfileLinkService.deleteEmployerUserLink(employerUserLink.getId());
            String currentUserId = SecurityContextUtility.getAuthenticatedUserId();
            employerUserLink.setLastUpdatedBy(currentUserId);
            postAuditEventForEmployerProfileUserRemoved(employerUserLink);
        } catch (Exception e) {
            throw new UnexpectedException("Could not delete profile " + e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PageEmployerProfileLink> getEmployerProfileLinks(
            UUID profileId,
            UUID userId,
            String name,
            String email,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        if (!isAllowedOnProfile(VIEW_ACTION, profileId)) {
            throw new ForbiddenException();
        }

        List<UUID> userIds = null;
        if ((!StringUtils.isBlank(name) || !StringUtils.isBlank(email)) && userId == null) {
            List<UserEntity> users =
                    userService.findByEmailContainingIgnoreCaseAndDeletedFalse(email);
            users.addAll(userService.findByNameContainingIgnoreCaseAndDeletedFalse(name));
            userIds = users.stream().map(UserEntity::getId).toList();
        } else if (userId != null) {
            userIds = new ArrayList<>(List.of(userId));
        }

        Page<EmployerProfileLinkResponse> results =
                employerProfileLinkService
                        .getEmployerUserLinks(
                                new EmployerProfileLinkFilters(
                                        profileId,
                                        userIds,
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize))
                        .map(employerProfileLinkMapper::employerProfileLinkToResponseModel);

        PageEmployerProfileLink response = new PageEmployerProfileLink();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<PageIndividualLinksResponseModel> getIndividualLinks(
            UUID profileId,
            UUID userId,
            String name,
            String email,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        if (!isAllowedOnProfile(VIEW_ACTION, profileId)) {
            throw new ForbiddenException();
        }

        Optional<IndividualProfile> optionalIndividual =
                individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }

        Page<IndividualProfileLinkResponseModel> results =
                individualUserLinkService
                        .getIndividualLinksByFilters(
                                new IndividualProfileLinksFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        userId,
                                        email,
                                        name,
                                        profileId,
                                        userService))
                        .map(individualUserLinkMapper::individualProfileLinkToResponseModel);

        PageIndividualLinksResponseModel response = new PageIndividualLinksResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));
        return ResponseEntity.status(200).body(response);
    }

    @Override
    public ResponseEntity<IndividualProfileLinkResponseModel> updateIndividualProfileLink(
            UUID profileId,
            UUID userId,
            IndividualProfileLinkUpdateModel individualProfileLinkUpdateModel) {
        if (!isAllowedOnProfile(LINK_ACTION, profileId)) {
            throw new ForbiddenException();
        }

        Optional<IndividualProfile> optionalIndividual =
                individualService.getIndividualById(profileId);

        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }

        IndividualProfileLink savedIndividualLink =
                saveIndividualUserLink(
                        profileId,
                        optionalIndividual.get(),
                        ProfileAccessLevel.fromValue(
                                individualProfileLinkUpdateModel.getProfileAccessLevel()),
                        userId);

        return ResponseEntity.status(200)
                .body(
                        individualUserLinkMapper.individualProfileLinkToResponseModel(
                                savedIndividualLink));
    }

    @Override
    public ResponseEntity<Void> deleteIndividualProfileLink(UUID profileId, UUID userId) {
        if (!isAllowedOnProfile(DELETE_LINK_ACTION, profileId)) {
            throw new ForbiddenException();
        }

        Optional<IndividualProfile> optionalIndividual =
                individualService.getIndividualById(profileId);
        if (optionalIndividual.isEmpty()) {
            throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
        }
        Optional<IndividualProfileLink> existingIndividualUserLink =
                individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId);

        if (existingIndividualUserLink.isEmpty()) {
            throw new NotFoundException("individual user link not found");
        }

        individualUserLinkService.deleteIndividualUserLink(existingIndividualUserLink.get());

        postAuditEventForIndividualProfileUserRemoved(existingIndividualUserLink.get());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> postIndividualProfileInvitation(
            UUID profileId, ProfileInvitationRequestModel profileInvitationRequestModel) {
        return createProfileInvitation(
                profileId, profileInvitationRequestModel, ProfileType.INDIVIDUAL);
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> postEmployerProfileInvitation(
            UUID profileId, ProfileInvitationRequestModel profileInvitationRequestModel) {
        return createProfileInvitation(
                profileId, profileInvitationRequestModel, ProfileType.EMPLOYER);
    }

    private ResponseEntity<ProfileInvitationResponse> createProfileInvitation(
            UUID profileId,
            ProfileInvitationRequestModel profileInvitationRequestModel,
            ProfileType profileType) {

        Profile profile = validateAccessAndGetProfile(INVITE_ACTION, profileId, profileType);

        ProfileInvitation profileInvitation =
                invitationMapper.createModelToProfileInvitation(
                        profileId, profileInvitationRequestModel);

        ProfileInvitation savedProfileInvitation =
                invitationService.createProfileInvitation(
                        profileType, profile.getDisplayName(), profileInvitation);

        ProfileInvitationResponse profileInvitationResponse =
                invitationMapper.profileInvitationToResponseModel(savedProfileInvitation);

        invitationService.postAuditEventForProfileInvite(
                savedProfileInvitation, AuditActivityType.PROFILE_INVITATION_SENT);

        return ResponseEntity.ok(profileInvitationResponse);
    }

    @Override
    public ResponseEntity<PageProfileInvitationResponse> getIndividualProfileInvitations(
            UUID profileId,
            String accessLevel,
            String email,
            Boolean exactEmailMatch,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        return getProfileInvitations(
                profileId,
                accessLevel,
                email,
                exactEmailMatch,
                sortBy,
                sortOrder,
                pageNumber,
                pageSize,
                ProfileType.INDIVIDUAL);
    }

    @Override
    public ResponseEntity<PageProfileInvitationResponse> getEmployerProfileInvitations(
            UUID profileId,
            String accessLevel,
            String email,
            Boolean exactEmailMatch,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        return getProfileInvitations(
                profileId,
                accessLevel,
                email,
                exactEmailMatch,
                sortBy,
                sortOrder,
                pageNumber,
                pageSize,
                ProfileType.EMPLOYER);
    }

    @SuppressWarnings("java:S107")
    private ResponseEntity<PageProfileInvitationResponse> getProfileInvitations(
            UUID profileId,
            String accessLevel,
            String email,
            Boolean exactEmailMatch,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            ProfileType profileType) {

        Profile profile = validateAccessAndGetProfile(VIEW_ACTION, profileId, profileType);

        Page<ProfileInvitationResponse> results =
                invitationService
                        .getProfileInvitationsByFilters(
                                new ProfileInvitationFilters(
                                        sortBy,
                                        sortOrder,
                                        pageNumber,
                                        pageSize,
                                        accessLevel,
                                        email,
                                        exactEmailMatch,
                                        profileId,
                                        profile.getProfileType().getValue()))
                        .map(invitationMapper::profileInvitationToResponseModel);

        PageProfileInvitationResponse response = new PageProfileInvitationResponse();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> getIndividualProfileInvitationById(
            UUID invitationId) {
        return getProfileInvitationById(invitationId, ProfileType.INDIVIDUAL);
    }

    @Override
    public ResponseEntity<ProfileInvitationResponse> getEmployerProfileInvitationById(
            UUID invitationId) {
        return getProfileInvitationById(invitationId, ProfileType.EMPLOYER);
    }

    private ResponseEntity<ProfileInvitationResponse> getProfileInvitationById(
            UUID invitationId, ProfileType profileType) {

        ProfileInvitation invitation =
                invitationService
                        .getProfileInvitationById(invitationId)
                        .orElseThrow(() -> new NotFoundException(PROFILE_INVITATION_NOT_FOUND));

        if (!invitation.getEmail().equals(SecurityContextUtility.getAuthenticatedUserEmail())) {
            try {
                validateAccessAndGetProfile(VIEW_ACTION, invitation.getProfileId(), profileType);
            } catch (NotFoundException e) {
                throw new NotFoundException(PROFILE_INVITATION_NOT_FOUND);
            }
        }

        if (invitation.getType() != profileType) {
            throw new ProvidedDataException(
                    String.format(PROFILE_INVITATION_WRONG_TYPE, profileType));
        }

        return ResponseEntity.ok(invitationMapper.profileInvitationToResponseModel(invitation));
    }

    @Override
    public ResponseEntity<Void> claimIndividualProfileInvitation(UUID invitationId) {
        return claimProfileInvitation(invitationId, ProfileType.INDIVIDUAL);
    }

    @Override
    public ResponseEntity<Void> claimEmployerProfileInvitation(UUID invitationId) {
        return claimProfileInvitation(invitationId, ProfileType.EMPLOYER);
    }

    private ResponseEntity<Void> claimProfileInvitation(
            UUID invitationId, ProfileType profileType) {

        // hard check to avoid non-public users from claiming invitations if they are given
        // applicable public user roles
        if (!UserUtility.getAuthenticatedUserType()
                .trim()
                .equalsIgnoreCase(UserType.PUBLIC.getValue())) {
            throw new ForbiddenException("Claiming invitations is intended for public users only.");
        }

        ProfileInvitation invitation =
                invitationService
                        .getProfileInvitationById(invitationId)
                        .filter(
                                profileInvitation ->
                                        profileInvitation
                                                .getEmail()
                                                .equals(
                                                        SecurityContextUtility
                                                                .getAuthenticatedUserEmail()))
                        .orElseThrow(() -> new NotFoundException(PROFILE_INVITATION_NOT_FOUND));

        if (invitation.getType() != profileType) {
            throw new ProvidedDataException(
                    String.format(PROFILE_INVITATION_WRONG_TYPE, profileType));
        }

        invitationService.verifyInvitationIsClaimable(invitation);

        switch (profileType) {
            case EMPLOYER:
                employerService
                        .getEmployerById(invitation.getProfileId())
                        .ifPresentOrElse(
                                employer ->
                                        saveEmployerUserLink(
                                                invitation.getProfileId(),
                                                UUID.fromString(
                                                        SecurityContextUtility
                                                                .getAuthenticatedUserId()),
                                                employer,
                                                invitation.getAccessLevel()),
                                () -> {
                                    throw new NotFoundException(EMPLOYER_PROFILE_NOT_FOUND_MSG);
                                });
                break;

            case INDIVIDUAL:
                individualService
                        .getIndividualById(invitation.getProfileId())
                        .ifPresentOrElse(
                                individual ->
                                        saveIndividualUserLink(
                                                invitation.getProfileId(),
                                                individual,
                                                invitation.getAccessLevel(),
                                                UUID.fromString(
                                                        SecurityContextUtility
                                                                .getAuthenticatedUserId())),
                                () -> {
                                    throw new NotFoundException(INDIVIDUAL_PROFILE_NOT_FOUND_MSG);
                                });
                break;

            default:
                throw new UnexpectedException("Invitation profile type not supported for claiming");
        }

        invitationService.markAsClaimedAndSave(invitation);

        invitationService.postAuditEventForProfileInvite(
                invitation, AuditActivityType.PROFILE_INVITATION_CLAIMED);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteIndividualProfileInvitation(
            UUID profileId, UUID invitationId) {
        return deleteInvitation(profileId, invitationId, ProfileType.INDIVIDUAL);
    }

    @Override
    public ResponseEntity<Void> deleteEmployerProfileInvitation(UUID profileId, UUID invitationId) {
        return deleteInvitation(profileId, invitationId, ProfileType.EMPLOYER);
    }

    private ResponseEntity<Void> deleteInvitation(
            UUID profileId, UUID invitationId, ProfileType profileType) {

        validateAccessAndGetProfile(INVITE_ACTION, profileId, profileType);

        ProfileInvitation invitation =
                invitationService
                        .getProfileInvitationById(invitationId)
                        .filter(
                                profileInvitation ->
                                        profileInvitation.getProfileId().equals(profileId))
                        .orElseThrow(() -> new NotFoundException(PROFILE_INVITATION_NOT_FOUND));

        if (invitation.getType() != profileType) {
            throw new ProvidedDataException(
                    String.format(PROFILE_INVITATION_WRONG_TYPE, profileType));
        }

        if (Boolean.TRUE.equals(invitation.getClaimed())) {
            throw new ConflictException(
                    "Cannot delete an invitation that has already been claimed.");
        }

        invitationService.deleteProfileInvitation(invitationId);

        invitationService.postAuditEventForProfileInvite(
                invitation, AuditActivityType.PROFILE_INVITATION_DELETED);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resendIndividualProfileInvitation(
            UUID profileId, UUID invitationId) {
        return resendInvitation(profileId, invitationId, ProfileType.INDIVIDUAL);
    }

    @Override
    public ResponseEntity<Void> resendEmployerProfileInvitation(UUID profileId, UUID invitationId) {
        return resendInvitation(profileId, invitationId, ProfileType.EMPLOYER);
    }

    private ResponseEntity<Void> resendInvitation(
            UUID profileId, UUID invitationId, ProfileType profileType) {

        Profile profile = validateAccessAndGetProfile(INVITE_ACTION, profileId, profileType);

        ProfileInvitation invitation =
                invitationService
                        .getProfileInvitationById(invitationId)
                        .filter(
                                profileInvitation ->
                                        profileInvitation.getProfileId().equals(profileId))
                        .orElseThrow(() -> new NotFoundException(PROFILE_INVITATION_NOT_FOUND));

        invitationService.resendExistingInvitation(invitation, profile.getDisplayName());

        invitationService.postAuditEventForProfileInvite(
                invitation, AuditActivityType.PROFILE_INVITATION_RESENT);

        return ResponseEntity.noContent().build();
    }

    private void postAuditEventForIndividualProfileUserRemoved(
            IndividualProfileLink individualUserLink) {
        try {
            individualService.postAuditEventForIndividualProfileUserRemoved(individualUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " individual  profile user with owner user id %s for user"
                                    + " with id %s.",
                            individualUserLink.getProfile().getCreatedBy(),
                            individualUserLink.getUser().getId());
            log.error(errorMessage, e);
        }
    }

    private IndividualProfileLink saveIndividualUserLink(
            UUID profileId,
            IndividualProfile individual,
            ProfileAccessLevel profileAccessLevel,
            UUID userId) {

        Optional<IndividualProfileLink> optionalIndividualLink =
                individualUserLinkService.getIndividualUserLinkByProfileAndUserId(
                        profileId, userId);

        UserEntity userEntity =
                userService
                        .getUserByIdLoaded(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        IndividualProfileLink individualLinkToBeSaved = new IndividualProfileLink();

        individualLinkToBeSaved.setProfileType(ProfileType.INDIVIDUAL);
        individualLinkToBeSaved.setProfile(individual);
        individualLinkToBeSaved.setUser(userEntity);
        individualLinkToBeSaved.setProfileAccessLevel(profileAccessLevel);

        try {
            if (optionalIndividualLink.isPresent()) {
                individualLinkToBeSaved.setId(optionalIndividualLink.get().getId());
                individualLinkToBeSaved.setCreatedBy(optionalIndividualLink.get().getCreatedBy());
                individualLinkToBeSaved.setCreatedTimestamp(
                        optionalIndividualLink.get().getCreatedTimestamp());
                AuditableAction.builder(IndividualProfileLink.class)
                        .auditHandler(
                                new IndividualProfileUserAccessLevelChangedAuditHandler(
                                        individualAuditEventService))
                        .requestContextTimestamp(requestContextTimestamp)
                        .action(
                                individualUserLink ->
                                        individualUserLinkService.saveIndividualUserLink(
                                                individualLinkToBeSaved))
                        .build()
                        .execute(optionalIndividualLink.get());
            } else {
                individualUserLinkService.saveIndividualUserLink(individualLinkToBeSaved);
            }

        } catch (Exception e) {
            throw new UnexpectedException(e);
        }

        IndividualProfileLink savedIndividualLink =
                individualUserLinkService
                        .getIndividualUserLinkByProfileAndUserId(profileId, userId)
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "Individual profile link not found after saving"));

        if (optionalIndividualLink.isEmpty()) {
            postAuditEventForIndividualProfileUserCreated(savedIndividualLink);
        }
        return savedIndividualLink;
    }

    private void postAuditEventForIndividualProfileUserCreated(
            IndividualProfileLink individualUserLink) {
        try {
            individualService.postAuditEventForIndividualProfileUserAdded(individualUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " individual  profile user with owner user id %s for user"
                                    + " with id %s.",
                            individualUserLink.getProfile().getCreatedBy(),
                            individualUserLink.getUser().getId());
            log.error(errorMessage, e);
        }
    }

    private EmployerProfileLink saveEmployerUserLink(
            UUID profileId,
            UUID userId,
            EmployerProfile employer,
            ProfileAccessLevel profileAccessLevel) {
        Optional<EmployerProfileLink> employerUserLinkOptional =
                employerProfileLinkService.getEmployerUserLink(profileId, userId);

        EmployerProfileLink employerUserLinkToBeSaved = new EmployerProfileLink();

        UserEntity userEntity =
                userService
                        .getUserByIdLoaded(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));
        employerUserLinkToBeSaved.setUser(userEntity);
        employerUserLinkToBeSaved.setProfileAccessLevel(profileAccessLevel);
        employerUserLinkToBeSaved.setProfileType(ProfileType.EMPLOYER);
        employerUserLinkToBeSaved.setProfile(employer);

        try {
            if (employerUserLinkOptional.isPresent()) {
                employerUserLinkToBeSaved.setId(employerUserLinkOptional.get().getId());
                employerUserLinkToBeSaved.setCreatedBy(
                        employerUserLinkOptional.get().getCreatedBy());
                employerUserLinkToBeSaved.setCreatedTimestamp(
                        employerUserLinkOptional.get().getCreatedTimestamp());
                AuditableAction.builder(EmployerProfileLink.class)
                        .auditHandler(
                                new EmployerProfileUserAccessLevelChangedAuditHandler(
                                        employerAuditEventService))
                        .requestContextTimestamp(requestContextTimestamp)
                        .action(
                                employerLink ->
                                        employerProfileLinkService.saveEmployerUserLink(
                                                employerUserLinkToBeSaved))
                        .build()
                        .execute(employerUserLinkOptional.get());
            } else {
                employerProfileLinkService.saveEmployerUserLink(employerUserLinkToBeSaved);
            }

        } catch (Exception e) {
            throw new UnexpectedException(e);
        }

        EmployerProfileLink savedEmployerUserLink =
                employerProfileLinkService
                        .getEmployerUserLink(profileId, userId)
                        .orElseThrow(
                                () ->
                                        new UnexpectedException(
                                                "Employer User Link not found after saving"));

        if (employerUserLinkOptional.isEmpty()) {
            postAuditEventForEmployerProfileUserAdded(savedEmployerUserLink);
        }
        return savedEmployerUserLink;
    }

    private boolean isAllowedOnProfile(@NotNull String action, UUID profileId) {
        if (profileId != null) {
            Optional<Profile> profileOpt =
                    commonProfileService
                            .getProfileById(profileId)
                            .filter(p -> authorizationHandler.isAllowedForInstance(VIEW_ACTION, p));

            if (profileOpt.isPresent()) {
                Profile requestProfile = profileOpt.get();
                return authorizationHandler.isAllowedForInstance(action, requestProfile);
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Validates access and gets the profile. Also throws exceptions if access is not granted or profile is not of expected type.
     * 
     * @param action the Cerbos action to validate
     * @param profileId the wanted Profile id
     * @param profileType the ProfileType expected
     * @return the Profile
     * 
     * @throws NotFoundException if the profile is not found or reader access is not granted.
     * @throws ForbiddenException if the action is not allowed on the profile.
     * @throws ProvidedDataException if the profile is not of the expected type.
     */
    private @NotNull Profile validateAccessAndGetProfile(
            @NotNull String action, @NotNull UUID profileId, ProfileType profileType) {

        Profile profile =
                commonProfileService
                        .getProfileById(profileId)
                        .filter(p -> authorizationHandler.isAllowedForInstance(VIEW_ACTION, p))
                        .orElseThrow(() -> new NotFoundException("Profile not found"));

        if (!authorizationHandler.isAllowedForInstance(action, profile)) {
            throw new ForbiddenException("Forbidden action on the profile");
        }

        if (profile.getProfileType() != profileType) {
            throw new ProvidedDataException(
                    "Profile is not of required type (" + profileType.name() + ").");
        }

        return profile;
    }

    private void postAuditEventForEmployerProfileCreated(EmployerProfile profile) {
        try {
            employerService.postAuditEventForEmployerCreated(profile);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " employer profile with user id %s for profile with id %s.",
                            profile.getCreatedBy(),
                            profile.getId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForEmployerProfileUserAdded(
            EmployerProfileLink employerProfileLink) {
        try {
            employerService.postAuditEventForEmployerProfileUserAdded(employerProfileLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " employer  profile user with owner user id %s for user with"
                                    + " id %s.",
                            employerProfileLink.getProfile().getCreatedBy(),
                            employerProfileLink.getUser().getId());
            log.error(errorMessage, e);
        }
    }

    private void postAuditEventForEmployerProfileUserRemoved(EmployerProfileLink employerUserLink) {
        try {
            employerService.postAuditEventForEmployerProfileUserRemoved(employerUserLink);
        } catch (Exception e) {
            String errorMessage =
                    String.format(
                            CREATION_AUDIT_EVENT_ERR_MSG
                                    + " employer  profile user with owner user id %s for user with"
                                    + " id %s.",
                            employerUserLink.getProfile().getCreatedBy(),
                            employerUserLink.getUser().getId());
            log.error(errorMessage, e);
        }
    }
}
