package io.nuvalence.user.management.api.service.mapper;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileLinkResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EmployerProfileLinkMapper {
    EmployerProfileLinkMapper INSTANCE = Mappers.getMapper(EmployerProfileLinkMapper.class);

    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(source = "user.id", target = "userId")
    EmployerProfileLinkResponse employerProfileLinkToResponseModel(EmployerProfileLink employer);
}
