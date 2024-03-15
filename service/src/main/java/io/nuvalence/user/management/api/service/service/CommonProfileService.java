package io.nuvalence.user.management.api.service.service;

import io.nuvalence.user.management.api.service.entity.profile.Profile;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CommonProfileService {

    private final IndividualProfileService individualService;
    private final EmployerProfileService employerService;

    public Optional<Profile> getProfileById(UUID id) {
        return individualService
                .getIndividualById(id)
                .map(i -> (Profile) i)
                .or(() -> employerService.getEmployerById(id).map(e -> (Profile) e));
    }
}
