package io.nuvalence.user.management.api.service.models;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.util.UserUtility;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmployerProfileLinksFiltersTest {
    @InjectMocks private EmployerProfileLinkFilters filters;

    @Mock private Root<EmployerProfileLink> root;

    @Mock private Path<String> pathExpression;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder criteriaBuilder;

    @Captor private ArgumentCaptor<Predicate> predicateCaptor;

    @Test
    void testIndividualSpecificationWitProfileId() {
        filters = new EmployerProfileLinkFilters(UUID.randomUUID(), null, null, null, null, null);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        Path<Object> path = mock(Path.class);
        when(root.get("profile")).thenReturn(path);
        when(path.<String>get("id")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, filters.getProfileId()))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            Specification<EmployerProfileLink> specification =
                    filters.getEmployerUserLinkSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertNotNull(specification);
            Assertions.assertNotNull(result);

            Assertions.assertEquals(finalPredicate, result);
        }
    }

    @Test
    void testIndividualSpecificationWithListOfIds() {
        filters =
                new EmployerProfileLinkFilters(
                        null, List.of(UUID.randomUUID()), null, null, null, null);
        Predicate finalPredicate = mock(Predicate.class);

        Path<Object> path = mock(Path.class);
        when(root.get("user")).thenReturn(path);
        when(path.get("id")).thenReturn(path);

        CriteriaBuilder.In<Object> in = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            Specification<EmployerProfileLink> specification =
                    filters.getEmployerUserLinkSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertNotNull(specification);
            Assertions.assertNotNull(result);

            Assertions.assertEquals(finalPredicate, result);
        }
    }

    @Test
    void testIndividualSpecificationWithNullValues() {
        // Given
        filters = new EmployerProfileLinkFilters(null, null, null, null, null, null);
        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            Specification<EmployerProfileLink> specification =
                    filters.getEmployerUserLinkSpecification();
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            Assertions.assertNull(result);
            verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
            verify(criteriaBuilder, atMost(2)).and();
        }
    }
}
