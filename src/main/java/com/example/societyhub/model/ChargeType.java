package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class ChargeType {
    private Integer id;              // PK
    private Integer society_id;      // FK -> Society
    private String name;             // e.g. "Maintenance", "Rent", "Parking"
    private BigDecimal default_amount;
    private String applicable_to;    // ENUM: 'ALL', 'OWNER', 'TENANT'
    private Boolean is_active;       // soft-delete / disable toggle
}
