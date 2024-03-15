package io.nuvalence.user.management.api.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nuvalence.user.management.api.service.entity.profile.Address;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class IndividualProfileTest {
    private static final String FIRST_NAME = "John";
    private static final String MIDDLE_NAME = "Locke";
    private static final String LAST_NAME = "Doe";

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

        EqualsVerifier.forClass(IndividualProfile.class)
                .withPrefabValues(Address.class, address1, address2)
                .usingGetClass()
                .verify();
    }

    @Test
    void shouldCreateDisplayNameWithAllFields() {
        IndividualProfile individualProfile = new IndividualProfile();

        individualProfile.setFirstName(FIRST_NAME);
        individualProfile.setMiddleName(MIDDLE_NAME);
        individualProfile.setLastName(LAST_NAME);

        assertEquals("John Locke Doe", individualProfile.getDisplayName());
    }

    @Test
    void shouldCreateDisplayNameWithNullMiddleName() {
        IndividualProfile individualProfile = new IndividualProfile();

        individualProfile.setFirstName(FIRST_NAME);
        individualProfile.setLastName(LAST_NAME);

        assertEquals("John Doe", individualProfile.getDisplayName());
    }

    @Test
    void shouldCreateDisplayNameWithMultipleSpaces() {
        IndividualProfile individualProfile = new IndividualProfile();

        individualProfile.setFirstName("John   ");
        individualProfile.setMiddleName("  Locke");
        individualProfile.setLastName("Doe");

        assertEquals("John Locke Doe", individualProfile.getDisplayName());
    }

    @Test
    void shouldCreateDisplayNameWithEmail() {
        IndividualProfile individualProfile = new IndividualProfile();
        individualProfile.setEmail("email@email.com");

        assertEquals("email@email.com", individualProfile.getDisplayName());
    }

    @Test
    void shouldCreateDisplayNameWithId() {
        IndividualProfile individualProfile = new IndividualProfile();
        UUID id = UUID.randomUUID();
        individualProfile.setId(id);

        assertEquals(id.toString(), individualProfile.getDisplayName());
    }
}
