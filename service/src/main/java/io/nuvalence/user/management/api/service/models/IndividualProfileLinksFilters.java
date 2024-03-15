package io.nuvalence.user.management.api.service.models;

import io.nuvalence.user.management.api.service.entity.UserEntity;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.service.UserService;
import io.nuvalence.user.management.api.service.util.UserUtility;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class IndividualProfileLinksFilters extends BaseFilters {
    private final UUID userId;
    private final String email;
    private final String name;
    private final UUID profileId;
    private final UserService userService;

    @SuppressWarnings("java:S107")
    @Builder
    public IndividualProfileLinksFilters(
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize,
            UUID userId,
            String email,
            String name,
            UUID profileId,
            UserService userService) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profileId = profileId;
        this.userService = userService;
    }

    public Specification<IndividualProfileLink> getIndividualLinksSpecification() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(this.name)) {
                List<UserEntity> users =
                        userService.findByNameContainingIgnoreCaseAndDeletedFalse(this.name);
                userIsPresent(root, criteriaBuilder, predicates, users);
            }

            if (this.userId != null) {
                predicates.add(criteriaBuilder.equal(root.join("user").get("id"), this.userId));
            }

            if (this.profileId != null) {
                predicates.add(
                        criteriaBuilder.equal(root.join("profile").get("id"), this.profileId));
            }

            if (StringUtils.isNotBlank(this.email)) {
                List<UserEntity> users =
                        userService.findByEmailContainingIgnoreCaseAndDeletedFalse(this.email);
                userIsPresent(root, criteriaBuilder, predicates, users);
            }

            if (UserUtility.getAuthenticatedUserType().equals("public")) {
                predicates.add(
                        criteriaBuilder.notEqual(
                                root.get("profileAccessLevel"),
                                ProfileAccessLevel.AGENCY_READONLY));
            }

            return !predicates.isEmpty()
                    ? criteriaBuilder.and(predicates.toArray(new Predicate[0]))
                    : null;
        };
    }

    private void userIsPresent(
            Root<IndividualProfileLink> root,
            CriteriaBuilder criteriaBuilder,
            ArrayList<Object> predicates,
            List<UserEntity> users) {
        if (!users.isEmpty()) {
            List<UUID> userIds = new ArrayList<>();
            users.forEach(userDTO -> userIds.add(userDTO.getId()));
            predicates.add(criteriaBuilder.in(root.get("user").get("id")).value(userIds));
        } else {
            predicates.add(criteriaBuilder.disjunction());
        }
    }
}
