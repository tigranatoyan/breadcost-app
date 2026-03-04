package com.breadcost.customers;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Value object: delivery address for a customer.
 * Stored as a collection element in customer_addresses table.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {

    private String label;       // e.g. "Home", "Office", "Warehouse"
    private String line1;
    private String line2;
    private String city;
    private String postalCode;
    private String countryCode;  // ISO 3166-1 alpha-2, e.g. "AM"
}
