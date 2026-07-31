package dev.tuiop.accountservice.customer;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter

public class Address {

        private String country;
        private String city;
        private String street;
        private String postalCode;
        private String apartment;

}
