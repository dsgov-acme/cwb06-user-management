package io.nuvalence.user.management.api.service.models;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.user.management.api.service.entity.PublicUser;
import io.nuvalence.user.management.api.service.entity.UserEntity;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.service.UserService;
import io.nuvalence.user.management.api.service.util.UserUtility;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
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
class IndividualProfileLinksFiltersTest {

    @InjectMocks private IndividualProfileLinksFilters filters;

    @Mock private Root<IndividualProfileLink> root;

    @Mock private Path<String> pathExpression;

    @Mock private CriteriaQuery<?> query;

    @Mock private CriteriaBuilder criteriaBuilder;

    @Mock private UserService userService;

    @Captor private ArgumentCaptor<Predicate> predicateCaptor;

    @Test
    void testIndividualSpecificationWithRecordDefinitionKey() {
        // Given
        filters =
                new IndividualProfileLinksFilters(
                        null, null, null, null, UUID.randomUUID(), null, null, null, userService);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        Join<Object, Object> join = mock(Join.class);
        when(root.join("user")).thenReturn(join);
        when(join.<String>get("id")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, filters.getUserId())).thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            Specification<IndividualProfileLink> specification =
                    filters.getIndividualLinksSpecification();
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertEquals(finalPredicate, result);
            verify(criteriaBuilder).and(predicateCaptor.capture());

            var value = predicateCaptor.getValue();
            Assertions.assertEquals(firstPredicate, value);
        }
    }

    @Test
    void testIndividualSpecificationWithProfileId() {
        // Given
        filters =
                new IndividualProfileLinksFilters(
                        null, null, null, null, null, null, null, UUID.randomUUID(), userService);
        Predicate firstPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        Join<Object, Object> join = mock(Join.class);
        when(root.join("profile")).thenReturn(join);
        when(join.<String>get("id")).thenReturn(pathExpression);
        when(criteriaBuilder.equal(pathExpression, filters.getProfileId()))
                .thenReturn(firstPredicate);
        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");
            Specification<IndividualProfileLink> specification =
                    filters.getIndividualLinksSpecification();
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            // Verify
            Assertions.assertEquals(finalPredicate, result);
            verify(criteriaBuilder).and(predicateCaptor.capture());

            var value = predicateCaptor.getValue();
            Assertions.assertEquals(firstPredicate, value);
        }
    }

    @Test
    void testIndividualSpecificationWithNullValues() {
        // Given
        filters =
                new IndividualProfileLinksFilters(
                        null, null, null, null, null, null, null, null, userService);

        // Execute

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            Specification<IndividualProfileLink> specification =
                    filters.getIndividualLinksSpecification();
            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            Assertions.assertNull(result);
            verify(criteriaBuilder, never()).like(any(Expression.class), any(String.class));
            verify(criteriaBuilder, atMost(2)).and();
        }
    }

    @Test
    void testIndividualSpecificationWithEmail() {
        filters =
                new IndividualProfileLinksFilters(
                        null, null, null, null, null, "email@email.com", null, null, userService);

        List<UserEntity> userList = List.of(createUserEntity());
        when(userService.findByEmailContainingIgnoreCaseAndDeletedFalse(any()))
                .thenReturn(userList);

        Path<Object> path = mock(Path.class);
        when(root.get("user")).thenReturn(path);
        when(path.<String>get("id")).thenReturn(pathExpression);

        CriteriaBuilder.In<Object> in = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.<Object>in(pathExpression)).thenReturn(in);

        CriteriaBuilder.In<Object> in2 = mock(CriteriaBuilder.In.class);
        when(in.value(anyList())).thenReturn(in2);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");

            Specification<IndividualProfileLink> specification =
                    filters.getIndividualLinksSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            Assertions.assertNotNull(specification);
            Assertions.assertNotNull(result);

            Assertions.assertEquals(finalPredicate, result);
        }
    }

    @Test
    void testIndividualSpecificationWithName() {
        filters =
                new IndividualProfileLinksFilters(
                        null, null, null, null, null, null, "name", null, userService);

        List<UserEntity> userList = List.of(createUserEntity());
        when(userService.findByNameContainingIgnoreCaseAndDeletedFalse(any())).thenReturn(userList);

        Path<Object> path = mock(Path.class);
        when(root.get("user")).thenReturn(path);
        when(path.<String>get("id")).thenReturn(pathExpression);

        CriteriaBuilder.In<Object> in = mock(CriteriaBuilder.In.class);
        when(criteriaBuilder.<Object>in(pathExpression)).thenReturn(in);

        CriteriaBuilder.In<Object> in2 = mock(CriteriaBuilder.In.class);
        when(in.value(anyList())).thenReturn(in2);

        Predicate finalPredicate = mock(Predicate.class);

        when(criteriaBuilder.and(any())).thenReturn(finalPredicate);

        try (MockedStatic<UserUtility> mock = Mockito.mockStatic(UserUtility.class)) {

            mock.when(UserUtility::getAuthenticatedUserType).thenReturn("public");
            Specification<IndividualProfileLink> specification =
                    filters.getIndividualLinksSpecification();

            Predicate result = specification.toPredicate(root, query, criteriaBuilder);

            Assertions.assertNotNull(specification);
            Assertions.assertNotNull(result);

            Assertions.assertEquals(finalPredicate, result);
        }
    }

    private UserEntity createUserEntity() {
        PublicUser userEntity = new PublicUser();
        userEntity.setId(UUID.randomUUID());
        userEntity.setFirstName("John");
        userEntity.setLastName("Doe");
        userEntity.setEmail("TestEmail");
        return userEntity;
    }
}
