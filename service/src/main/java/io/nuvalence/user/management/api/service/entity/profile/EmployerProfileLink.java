package io.nuvalence.user.management.api.service.entity.profile;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.user.management.api.service.entity.UpdateTrackedEntityEventListener;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@DiscriminatorValue("employer")
@Builder
@AccessResource("employer_profile_link")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class EmployerProfileLink extends ProfileLink<EmployerProfile> {

    @ManyToOne
    @JoinColumn(name = "employer_profile_id")
    private EmployerProfile profile;
}
