package io.nuvalence.user.management.api.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.user.management.api.service.entity.profile.Address;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class EmployerProfileTest {
    @Test
    void equalsHashCodeContract() {
        Address address1 = new Address();
        address1.setId(UUID.randomUUID());
        address1.setAddress1("123 Main St");
        address1.setCity("City A");

        Address address2 = new Address();
        address2.setId(UUID.randomUUID());
        address2.setAddress1("456 Main St");
        address2.setCity("City B");

        List<Address> locations1 = new ArrayList<>();
        locations1.add(address1);

        List<Address> locations2 = new ArrayList<>();
        locations2.add(address2);

        EqualsVerifier.forClass(EmployerProfile.class)
                .withPrefabValues(Address.class, address1, address2)
                .withPrefabValues(List.class, locations1, locations2)
                .usingGetClass()
                .verify();
    }

    @Test
    void getDisplayName() {
        EmployerProfile employerProfile = new EmployerProfile();
        employerProfile.setLegalName("Legal Name");

        assertEquals("Legal Name", employerProfile.getDisplayName());
    }
}
