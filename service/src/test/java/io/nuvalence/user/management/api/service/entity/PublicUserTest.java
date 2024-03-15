package io.nuvalence.user.management.api.service.entity;

import static org.junit.jupiter.api.Assertions.*;

import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import org.junit.jupiter.api.Test;

class PublicUserTest {

    public static final String FIRST_NAME = "John";
    public static final String MIDDLE_NAME = "Q";
    public static final String LAST_NAME = "Smith";
    public static final String EMAIL = "jsmith@employer.com";
    public static final String PHONE_NUMBER = "123-456-7890";

    @Test
    void testUserWithoutProfile() {
        PublicUser user = new PublicUser();
        user.setFirstName(FIRST_NAME);
        user.setMiddleName(MIDDLE_NAME);
        user.setLastName(LAST_NAME);
        user.setEmail(EMAIL);
        user.setPhoneNumber(PHONE_NUMBER);

        assertEquals(FIRST_NAME, user.getFirstName());
        assertEquals(MIDDLE_NAME, user.getMiddleName());
        assertEquals(LAST_NAME, user.getLastName());
        assertEquals(EMAIL, user.getEmail());
        assertEquals(PHONE_NUMBER, user.getPhoneNumber());
    }

    @Test
    void testUserWithProfile() {
        PublicUser user = new PublicUser();
        user.setIndividualProfile(new IndividualProfile());
        user.setFirstName(FIRST_NAME);
        user.setMiddleName(MIDDLE_NAME);
        user.setLastName(LAST_NAME);
        user.setEmail(EMAIL);
        user.setPhoneNumber(PHONE_NUMBER);

        // The profile values should be updated
        assertEquals(FIRST_NAME, user.getIndividualProfile().getFirstName());
        assertEquals(MIDDLE_NAME, user.getIndividualProfile().getMiddleName());
        assertEquals(LAST_NAME, user.getIndividualProfile().getLastName());
        assertEquals(EMAIL, user.getIndividualProfile().getEmail());
        assertEquals(PHONE_NUMBER, user.getIndividualProfile().getPhoneNumber());

        // assert that the native values were also updated
        user.setIndividualProfile(null);
        assertEquals(FIRST_NAME, user.getFirstName());
        assertEquals(MIDDLE_NAME, user.getMiddleName());
        assertEquals(LAST_NAME, user.getLastName());
        assertEquals(EMAIL, user.getEmail());
        assertEquals(PHONE_NUMBER, user.getPhoneNumber());
    }
}
