package io.nuvalence.user.management.api.service.models;

import io.nuvalence.user.management.api.service.entity.profile.EmployerProfileLink;
import io.nuvalence.user.management.api.service.entity.profile.ProfileAccessLevel;
import io.nuvalence.user.management.api.service.util.UserUtility;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class EmployerProfileLinkFilters extends BaseFilters {
    private final UUID profileId;
    private final List<UUID> userIds;

    @Builder
    public EmployerProfileLinkFilters(
            UUID profileId,
            List<UUID> userIds,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {
        super(sortBy, sortOrder, pageNumber, pageSize);
        this.profileId = profileId;
        this.userIds = userIds;
    }

    public Specification<EmployerProfileLink> getEmployerUserLinkSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (profileId != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("profile").get("id"), this.profileId));
            }
            if (userIds != null) {
                predicates.add(root.get("user").get("id").in(userIds));
            }

            if (UserUtility.getAuthenticatedUserType().equals("public")) {
                predicates.add(
                        criteriaBuilder.notEqual(
                                root.get("profileAccessLevel"),
                                ProfileAccessLevel.AGENCY_READONLY));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
