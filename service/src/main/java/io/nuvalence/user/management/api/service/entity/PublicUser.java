package io.nuvalence.user.management.api.service.entity;

import io.nuvalence.user.management.api.service.entity.profile.IndividualProfile;
import io.nuvalence.user.management.api.service.entity.profile.IndividualProfileLink;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@DiscriminatorValue("public")
@Builder
public class PublicUser extends UserEntity {
    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "profile_id")
    private IndividualProfile individualProfile;

    // Remove these fields after the profile refactor is fully implemented
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @OneToMany(mappedBy = "user")
    private Set<IndividualProfileLink> profileLinks;

    @Override
    public String getFirstName() {
        return (individualProfile != null) ? individualProfile.getFirstName() : firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        if (individualProfile != null) {
            individualProfile.setFirstName(firstName);
        }
    }

    @Override
    public String getMiddleName() {
        return (individualProfile != null) ? individualProfile.getMiddleName() : middleName;
    }

    @Override
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
        if (individualProfile != null) {
            individualProfile.setMiddleName(middleName);
        }
    }

    @Override
    public String getLastName() {
        return (individualProfile != null) ? individualProfile.getLastName() : lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
        if (individualProfile != null) {
            individualProfile.setLastName(lastName);
        }
    }

    @Override
    public String getEmail() {
        return (individualProfile != null) ? individualProfile.getEmail() : email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
        if (individualProfile != null) {
            individualProfile.setEmail(email);
        }
    }

    @Override
    public String getPhoneNumber() {
        return (individualProfile != null) ? individualProfile.getPhoneNumber() : phoneNumber;
    }

    @Override
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        if (individualProfile != null) {
            individualProfile.setPhoneNumber(phoneNumber);
        }
    }

    public void addProfileLink(IndividualProfileLink profileLink) {
        if (profileLinks == null) {
            profileLinks = new HashSet<>();
        }
        profileLinks.add(profileLink);
        profileLink.setUser(this);
    }
}
