package com.breadcost.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Item master data entity
 * Represents raw materials, packaging, finished goods, byproducts, WIP, and sentinels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    private String itemId;
    private ItemType type;
    private String baseUom;
    private String roundingProfileId;
    private Boolean lotTracked;
    private Boolean locationTracked;
    private Boolean expiryTracked;
    private Boolean negativeAllowed;

    public enum ItemType {
        INGREDIENT,
        PACKAGING,
        FG,          // Finished Goods
        BYPRODUCT,
        WIP,         // Work in Progress
        SENTINEL     // System sentinel (e.g., __NO_LOT__, __NO_LOC__)
    }
}
