package io.nuvalence.user.management.api.service.entity;

import io.nuvalence.user.management.api.service.entity.profile.Address;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AddressTest {
    @Test
    void equalsHashCodeContract() {
        IndividualProfile individualProfile1 = new IndividualProfile();
        individualProfile1.setId(UUID.randomUUID());
        individualProfile1.setFirstName("John");
        individualProfile1.setLastName("Doe");

        IndividualProfile individualProfile2 = new IndividualProfile();
        individualProfile2.setId(UUID.randomUUID());
        individualProfile2.setFirstName("Jane");
        individualProfile2.setLastName("Doe");

        EmployerProfile employerProfile1 = new EmployerProfile();
        employerProfile1.setId(UUID.randomUUID());
        employerProfile1.setLegalName("Company A");

        EmployerProfile employerProfile2 = new EmployerProfile();
        employerProfile2.setId(UUID.randomUUID());
        employerProfile2.setLegalName("Company B");

        EqualsVerifier.forClass(Address.class)
                .withPrefabValues(IndividualProfile.class, individualProfile1, individualProfile2)
                .withPrefabValues(EmployerProfile.class, employerProfile1, employerProfile2)
                .withIgnoredFields(
                        "individualForMailing",
                        "individualForAddress",
                        "employerForMailing",
                        "employerForLocations")
                .usingGetClass()
                .verify();
    }
}
