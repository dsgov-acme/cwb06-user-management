package io.nuvalence.user.management.api.service.service;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.models.EmployerProfileLinkFilters;
import io.nuvalence.user.management.api.service.repository.EmployerProfileLinkRepository;
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
public class EmployerProfileLinkService {
    private final EmployerProfileLinkRepository repository;

    public Optional<EmployerProfileLink> getEmployerUserLink(UUID profileId, UUID userId) {
        return repository.findByProfileIdAndUserId(profileId, userId);
    }

    public EmployerProfileLink saveEmployerUserLink(EmployerProfileLink employerUserLink) {
        return repository.save(employerUserLink);
    }

    public void deleteEmployerUserLink(UUID id) {
        repository.deleteById(id);
    }

    public Page<EmployerProfileLink> getEmployerUserLinks(
            final EmployerProfileLinkFilters filters) {
        return repository.findAll(
                filters.getEmployerUserLinkSpecification(), filters.getPageRequest());
    }

    public List<EmployerProfileLink> getEmployerLinksByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }
}
