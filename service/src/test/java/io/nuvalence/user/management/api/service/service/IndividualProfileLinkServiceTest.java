package io.nuvalence.user.management.api.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.models.IndividualProfileLinksFilters;
import io.nuvalence.user.management.api.service.repository.IndividualProfileLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class IndividualProfileLinkServiceTest {
    @Mock private IndividualProfileLinkRepository repository;

    @Mock private IndividualProfileService individualProfileService;

    @InjectMocks private IndividualProfileLinkService service;

    @Test
    void testSaveIndividualUserLink() {
        IndividualProfileLink individualUserLink = new IndividualProfileLink();
        when(repository.save(any(IndividualProfileLink.class))).thenReturn(individualUserLink);

        IndividualProfileLink savedLink = service.saveIndividualUserLink(individualUserLink);

        assertNotNull(savedLink);
        assertEquals(individualUserLink, savedLink);
    }

    @Test
    void testGetIndividualUserLinkByProfileAndUserId() {
        UUID profileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        IndividualProfileLink expectedLink = new IndividualProfileLink();

        when(repository.findByProfileIdAndUserId(profileId, userId))
                .thenReturn(Optional.of(expectedLink));

        Optional<IndividualProfileLink> actualLink =
                service.getIndividualUserLinkByProfileAndUserId(profileId, userId);

        assertTrue(actualLink.isPresent());
        assertEquals(expectedLink, actualLink.get());
    }

    @Test
    void testGetIndividualUserLinkByProfileAndUserId_NullInputs() {
        Optional<IndividualProfileLink> link =
                service.getIndividualUserLinkByProfileAndUserId(null, null);
        assertTrue(link.isEmpty());
    }

    @Test
    void testDeleteIndividualUserLink() {
        IndividualProfileLink individualUserLink = new IndividualProfileLink();
        service.deleteIndividualUserLink(individualUserLink);
        Mockito.verify(repository).delete(individualUserLink);
    }

    @Test
    void testGetIndividualLinksByFilters() {
        // Given
        IndividualProfileLinksFilters filters =
                IndividualProfileLinksFilters.builder()
                        .userId(UUID.randomUUID())
                        .email("test@example.com")
                        .sortBy("name")
                        .sortOrder("ASC")
                        .pageNumber(0)
                        .pageSize(10)
                        .name("John Doe")
                        .build();

        // Use the same PageRequest as in the service method
        PageRequest pageRequest =
                PageRequest.of(
                        filters.getPageNumber(),
                        filters.getPageSize(),
                        Sort.by(Sort.Order.asc(filters.getSortBy())) // Use the same sorting order
                        );

        // Mock the behavior of your repository
        when(repository.findAll(any(Specification.class), eq(pageRequest)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        // When
        Page<IndividualProfileLink> result = service.getIndividualLinksByFilters(filters);

        assertNotNull(result);
    }

    @Test
    void getIndividualLinksByUserIdTest() {
        UUID userId = UUID.randomUUID();
        IndividualProfileLink individualUserLink = IndividualProfileLink.builder().build();
        individualUserLink.setId(UUID.randomUUID());
        when(repository.findByUserId(userId)).thenReturn(List.of(individualUserLink));

        List<IndividualProfileLink> result = service.getIndividualLinksByUserId(userId);
        assertEquals(1, result.size());
        assertEquals(individualUserLink, result.get(0));
    }

    @Test
    void getIndividualLinksByUserId() {
        UUID userId = UUID.randomUUID();

        IndividualProfileLink individualUserLink = IndividualProfileLink.builder().build();
        individualUserLink.setId(UUID.randomUUID());
        List<IndividualProfileLink> expected = List.of(individualUserLink);

        when(repository.findByUserId(userId)).thenReturn(expected);

        List<IndividualProfileLink> result = service.getIndividualLinksByUserId(userId);

        assertEquals(expected, result);
    }

    @Test
    void getEmployerLinksByUserId() {
        IndividualProfileLink individualUserLink = IndividualProfileLink.builder().build();

        when(repository.findByUserId(any())).thenReturn(List.of(individualUserLink));

        List<IndividualProfileLink> result = service.getIndividualLinksByUserId(UUID.randomUUID());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(individualUserLink, result.get(0));
    }
}
