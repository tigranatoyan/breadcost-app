package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Location entity
 * Represents physical storage locations (bins, racks, stations, system locations)
 * Key: (siteId, locationId)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private String siteId;
    private String locationId;
    private LocationType type;

    public enum LocationType {
        BIN,
        RACK,
        STATION,
        SYSTEM
    }
}
