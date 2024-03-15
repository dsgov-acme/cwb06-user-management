package io.nuvalence.user.management.api.service.service;

import io.nuvalence.user.management.api.service.models.AccessProfileDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CommonProfileLinkService {

    private final IndividualProfileLinkService individualProfileLinkService;
    private final EmployerProfileLinkService employerProfileLinkService;

    public List<AccessProfileDto> getProfilesByUserId(UUID userId) {
        List<AccessProfileDto> profiles = new ArrayList<>();
        individualProfileLinkService.getIndividualLinksByUserId(userId).stream()
                .forEach(
                        link ->
                                profiles.add(
                                        AccessProfileDto.builder()
                                                .id(link.getProfile().getId())
                                                .type(link.getProfile().getProfileType())
                                                .level(link.getProfileAccessLevel())
                                                .build()));
        employerProfileLinkService.getEmployerLinksByUserId(userId).stream()
                .forEach(
                        link ->
                                profiles.add(
                                        AccessProfileDto.builder()
                                                .id(link.getProfile().getId())
                                                .type(link.getProfile().getProfileType())
                                                .level(link.getProfileAccessLevel())
                                                .build()));
        return profiles;
    }
}
