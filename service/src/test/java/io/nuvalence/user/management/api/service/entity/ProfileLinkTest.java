package io.nuvalence.user.management.api.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfile;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.entity.profile.ProfileLink;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class ProfileLinkTest {

    private static final String CREATOR = "creator";
    private static final String UPDATER = "updater";

    private static class ConcreteProfileLink extends ProfileLink<EmployerProfile> {
        private EmployerProfile profile;

        @Override
        public EmployerProfile getProfile() {
            return profile;
        }
    }

    @Test
    void testEquals() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ConcreteProfileLink link1 = new ConcreteProfileLink();
        link1.setId(id);
        link1.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
        link1.setCreatedBy(CREATOR);
        link1.setLastUpdatedBy(UPDATER);
        link1.setCreatedTimestamp(now);
        link1.setLastUpdatedTimestamp(now);

        ConcreteProfileLink link2 = new ConcreteProfileLink();
        link2.setId(id);
        link2.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
        link2.setCreatedBy(CREATOR);
        link2.setLastUpdatedBy(UPDATER);
        link2.setCreatedTimestamp(now);
        link2.setLastUpdatedTimestamp(now);

        assertTrue(
                link1.equals(link2) && link2.equals(link1), "ProfileLink objects should be equal");
        assertEquals(
                link1.hashCode(), link2.hashCode(), "Hash codes should be equal for equal objects");
    }

    @Test
    void testNotEquals() {
        ConcreteProfileLink link1 = new ConcreteProfileLink();
        link1.setId(UUID.randomUUID());
        link1.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
        link1.setCreatedBy(CREATOR);
        link1.setLastUpdatedBy(UPDATER);
        link1.setCreatedTimestamp(OffsetDateTime.now());
        link1.setLastUpdatedTimestamp(OffsetDateTime.now());

        ConcreteProfileLink link2 = new ConcreteProfileLink();
        link2.setId(UUID.randomUUID());
        link2.setProfileAccessLevel(ProfileAccessLevel.ADMIN);
        link2.setCreatedBy(CREATOR);
        link2.setLastUpdatedBy(UPDATER);
        link2.setCreatedTimestamp(OffsetDateTime.now());
        link2.setLastUpdatedTimestamp(OffsetDateTime.now());

        assertNotEquals(link1, link2);
    }

    @Test
    void testEqualsDifferentAccessLevel() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ConcreteProfileLink link1 =
                createConcreteProfileLink(id, ProfileAccessLevel.ADMIN, CREATOR, UPDATER, now, now);
        ConcreteProfileLink link2 =
                createConcreteProfileLink(
                        id, ProfileAccessLevel.READER, CREATOR, UPDATER, now, now);

        assertNotEquals(
                link1, link2, "ProfileLink objects should not be equal if access levels differ");
    }

    @Test
    void testEqualsDifferentCreatedBy() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ConcreteProfileLink link1 =
                createConcreteProfileLink(id, ProfileAccessLevel.ADMIN, CREATOR, UPDATER, now, now);
        ConcreteProfileLink link2 =
                createConcreteProfileLink(
                        id, ProfileAccessLevel.ADMIN, "differentCreator", UPDATER, now, now);

        assertNotEquals(
                link1, link2, "ProfileLink objects should not be equal if createdBy differ");
    }

    @Test
    void testEqualsDifferentLastUpdatedBy() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        ConcreteProfileLink link1 =
                createConcreteProfileLink(id, ProfileAccessLevel.ADMIN, CREATOR, UPDATER, now, now);
        ConcreteProfileLink link2 =
                createConcreteProfileLink(
                        id, ProfileAccessLevel.ADMIN, CREATOR, "differentUpdater", now, now);

        assertNotEquals(
                link1, link2, "ProfileLink objects should not be equal if lastUpdatedBy differ");
    }

    @Test
    void testEqualsDifferentCreatedTimestamp() {
        UUID id = UUID.randomUUID();

        ConcreteProfileLink link1 =
                createConcreteProfileLink(
                        id,
                        ProfileAccessLevel.ADMIN,
                        CREATOR,
                        UPDATER,
                        OffsetDateTime.now(),
                        OffsetDateTime.now());
        ConcreteProfileLink link2 =
                createConcreteProfileLink(
                        id,
                        ProfileAccessLevel.ADMIN,
                        CREATOR,
                        UPDATER,
                        OffsetDateTime.now().minusDays(1),
                        OffsetDateTime.now());

        assertNotEquals(
                link1,
                link2,
                "ProfileLink objects should not be equal if createdTimestamps differ");
    }

    @Test
    void testEqualsDifferentLastUpdatedTimestamp() {
        UUID id = UUID.randomUUID();

        ConcreteProfileLink link1 =
                createConcreteProfileLink(
                        id,
                        ProfileAccessLevel.ADMIN,
                        CREATOR,
                        UPDATER,
                        OffsetDateTime.now(),
                        OffsetDateTime.now());
        ConcreteProfileLink link2 =
                createConcreteProfileLink(
                        id,
                        ProfileAccessLevel.ADMIN,
                        CREATOR,
                        UPDATER,
                        OffsetDateTime.now(),
                        OffsetDateTime.now().minusDays(1));

        assertNotEquals(
                link1,
                link2,
                "ProfileLink objects should not be equal if lastUpdatedTimestamps differ");
    }

    private ConcreteProfileLink createConcreteProfileLink(
            UUID id,
            ProfileAccessLevel accessLevel,
            String createdBy,
            String lastUpdatedBy,
            OffsetDateTime createdTimestamp,
            OffsetDateTime lastUpdatedTimestamp) {
        ConcreteProfileLink link = new ConcreteProfileLink();
        link.setId(id);
        link.setProfileAccessLevel(accessLevel);
        link.setCreatedBy(createdBy);
        link.setLastUpdatedBy(lastUpdatedBy);
        link.setCreatedTimestamp(createdTimestamp);
        link.setLastUpdatedTimestamp(lastUpdatedTimestamp);
        return link;
    }
}
