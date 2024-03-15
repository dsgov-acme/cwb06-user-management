package io.nuvalence.user.management.api.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.models.AccessProfileDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CommonProfileLinkServiceTest {

    @Mock private IndividualProfileLinkService individualProfileLinkService;

    @Mock private EmployerProfileLinkService employerProfileLinkService;

    @InjectMocks private CommonProfileLinkService commonProfileLinkService;

    @Test
    void testGetProfilesByUserId() {
        UUID userId = UUID.randomUUID();
        IndividualProfileLink individualLink =
                IndividualProfileLink.builder()
                        .profile(IndividualProfile.builder().id(UUID.randomUUID()).build())
                        .build();
        individualLink.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
        when(individualProfileLinkService.getIndividualLinksByUserId(userId))
                .thenReturn(Arrays.asList(individualLink));

        EmployerProfileLink employerLink =
                EmployerProfileLink.builder()
                        .profile(EmployerProfile.builder().id(UUID.randomUUID()).build())
                        .build();
        employerLink.setProfileAccessLevel(ProfileAccessLevel.READER);

        when(employerProfileLinkService.getEmployerLinksByUserId(userId))
                .thenReturn(Arrays.asList(employerLink));

        List<AccessProfileDto> profiles = commonProfileLinkService.getProfilesByUserId(userId);

        assertEquals(2, profiles.size());
        assertEquals(ProfileAccessLevel.ADMIN, profiles.get(0).getLevel());
        assertEquals(ProfileType.INDIVIDUAL, profiles.get(0).getType());
        assertEquals(individualLink.getProfile().getId(), profiles.get(0).getId());
        assertEquals(ProfileAccessLevel.READER, profiles.get(1).getLevel());
        assertEquals(ProfileType.EMPLOYER, profiles.get(1).getType());
        assertEquals(employerLink.getProfile().getId(), profiles.get(1).getId());
    }
}
