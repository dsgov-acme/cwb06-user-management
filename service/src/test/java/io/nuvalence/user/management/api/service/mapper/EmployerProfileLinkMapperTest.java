package io.nuvalence.user.management.api.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.generated.models.EmployerProfileLinkResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

class EmployerProfileLinkMapperTest {
    private final EmployerProfileLinkMapper mapper =
            Mappers.getMapper(EmployerProfileLinkMapper.class);

    @Test
    void employerProfileLinkToResponseModel_mapsCorrectly() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileAccessLevel accessLevel = ProfileAccessLevel.ADMIN;

        EmployerProfile profile = new EmployerProfile();
        profile.setId(profileId);

        PublicUser user = new PublicUser();
        user.setId(userId);

        EmployerProfileLink employerProfileLink =
                EmployerProfileLink.builder().profile(profile).build();
        employerProfileLink.setUser(user);
        employerProfileLink.setProfileAccessLevel(accessLevel);

        EmployerProfileLinkResponse responseModel =
                mapper.employerProfileLinkToResponseModel(employerProfileLink);

        assertThat(responseModel.getProfileId()).isEqualTo(profileId);
        assertThat(responseModel.getUserId()).isEqualTo(userId);
        assertThat(responseModel.getProfileAccessLevel()).isEqualTo(accessLevel.toString());
    }
}
