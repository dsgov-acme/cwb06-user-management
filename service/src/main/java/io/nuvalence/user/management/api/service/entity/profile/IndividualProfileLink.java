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
@DiscriminatorValue("individual")
@Builder
@AccessResource("individual_profile_link")
@EntityListeners(UpdateTrackedEntityEventListener.class)
public class IndividualProfileLink extends ProfileLink<IndividualProfile> {

    @ManyToOne
    @JoinColumn(name = "individual_profile_id")
    private IndividualProfile profile;
}
