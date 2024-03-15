package io.nuvalence.user.management.api.service.controller;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.user.management.api.service.config.exception.ResourceNotFoundException;
import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.RoleEntity;
import io.nuvalence.user.management.api.service.entity.UserEntity;
import io.nuvalence.user.management.api.service.entity.UserPreferenceEntity;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.enums.SortOrder;
import io.nuvalence.user.management.api.service.generated.controllers.UsersApiDelegate;
import io.nuvalence.user.management.api.service.generated.models.UserCreationRequest;
import io.nuvalence.user.management.api.service.generated.models.UserDTO;
import io.nuvalence.user.management.api.service.generated.models.UserPageDTO;
import io.nuvalence.user.management.api.service.generated.models.UserPreferenceDTO;
import io.nuvalence.user.management.api.service.generated.models.UserUpdateRequest;
import io.nuvalence.user.management.api.service.mapper.PagingMetadataMapper;
import io.nuvalence.user.management.api.service.mapper.UserEntityMapper;
import io.nuvalence.user.management.api.service.mapper.UserPreferenceEntityMapper;
import io.nuvalence.user.management.api.service.models.AccessProfileDto;
import io.nuvalence.user.management.api.service.service.CommonProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileService;
import io.nuvalence.user.management.api.service.service.RoleService;
import io.nuvalence.user.management.api.service.service.UserPreferenceService;
import io.nuvalence.user.management.api.service.service.UserSearchCriteria;
import io.nuvalence.user.management.api.service.service.UserService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for cloud function user actions.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@Slf4j
class UsersApiDelegateImpl implements UsersApiDelegate {

    private static final String USER_NOT_FOUND_EXCEPTION_MESSAGE = "User not found!";

    private static final String VIEW_AUTHORIZATION = "view";
    private static final String UPDATE_AUTHORIZATION = "update";

    private static final String ACCESS_DENIED_UPDATE_EXCEPTION_MESSAGE =
            "You do not have permission to modify this resource.";
    private final UserService userService;

    private final IndividualProfileLinkService individualUserLinkService;

    private final AuthorizationHandler authorizationHandler;

    private final PagingMetadataMapper pagingMetadataMapper;

    private final UserPreferenceService userPreferenceService;
    private final RoleService roleService;

    private final UserEntityMapper userMapper;

    private final IndividualProfileService individualProfileService;

    private final CommonProfileLinkService commonProfileLinkService;

