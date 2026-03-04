package com.breadcost.masterdata;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "pos_sale_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleLineEntity {

    @Id
    private String lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saleId")
    @JsonIgnore
    private SaleEntity sale;

    private String productId;
    private String productName;

    @Column(precision = 14, scale = 4)
    private BigDecimal quantity;

    private String unit;

    @Column(precision = 14, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 14, scale = 4)
    private BigDecimal lineTotal;
}
