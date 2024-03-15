package io.nuvalence.user.management.api.service.entity;

import io.nuvalence.auth.access.AccessResource;
import io.nuvalence.user.management.api.service.enums.UserType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single User Entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_table")
@AccessResource(value = "user", translator = UserAccessResourceTranslator.class)
@NamedEntityGraph(
        name = "user.complete",
        attributeNodes = {@NamedAttributeNode("roles"), @NamedAttributeNode("userPreference")})
@EntityListeners(UserEntityEventListener.class)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@SuppressWarnings("ClassFanOutComplexity")
public abstract class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "identity_provider", nullable = false)
    private String identityProvider;

    @Column(name = "created_At", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "user_type", insertable = false, updatable = false, nullable = false)
    private UserType userType;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    @Column(name = "deleted_On")
    private OffsetDateTime deletedOn;

    @Generated(event = EventType.INSERT)
    @Column(name = "full_name")
    private String fullName;

    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.DETACH})
    private List<RoleEntity> roles;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserPreferenceEntity userPreference;

    public abstract String getFirstName();

    public abstract void setFirstName(String firstName);

    public abstract String getMiddleName();

    public abstract void setMiddleName(String middleName);

    public abstract String getLastName();

    public abstract void setLastName(String lastName);

    public abstract String getEmail();

    public abstract void setEmail(String email);

    public abstract String getPhoneNumber();

    public abstract void setPhoneNumber(String phoneNumber);
}
