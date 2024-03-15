package io.nuvalence.user.management.api.service.config;

import io.nuvalence.auth.access.cerbos.CerbosPrincipalAttributesProvider;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.models.AccessProfileDto;
import io.nuvalence.user.management.api.service.service.EmployerProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CerbosPrincipalAttributeProviderImpl implements CerbosPrincipalAttributesProvider {

    private final IndividualProfileLinkService individualUserLinkService;
    private final EmployerProfileLinkService employerUserLinkService;

    @Override
    public Map<String, Object> getAttributes(Authentication principal) {
        String id = SecurityContextUtility.getAuthenticatedUserId();

        Map<String, Object> attributes = new HashMap<>();
        if (id != null) {
            List<AccessProfileDto> accessProfiles = new ArrayList<>();
            accessProfiles.addAll(
                    createAccessProfilesFromIndividualLinks(
                            individualUserLinkService.getIndividualLinksByUserId(
                                    UUID.fromString(id))));
            accessProfiles.addAll(
                    createAccessProfilesFromEmployerLinks(
                            employerUserLinkService.getEmployerLinksByUserId(UUID.fromString(id))));

            attributes.put("accessProfiles", accessProfiles);
        }

        return attributes;
    }

    private List<AccessProfileDto> createAccessProfilesFromIndividualLinks(
            List<IndividualProfileLink> links) {
        return links.stream()
                .map(
                        link ->
                                AccessProfileDto.builder()
                                        .id(link.getProfile().getId())
                                        .type(ProfileType.INDIVIDUAL)
                                        .level(link.getProfileAccessLevel())
                                        .build())
                .toList();
    }

    private List<AccessProfileDto> createAccessProfilesFromEmployerLinks(
            List<EmployerProfileLink> links) {
        return links.stream()
                .map(
                        link ->
                                AccessProfileDto.builder()
                                        .id(link.getProfile().getId())
                                        .type(ProfileType.EMPLOYER)
                                        .level(link.getProfileAccessLevel())
                                        .build())
                .toList();
    }
}
