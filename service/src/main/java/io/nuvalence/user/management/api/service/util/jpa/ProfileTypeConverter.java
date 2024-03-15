package io.nuvalence.user.management.api.service.util.jpa;

import io.nuvalence.user.management.api.service.enums.ProfileType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProfileTypeConverter implements AttributeConverter<ProfileType, String> {
    @Override
    public String convertToDatabaseColumn(ProfileType entityValue) {
        return (entityValue == null) ? null : entityValue.toString();
    }

    @Override
    public ProfileType convertToEntityAttribute(String databaseValue) {
        return ProfileType.fromValue(databaseValue);
    }
}
