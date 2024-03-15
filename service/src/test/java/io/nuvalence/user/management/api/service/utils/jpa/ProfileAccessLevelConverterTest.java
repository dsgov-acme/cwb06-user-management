package io.nuvalence.user.management.api.service.utils.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.util.jpa.ProfileAccessLevelConverter;
import org.junit.jupiter.api.Test;

class ProfileAccessLevelConverterTest {

    private final ProfileAccessLevelConverter converter = new ProfileAccessLevelConverter();

    @Test
    void convertToDatabaseColumn() {
        assertEquals("ADMIN", converter.convertToDatabaseColumn(ProfileAccessLevel.ADMIN));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute() {
        assertEquals(ProfileAccessLevel.ADMIN, converter.convertToEntityAttribute("ADMIN"));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
