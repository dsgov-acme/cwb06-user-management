package io.nuvalence.user.management.api.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.Profile;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ProfileTest {

    @Test
    void testIndividualProfileType() {
        Profile individualProfile =
                new IndividualProfile() {
                    @Override
                    public UUID getId() {
                        return UUID.randomUUID();
                    }
                };

        assertEquals(
                ProfileType.INDIVIDUAL,
                individualProfile.getProfileType(),
                "The profile type should be INDIVIDUAL");
    }

    @Test
    void testEmployerProfileType() {
        Profile employerProfile =
                new EmployerProfile() {
                    @Override
                    public UUID getId() {
                        return UUID.randomUUID();
                    }
                };

        assertEquals(
                ProfileType.EMPLOYER,
                employerProfile.getProfileType(),
                "The profile type should be EMPLOYER");
    }

    @Test
    void testUnknownProfileType() {
        Profile unknownProfile =
                new Profile() {
                    @Override
                    public UUID getId() {
                        return UUID.randomUUID();
                    }

                    @Override
                    public String getDisplayName() {
                        return "Unknown Profile";
                    }
                };

        assertThrows(
                IllegalStateException.class,
                unknownProfile::getProfileType,
                "An IllegalStateException should be thrown for unknown profile types");
    }
}
