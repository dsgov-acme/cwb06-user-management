package io.nuvalence.user.management.api.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CommonProfileServiceTest {

    @Mock private IndividualProfileService individualService;

    @Mock private EmployerProfileService employerService;

    @InjectMocks private CommonProfileService commonProfileService;

    @Test
    void getProfileById_IndividualFound() {
        UUID id = UUID.randomUUID();
        IndividualProfile individualProfile = mock(IndividualProfile.class);
        when(individualService.getIndividualById(id)).thenReturn(Optional.of(individualProfile));

        Optional<Profile> result = commonProfileService.getProfileById(id);

        assertTrue(result.isPresent());
        assertEquals(individualProfile, result.get());
    }

    @Test
    void getProfileById_EmployerFound() {
        UUID id = UUID.randomUUID();
        EmployerProfile employerProfile = mock(EmployerProfile.class);
        when(individualService.getIndividualById(id)).thenReturn(Optional.empty());
        when(employerService.getEmployerById(id)).thenReturn(Optional.of(employerProfile));

        Optional<Profile> result = commonProfileService.getProfileById(id);

        assertTrue(result.isPresent());
        assertEquals(employerProfile, result.get());
    }

    @Test
    void getProfileById_NoProfileFound() {
        UUID id = UUID.randomUUID();
        when(individualService.getIndividualById(id)).thenReturn(Optional.empty());
        when(employerService.getEmployerById(id)).thenReturn(Optional.empty());

        Optional<Profile> result = commonProfileService.getProfileById(id);

        assertFalse(result.isPresent());
    }
}
