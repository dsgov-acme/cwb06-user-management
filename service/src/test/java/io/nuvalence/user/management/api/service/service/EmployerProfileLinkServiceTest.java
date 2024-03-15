package io.nuvalence.user.management.api.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.models.EmployerProfileLinkFilters;
import io.nuvalence.user.management.api.service.repository.EmployerProfileLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmployerProfileLinkServiceTest {
    @Mock private EmployerProfileLinkRepository repository;

    @InjectMocks private EmployerProfileLinkService service;

    @Test
    void saveEmployerUserLink() {
        EmployerProfileLink link = new EmployerProfileLink();
        service.saveEmployerUserLink(link);
        verify(repository, times(1)).save(link);
    }

    @Test
    void getEmployerUserLink() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployerProfileLink link = new EmployerProfileLink();

        when(repository.findByProfileIdAndUserId(profileId, userId)).thenReturn(Optional.of(link));

        Optional<EmployerProfileLink> result = service.getEmployerUserLink(profileId, userId);

        assertTrue(result.isPresent());
        assertEquals(link, result.get());
    }

    @Test
    void deleteEmployerUserLink() {
        UUID id = UUID.randomUUID();
        service.deleteEmployerUserLink(id);
        verify(repository, times(1)).deleteById(id);
    }

    @Test
    void getEmployerUserLinks() {
        UUID profileId = UUID.randomUUID();
        List<UUID> userIds = Collections.singletonList(UUID.randomUUID());
        String sortBy = "createdTimestamp";
        String sortOrder = "ASC";
        Integer pageNumber = 0;
        Integer pageSize = 10;

        EmployerProfileLinkFilters filters =
                EmployerProfileLinkFilters.builder()
                        .profileId(profileId)
                        .userIds(userIds)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .build();

        EmployerProfileLink link = new EmployerProfileLink();
        Page<EmployerProfileLink> expectedPage = new PageImpl<>(Collections.singletonList(link));

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(expectedPage);

        Page<EmployerProfileLink> result = service.getEmployerUserLinks(filters);

        assertEquals(expectedPage, result);
    }
}
