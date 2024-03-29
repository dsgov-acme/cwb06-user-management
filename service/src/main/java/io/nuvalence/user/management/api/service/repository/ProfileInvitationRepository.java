package io.nuvalence.user.management.api.service.repository;

import io.nuvalence.user.management.api.service.entity.profile.ProfileInvitation;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ProfileInvitationRepository
        extends JpaRepository<ProfileInvitation, UUID>,
                JpaSpecificationExecutor<ProfileInvitation> {

    Optional<ProfileInvitation> findFirstByEmailAndProfileIdAndExpiresAfter(
            String email, UUID profileId, OffsetDateTime now);

    Optional<ProfileInvitation> findByIdAndEmail(UUID invitationId, String email);

    Optional<ProfileInvitation> findByIdAndType(UUID id, ProfileType type);
}
