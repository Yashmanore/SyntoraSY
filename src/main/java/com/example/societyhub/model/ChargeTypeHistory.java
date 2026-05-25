package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class ChargeTypeHistory {
    private Integer history_id;          // PK
    private Integer charge_type_id;      // FK -> ChargeType
    private Integer society_id;          // FK -> Society (denormalized for easy querying)
    private String name_at_billing;
    private BigDecimal amount_at_billing;
    private String applicable_to;        // Frozen snapshot: 'ALL', 'OWNER', 'TENANT'
    private String month;
    private Integer year;
}
