package io.nuvalence.user.management.api.service.models;

import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Data
@Slf4j
@Builder
public class AccessProfileDto {
    private UUID id;
    private ProfileAccessLevel level;
    private ProfileType type;
}
