package io.nuvalence.user.management.api.service.mapper;

import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileLinkResponseModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface IndividualProfileLinkMapper {
    IndividualProfileLinkMapper INSTANCE = Mappers.getMapper(IndividualProfileLinkMapper.class);

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(source = "user.id", target = "userId")
    IndividualProfileLinkResponseModel individualProfileLinkToResponseModel(
            IndividualProfileLink individualUserLink);
}
