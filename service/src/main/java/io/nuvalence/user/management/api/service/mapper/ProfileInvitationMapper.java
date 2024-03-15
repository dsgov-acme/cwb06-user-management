package io.nuvalence.user.management.api.service.mapper;

import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ProfileInvitationMapper {
    ProfileInvitationMapper INSTANCE = Mappers.getMapper(ProfileInvitationMapper.class);

    ProfileInvitation createModelToProfileInvitation(
            UUID profileId, ProfileInvitationRequestModel createModel);

    @Mapping(source = "type", target = "profileType")
    ProfileInvitationResponse profileInvitationToResponseModel(ProfileInvitation profileInvitation);
}
