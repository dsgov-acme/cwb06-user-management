package io.nuvalence.user.management.api.service.entity.profile;

import io.nuvalence.user.management.api.service.enums.ProfileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = "id")
@Table(name = "profile_invitation")
public class ProfileInvitation {

    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "profile_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProfileType type;

    @Column(name = "profile_access_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProfileAccessLevel accessLevel;

    @Column(name = "expires", nullable = false)
    private OffsetDateTime expires;

    @Column(name = "claimed", nullable = false, columnDefinition = "boolean default false")
    private Boolean claimed;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "created_timestamp", nullable = false)
    private OffsetDateTime createdTimestamp;

    @Column(name = "claimed_timestamp")
    private OffsetDateTime claimedTimestamp;

    @PrePersist
    protected void onCreate() {
        createdTimestamp = OffsetDateTime.now();
        expires = createdTimestamp.plusDays(7);
    }
}
