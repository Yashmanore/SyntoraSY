package com.example.societyhub.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
public class FlatTenancy {
    private Integer tenancy_id;           // PK
    private Integer flat_id;              // FK -> Flat
    private Integer tenant_id;            // FK -> Tenant
    private LocalDate lease_start_date;
    private LocalDate lease_end_date;
    private BigDecimal security_deposit;
    private BigDecimal rent_amount;
}