    @Override
    public ResponseEntity<UserDTO> addUser(
            UserCreationRequest body, Boolean includeApplicationRoles, Boolean includeProfiles) {
        if (!authorizationHandler.isAllowed("create", UserEntity.class)) {
            throw new AccessDeniedException("You do not have permission to create this resource.");
        }

        try {
            final UserEntity user = userMapper.convertUserCreationRequestToUserEntity(body);

            if (user instanceof PublicUser publicUser
                    && publicUser.getIndividualProfile() == null) {
                IndividualProfile individualProfile =
                        IndividualProfile.builder()
                                .firstName(publicUser.getFirstName())
                                .middleName(publicUser.getMiddleName())
                                .lastName(publicUser.getLastName())
                                .email(publicUser.getEmail())
                                .phoneNumber(publicUser.getPhoneNumber())
                                .build();
                ((PublicUser) user).setIndividualProfile(individualProfile);
            }

            UserEntity savedUser = userService.createUser(user);

            if (user instanceof PublicUser savedPublicUser) {
                IndividualProfileLink individualLinkToBeSaved = new IndividualProfileLink();
                individualLinkToBeSaved.setProfileType(ProfileType.INDIVIDUAL);
                individualLinkToBeSaved.setProfile(savedPublicUser.getIndividualProfile());
                individualLinkToBeSaved.setUser(savedPublicUser);
                individualLinkToBeSaved.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
                individualUserLinkService.saveIndividualUserLink(individualLinkToBeSaved);

                individualProfileService.postAuditEventForIndividualProfileCreated(
                        savedPublicUser.getIndividualProfile());
            }

            return ResponseEntity.ok(
                    userMapper.entityToModelWithAppRolesAndProfiles(
                            savedUser,
                            getAppRolesIfIncluded(includeApplicationRoles, savedUser.getId()),
                            getLinksIfIncluded(includeProfiles, savedUser.getId())));
        } catch (RuntimeException e) {
            if (userService.isDuplicateExternalUserException(e)) {
                throw new ConstraintViolationException(
                        "A user already exists with this identityProvider and externalId",
                        Collections.emptySet());
            }

            throw e;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<UserPageDTO> getUserList(
            List<String> roleIds,
            String email,
            String externalId,
            String name,
            Integer pageNumber,
            Integer pageSize,
            String sortOrder,
            String sortBy,
            List<String> roleNames,
            String identityProvider,
            String userType,
            Boolean includeDeleted,
            Boolean includeApplicationRoles,
            Boolean includeProfiles) {
        final Pageable pageable =
                PageRequest.of(
                        pageNumber,
                        pageSize,
                        (SortOrder.DESC.toString().equals(sortBy))
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending());

        final UserSearchCriteria searchCriteria =
                UserSearchCriteria.builder()
                        .email(email)
                        .externalId(externalId)
                        .identityProvider(identityProvider)
                        .userType(userType)
                        .name(name)
                        .includeDeleted(includeDeleted)
                        .roleIds(roleIds)
                        .roleNames(roleNames)
                        .build();

        final Page<UserEntity> userPage =
                userService.getUsersBySearchCriteria(searchCriteria, pageable);

        final List<UserDTO> users =
                userPage.stream()
                        .filter(
                                authorizationHandler.getAuthFilter(
                                        VIEW_AUTHORIZATION, UserEntity.class))
                        .map(
                                u ->
                                        userMapper.entityToModelWithAppRolesAndProfiles(
                                                u,
                                                getAppRolesIfIncluded(
                                                        includeApplicationRoles, u.getId()),
                                                getLinksIfIncluded(includeProfiles, u.getId())))
                        .toList();

        final UserPageDTO userPageDTO = new UserPageDTO();
        userPageDTO.setUsers(users);
        userPageDTO.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(userPage));

        return ResponseEntity.ok(userPageDTO);
    }

    @Override
    public ResponseEntity<Void> deleteUserById(final UUID id) {
        if (!authorizationHandler.isAllowed("delete", UserEntity.class)) {
            throw new AccessDeniedException("You do not have permission to delete this resource.");
        }
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<UserDTO> getUserById(
            final UUID id, Boolean includeApplicationRoles, Boolean includeProfiles) {

        Optional<UserEntity> userOptional = userService.getUserByIdLoaded(id);
        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException(USER_NOT_FOUND_EXCEPTION_MESSAGE);
        }
        UserEntity user = userOptional.get();
        if (!authorizationHandler.isAllowedForInstance(VIEW_AUTHORIZATION, user)) {
            user = userService.trimUserDataToPublicFields(user);
            return ResponseEntity.ok(userMapper.entityToModel(user));
        }

        return ResponseEntity.ok(
                userMapper.entityToModelWithAppRolesAndProfiles(
                        user,
                        getAppRolesIfIncluded(includeApplicationRoles, id),
                        getLinksIfIncluded(includeProfiles, id)));
    }

    @Override
    public ResponseEntity<UserDTO> updateUserById(
            final UUID id,
            UserUpdateRequest body,
            Boolean includeApplicationRoles,
            Boolean includeProfiles) {
        Optional<UserEntity> userEntityOptional = userService.getUserByIdLoaded(id);

        if (userEntityOptional.isEmpty()) {
            throw new ResourceNotFoundException(USER_NOT_FOUND_EXCEPTION_MESSAGE);
        }
        if (!authorizationHandler.isAllowedForInstance(
                UPDATE_AUTHORIZATION, userEntityOptional.get())) {
            throw new AccessDeniedException(ACCESS_DENIED_UPDATE_EXCEPTION_MESSAGE);
        }

        UserEntity user = userService.updateUserById(id, body);

        return ResponseEntity.ok(
                userMapper.entityToModelWithAppRolesAndProfiles(
                        user,
                        getAppRolesIfIncluded(includeApplicationRoles, id),
                        getLinksIfIncluded(includeProfiles, id)));
    }

    @Override
    public ResponseEntity<UserPreferenceDTO> getUserPreferences(UUID id) {

        UserPreferenceDTO preferences =
                userPreferenceService
                        .getUserPreferences(id)
                        .filter(
                                authorizationHandler.getAuthFilter(
                                        VIEW_AUTHORIZATION, UserPreferenceEntity.class))
                        .map(
                                UserPreferenceEntityMapper.INSTANCE
                                        ::convertUserPreferenceEntityToUserModel)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Preferences not found for given user!"));

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(preferences);
    }

    @Override
    public ResponseEntity<Void> updatePreferences(UUID id, UserPreferenceDTO userPreferences) {
        UserEntity user =
                userService
                        .getUserByIdLoaded(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                USER_NOT_FOUND_EXCEPTION_MESSAGE));

        if (!authorizationHandler.isAllowedForInstance(UPDATE_AUTHORIZATION, user)) {
            throw new AccessDeniedException(ACCESS_DENIED_UPDATE_EXCEPTION_MESSAGE);
        }

        userPreferenceService.updateUserPreferences(userPreferences, user);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> addRoleToUser(UUID userId, UUID roleId) {
        UserEntity user =
                userService
                        .getUserByIdLoaded(userId)
                        .filter(
                                userEntity ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_AUTHORIZATION, userEntity))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                USER_NOT_FOUND_EXCEPTION_MESSAGE));

        if (!authorizationHandler.isAllowedForInstance(UPDATE_AUTHORIZATION, user)) {
            throw new AccessDeniedException(ACCESS_DENIED_UPDATE_EXCEPTION_MESSAGE);
        }

        RoleEntity role =
                roleService
                        .getRole(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found!"));

        if (!user.getRoles().contains(role)) {
            userService.addRoleToUser(user, role);
        }

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRoleFromUser(UUID userId, UUID roleId) {
        UserEntity user =
                userService
                        .getUserByIdLoaded(userId)
                        .filter(
                                userEntity ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_AUTHORIZATION, userEntity))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                USER_NOT_FOUND_EXCEPTION_MESSAGE));

        if (!authorizationHandler.isAllowedForInstance(UPDATE_AUTHORIZATION, user)) {
            throw new AccessDeniedException(ACCESS_DENIED_UPDATE_EXCEPTION_MESSAGE);
        }

        RoleEntity role =
                roleService
                        .getRole(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found!"));

        if (user.getRoles().contains(role)) {
            userService.deleteRoleFromUser(user, role);
        }

        return ResponseEntity.noContent().build();
    }

    private List<String> getAppRolesIfIncluded(Boolean includeApplicationRoles, UUID userId) {
        return Optional.ofNullable(includeApplicationRoles)
                .filter(Boolean::booleanValue)
                .map(included -> userService.getAppRolesByUserId(userId))
                .orElse(null);
    }

    private List<AccessProfileDto> getLinksIfIncluded(Boolean includeProfiles, UUID userId) {
        return Optional.ofNullable(includeProfiles)
                .filter(Boolean::booleanValue)
                .map(included -> commonProfileLinkService.getProfilesByUserId(userId))
                .orElse(null);
    }
}
