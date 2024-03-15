package io.nuvalence.user.management.api.service.repository;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployerProfileLinkRepository
        extends JpaRepository<EmployerProfileLink, UUID>,
                JpaSpecificationExecutor<EmployerProfileLink> {
    Optional<EmployerProfileLink> findByProfileIdAndUserId(UUID profileId, UUID userId);

    List<EmployerProfileLink> findByUserId(UUID userId);
}
