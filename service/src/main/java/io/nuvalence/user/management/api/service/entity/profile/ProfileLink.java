package io.nuvalence.user.management.api.service.entity.profile;

import com.google.common.base.Objects;
import io.nuvalence.user.management.api.service.entity.UpdateTrackedEntity;
import io.nuvalence.user.management.api.service.entity.UserEntity;
import io.nuvalence.user.management.api.service.enums.ProfileType;
import io.nuvalence.user.management.api.service.util.jpa.ProfileAccessLevelConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "profile_link")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "profile_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ProfileLink<T extends Profile> implements UpdateTrackedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    protected UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    protected UserEntity user;

    @Column(name = "profile_access_level", nullable = false)
    @Convert(converter = ProfileAccessLevelConverter.class)
    protected ProfileAccessLevel profileAccessLevel;

    @Column(name = "profile_type", insertable = false, updatable = false, nullable = false)
    protected ProfileType profileType;

    @Column(name = "created_by", length = 36, nullable = false)
    protected String createdBy;

    @Column(name = "last_updated_by", length = 36, nullable = false)
    protected String lastUpdatedBy;

    @Column(name = "created_timestamp", nullable = false)
    protected OffsetDateTime createdTimestamp;

    @Column(name = "last_updated_timestamp", nullable = false)
    protected OffsetDateTime lastUpdatedTimestamp;

    public abstract T getProfile();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileLink<?> that = (ProfileLink<?>) o;
        return Objects.equal(this.getId(), that.getId())
                && this.getProfileAccessLevel() == that.getProfileAccessLevel()
                && Objects.equal(createdBy, that.createdBy)
                && Objects.equal(lastUpdatedBy, that.lastUpdatedBy)
                && Objects.equal(this.getCreatedTimestamp(), that.getCreatedTimestamp())
                && Objects.equal(this.getLastUpdatedTimestamp(), that.getLastUpdatedTimestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                this.getId(),
                this.getProfileAccessLevel(),
                this.getCreatedBy(),
                this.getLastUpdatedBy(),
                this.getCreatedTimestamp(),
                this.getLastUpdatedTimestamp());
    }
}
