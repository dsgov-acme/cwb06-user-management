package io.nuvalence.user.management.api.service.service;

import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.models.IndividualProfileLinksFilters;
import io.nuvalence.user.management.api.service.repository.IndividualProfileLinkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class IndividualProfileLinkService {
    private final IndividualProfileService individualService;
    private final IndividualProfileLinkRepository repository;

    public IndividualProfileLink saveIndividualUserLink(
            final IndividualProfileLink individualUserLink) {
        return repository.save(individualUserLink);
    }

    public Optional<IndividualProfileLink> getIndividualUserLinkByProfileAndUserId(
            UUID profileId, UUID userId) {
        if (profileId == null || userId == null) {
            return Optional.empty();
        }

        return repository.findByProfileIdAndUserId(profileId, userId);
    }

    public void deleteIndividualUserLink(IndividualProfileLink individualUserLink) {
        repository.delete(individualUserLink);
    }

    public Page<IndividualProfileLink> getIndividualLinksByFilters(
            final IndividualProfileLinksFilters filters) {
        return repository.findAll(
                filters.getIndividualLinksSpecification(), filters.getPageRequest());
    }

    public List<IndividualProfileLink> getIndividualLinksByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }
}
