package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Site entity
 * Represents a manufacturing site/facility
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Site {
    private String siteId;
    private String name;
    private String timezone;
    private String defaultFgLocationId;
}
