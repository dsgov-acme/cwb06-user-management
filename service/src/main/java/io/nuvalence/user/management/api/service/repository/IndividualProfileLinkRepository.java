package io.nuvalence.user.management.api.service.repository;

import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndividualProfileLinkRepository
        extends JpaRepository<IndividualProfileLink, UUID>,
                JpaSpecificationExecutor<IndividualProfileLink> {

    Optional<IndividualProfileLink> findByProfileIdAndUserId(UUID profileId, UUID userId);

    List<IndividualProfileLink> findByUserId(UUID userId);
}
