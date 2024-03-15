package io.nuvalence.user.management.api.service.util.jpa;

import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for Profile access level.
 */
@Converter(autoApply = true)
public class ProfileAccessLevelConverter implements AttributeConverter<ProfileAccessLevel, String> {

    @Override
    public String convertToDatabaseColumn(ProfileAccessLevel profileAccessLevel) {
        if (profileAccessLevel == null) {
            return null;
        }
        return profileAccessLevel.name();
    }

    @Override
    public ProfileAccessLevel convertToEntityAttribute(String status) {
        if (status == null) {
            return null;
        }
        return ProfileAccessLevel.valueOf(status);
    }
}
