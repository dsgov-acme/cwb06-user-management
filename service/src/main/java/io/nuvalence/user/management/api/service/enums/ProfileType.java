package io.nuvalence.user.management.api.service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Enumeration of profile types.
 */
public enum ProfileType {
    EMPLOYER("EMPLOYER", "employer"),
    INDIVIDUAL("INDIVIDUAL", "individual");

    private final String value;
    private final String label;

    ProfileType(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public String getLabel() {
        return label;
    }

    /**
     * Constructs a new ProfileType with the given string value.
     * @param value the string value to be converted to an enum value
     * @return an element from the enum
     *
     * @throws IllegalArgumentException if value is not a valid enum value.
     */
    @JsonCreator
    public static ProfileType fromValue(String value) {
        if (value == null) {
            return null;
        }
        value = value.toUpperCase(Locale.ROOT);
        for (ProfileType b : ProfileType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
