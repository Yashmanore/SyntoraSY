package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
public class UnitBillRecord {
    private Integer id;             // PK
    private Integer bill_id;        // FK -> Bill (society-wide billing cycle)
    private Integer flat_id;        // FK -> Flat
    private String status;          // ENUM: 'UNPAID', 'PAID', 'PAID_WITH_FINE'
    private BigDecimal total_amount; // cached sum of all BillLineItems for this record
    private BigDecimal fine_amount;  // late-payment fine, if applicable
    private LocalDate paid_date;     // date the payment was made (null if unpaid)
    private String month;
    private Integer year;
}

