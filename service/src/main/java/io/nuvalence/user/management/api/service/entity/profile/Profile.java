package io.nuvalence.user.management.api.service.entity.profile;

import io.nuvalence.user.management.api.service.enums.ProfileType;

import java.util.UUID;

/**
 * Represents a profile.
 */
public interface Profile {

    UUID getId();

    String getDisplayName();

    // ProfileType getProfileType(); // this method signature is a recommended approach instead of
    // the following default method implementation. But this is being moved to tokens and
    // user-management
    /**
     * Returns the type of profile.
     *
     * @return the profile type
     */
    default ProfileType getProfileType() {
        if (this instanceof IndividualProfile) {
            return ProfileType.INDIVIDUAL;
        } else if (this instanceof EmployerProfile) {
            return ProfileType.EMPLOYER;
        } else {
            throw new IllegalStateException("Unknown profile type");
        }
    }
}
