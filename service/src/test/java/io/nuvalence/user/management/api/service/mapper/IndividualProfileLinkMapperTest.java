package io.nuvalence.user.management.api.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.generated.models.IndividualProfileLinkResponseModel;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

class IndividualProfileLinkMapperTest {
    private final IndividualProfileLinkMapper mapper =
            Mappers.getMapper(IndividualProfileLinkMapper.class);

    @Test
    void individualProfileLinkToResponseModel_mapsCorrectly() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProfileAccessLevel accessLevel = ProfileAccessLevel.ADMIN;
        IndividualProfile profile = new IndividualProfile();
        profile.setId(profileId);

        PublicUser user = new PublicUser();
        user.setId(userId);
        IndividualProfileLink individualProfileLink =
                IndividualProfileLink.builder().profile(profile).build();
        individualProfileLink.setUser(user);
        individualProfileLink.setProfileAccessLevel(accessLevel);

        IndividualProfileLinkResponseModel responseModel =
                mapper.individualProfileLinkToResponseModel(individualProfileLink);

        assertThat(responseModel.getProfileId()).isEqualTo(profileId);
        assertThat(responseModel.getUserId()).isEqualTo(userId);
        assertThat(responseModel.getProfileAccessLevel()).isEqualTo(accessLevel.toString());
    }
}
