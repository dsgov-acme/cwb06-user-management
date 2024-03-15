package io.nuvalence.user.management.api.service.entity.profile;

import com.google.common.base.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "address")
public class Address {
    @Id
    @Column(name = "id", insertable = false, updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "address1", nullable = false)
    private String address1;

    @Column(name = "address2", nullable = false)
    private String address2;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "county", nullable = false)
    private String county;

    @OneToOne
    @JoinColumn(name = "individual_for_mailing_id", referencedColumnName = "id")
    private IndividualProfile individualForMailing;

    @OneToOne
    @JoinColumn(name = "individual_for_primary_address_id", referencedColumnName = "id")
    private IndividualProfile individualForAddress;

    @OneToOne
    @JoinColumn(name = "employer_for_mailing_id", referencedColumnName = "id")
    private EmployerProfile employerForMailing;

    @ManyToOne
    @JoinColumn(name = "employer_for_locations_id", referencedColumnName = "id")
    private EmployerProfile employerForLocations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equal(address1, address.address1)
                && Objects.equal(address2, address.address2)
                && Objects.equal(city, address.city)
                && Objects.equal(state, address.state)
                && Objects.equal(postalCode, address.postalCode)
                && Objects.equal(country, address.country)
                && Objects.equal(county, address.county);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address1, address2, city, state, postalCode, country, county);
    }
}
