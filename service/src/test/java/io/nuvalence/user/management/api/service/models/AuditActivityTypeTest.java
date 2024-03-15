package io.nuvalence.user.management.api.service.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.nuvalence.user.management.api.service.models.auditevents.AuditActivityType;
import org.junit.jupiter.api.Test;

class AuditActivityTypeTest {
    @Test
    void fromValue_ValidValue_ReturnsCorrectEnum() {
        assertEquals(
                AuditActivityType.EMPLOYER_PROFILE_CREATED,
                AuditActivityType.fromValue("employer_profile_created"));
        assertEquals(
                AuditActivityType.INDIVIDUAL_PROFILE_CREATED,
                AuditActivityType.fromValue("individual_profile_created"));
        assertEquals(
                AuditActivityType.PROFILE_INVITATION_SENT,
                AuditActivityType.fromValue("profile_invitation_sent"));
    }

    @Test
    void fromValue_InvalidValue_ThrowsIllegalArgumentException() {
        String invalidValue = "non_existent_enum_value";
        assertThrows(
                IllegalArgumentException.class, () -> AuditActivityType.fromValue(invalidValue));
    }
}
