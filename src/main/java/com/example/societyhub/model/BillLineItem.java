package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class BillLineItem {
    private Integer id;                        // PK
    private Integer unit_bill_record_id;       // FK -> UnitBillRecord (per-flat)
    private Integer charge_type_history_id;    // FK -> ChargeTypeHistory
    private BigDecimal amount;                 // actual amount charged (from history snapshot)
}

