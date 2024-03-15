package io.nuvalence.user.management.api.service.config;

import static io.nuvalence.user.management.api.service.enums.ProfileType.EMPLOYER;
import static io.nuvalence.user.management.api.service.enums.ProfileType.INDIVIDUAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.models.AccessProfileDto;
import io.nuvalence.user.management.api.service.service.EmployerProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CerbosPrincipalAttributeProviderImplTest {
    @Mock private IndividualProfileLinkService individualUserLinkService;
    @Mock private EmployerProfileLinkService employerUserLinkService;
    @InjectMocks private CerbosPrincipalAttributeProviderImpl provider;

    @Test
    void getAttributesTest() {
        try (MockedStatic<SecurityContextUtility> mocked =
                Mockito.mockStatic(SecurityContextUtility.class)) {
            mocked.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());

            UUID individualId = UUID.randomUUID();
            IndividualProfile individual = IndividualProfile.builder().id(individualId).build();
            IndividualProfileLink individualUserLink =
                    IndividualProfileLink.builder().profile(individual).build();
            individualUserLink.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
            when(individualUserLinkService.getIndividualLinksByUserId(any()))
                    .thenReturn(List.of(individualUserLink));

            UUID employerId = UUID.randomUUID();
            EmployerProfile employer = EmployerProfile.builder().id(employerId).build();
            EmployerProfileLink employerUserLink =
                    EmployerProfileLink.builder().profile(employer).build();
            employerUserLink.setProfileAccessLevel(ProfileAccessLevel.READER);
            when(employerUserLinkService.getEmployerLinksByUserId(any()))
                    .thenReturn(List.of(employerUserLink));

            Map<String, Object> result = provider.getAttributes(mock(Authentication.class));

            assertTrue(result.containsKey("accessProfiles"));
            Object accessProfilesObject = result.get("accessProfiles");
            assertNotNull(accessProfilesObject);
            assertTrue(accessProfilesObject instanceof List);

            List<?> accessProfiles = (List<?>) accessProfilesObject;
            assertEquals(2, accessProfiles.size());

            accessProfiles.forEach(profile -> assertTrue(profile instanceof AccessProfileDto));

            assertEquals(INDIVIDUAL, ((AccessProfileDto) accessProfiles.get(0)).getType());
            assertEquals(
                    ProfileAccessLevel.ADMIN,
                    ((AccessProfileDto) accessProfiles.get(0)).getLevel());

            assertEquals(EMPLOYER, ((AccessProfileDto) accessProfiles.get(1)).getType());
            assertEquals(
                    ProfileAccessLevel.READER,
                    ((AccessProfileDto) accessProfiles.get(1)).getLevel());
        }
    }
}
