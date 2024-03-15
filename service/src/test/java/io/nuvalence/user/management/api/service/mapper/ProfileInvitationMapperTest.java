package io.nuvalence.user.management.api.service.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationRequestModel;
import io.nuvalence.user.management.api.service.generated.models.ProfileInvitationResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

class ProfileInvitationMapperTest {

    private final ProfileInvitationMapper mapper = Mappers.getMapper(ProfileInvitationMapper.class);
    private static final String TEST_EMAIL = "test@email.com";

    @Test
    void createModelToProfileInvitation() {
        UUID profileId = UUID.randomUUID();
        ProfileInvitationRequestModel model = new ProfileInvitationRequestModel();
        model.setAccessLevel("ADMIN");
        model.setEmail(TEST_EMAIL);

        ProfileInvitation result = mapper.createModelToProfileInvitation(profileId, model);

        ProfileInvitation expected =
                ProfileInvitation.builder()
                        .profileId(profileId)
                        .accessLevel(ProfileAccessLevel.ADMIN)
                        .email(TEST_EMAIL)
                        .build();

        assertEquals(expected, result);
    }

    @Test
    void profileInvitationToResponseModel() {
        UUID id = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        ProfileInvitation invitation =
                ProfileInvitation.builder()
                        .id(id)
                        .profileId(profileId)
                        .type(ProfileType.EMPLOYER)
                        .accessLevel(ProfileAccessLevel.WRITER)
                        .email(TEST_EMAIL)
                        .build();

        ProfileInvitationResponse result = mapper.profileInvitationToResponseModel(invitation);

        ProfileInvitationResponse expected = new ProfileInvitationResponse();
        expected.setId(id);
        expected.setProfileId(profileId);
        expected.setProfileType("EMPLOYER");
        expected.setAccessLevel("WRITER");
        expected.setEmail(TEST_EMAIL);

        assertEquals(expected, result);
    }
}
